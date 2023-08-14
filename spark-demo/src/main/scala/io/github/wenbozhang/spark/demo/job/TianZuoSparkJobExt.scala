package io.github.wenbozhang.spark.demo.job

import cn.tongdun.tiance.job.client.enumeration.JobReasonCodeEnum
import cn.tongdun.tiance.job.client.model.job.JobResult
import cn.tongdun.tiance.job.common.constant.DateFormatterConstants
import cn.tongdun.tiance.job.common.job.SparkJobLogger
import cn.tongdun.tiance.job.connector.api.egress.DataFrameSink
import cn.tongdun.tiance.job.connector.api.ingress.Column
import cn.tongdun.tiance.job.connector.impl.egress.{HiveDataFrameSink, HiveDataFrameSinkConfig}
import cn.tongdun.tianzuo.captain.client.entity.dto.IndexConfig
import cn.tongdun.tianzuo.captain.common.constant.DataType._
import com.alibaba.fastjson.JSON
import io.github.wenbozhang.spark.demo.data.MainArgsContext
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.spark.SparkConf
import org.apache.spark.api.java.function.MapFunction
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.{DataTypes, StructType}
import org.apache.spark.sql.{DataFrame, Row, RowFactory, SparkSession}

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable
import scala.util.{Failure, Success}

/**
 * @author wenbo.zhangw
 * @date 2023/8/8 14:47
 */
abstract class TianZuoSparkJobExt[T] {

  val jobName: String

  implicit var logger: SparkJobLogger = new SparkJobLogger

  implicit var sparkSession: SparkSession = getSparkSession

  private def getSparkSession: SparkSession = {
    val sparkConf = new SparkConf(true)
    val initHive = sparkConf.getBoolean("spark.hive.init", defaultValue = true)
    var sparkSession: SparkSession = null
    if (initHive) {
      sparkSession = SparkSession.builder.config(sparkConf)
        .master("local[*]")
        .enableHiveSupport.appName("SparkSessionJobOnYarn").getOrCreate
    } else {
      sparkSession = SparkSession.builder.config(sparkConf)
        .master("local[*]")
        .appName("SparkSessionJobOnYarn").getOrCreate
    }
    sparkSession.conf.set("spark.sql.hive.version", "1.2.1")
    sparkSession.sparkContext.hadoopConfiguration.set("mapreduce.fileoutputcommitter.marksuccessfuljobs", "false")
    logger.info("SparkSession Inited")
    sparkSession
  }


  val MESSAGE_COLUMN_NAME: String = "message";

  private val PARTITION_COLUMN_NAME: String = "ds";

  def runJob(args: Array[String]): JobResult = {
    val config: MainArgsContext = JSON.parseObject(args(0), classOf[MainArgsContext])

    val dimFieldCode = config.getDimFieldCode

    val ds = config.getExecuteDate.toLocalDate.format(DateFormatterConstants.Formatter_yyyy_MM_dd)

    val messageDf = loadMessages(config).cache()

    val errorList = config.getFeatureSets.asScala.map(ctx => {
        val dimFeatureCode = ctx.getDimFeatureCode
        val features = ctx.getFeatures.asScala
        val resultDF = messageDf.map(new MapFunction[Row, Row] {
          override def call(msgRow: Row): Row = {
            val msg: String = msgRow.getAs(MESSAGE_COLUMN_NAME)
            val resultMapping: Map[String, Any] = features.map(feature => {
              // calc
              val res = msg;
              (feature.getName, res)
            }).toMap

            val resultList = resultMapping.get(dimFeatureCode) +: features
              .map(f => resultMapping.get(f.getName))
              .toList :+ ds
            RowFactory.create(resultList)
          }
        }, RowEncoder.apply(buildFeatureSetRowStruct(features, dimFieldCode)))

        val sinkConfig = new HiveDataFrameSinkConfig(
          config.getOutputDatabase,
          ctx.getOutputTable,
          Option.apply(Column.apply(PARTITION_COLUMN_NAME, DataTypes.StringType))
        )
        jobSink(new HiveDataFrameSink(sinkConfig), resultDF)
      })
      .filter(res => res.getCode != JobReasonCodeEnum.SPARK_RUN_SUCCESS.getCode)
      .toList

    if (errorList.isEmpty) (
      JobResult.of(JobReasonCodeEnum.SPARK_RUN_SUCCESS)
      ) else {
      new JobResult(JobReasonCodeEnum.UNKNOWN_ERROR.getCode, errorList.map(e => s"${e.getCode}: ${e.getMessage}").mkString("\n,"));
    }
  }

  def jobSink(sink: DataFrameSink,
              df: DataFrame,
              errorCode: JobReasonCodeEnum = JobReasonCodeEnum.SINK_ERROR)
             (implicit sparkSession: SparkSession, logger: SparkJobLogger): JobResult = {
    sink.sink(df) match {
      case Failure(t) => jobFail(errorCode, t)
      case Success(x) => logger.info(s"$x"); jobSuccess()
    }
  }

  def jobFail(reason: JobReasonCodeEnum, throwable: Throwable = null): JobResult = {
    if (throwable != null) {
      new JobResult(reason.getCode, jobName + reason.getDesc + throwable.getMessage + ExceptionUtils.getStackTrace(throwable))
    } else {
      new JobResult(reason.getCode, jobName + reason.getDesc)
    }
  }

  def jobSuccess(): JobResult = JobResult.of(JobReasonCodeEnum.SPARK_RUN_SUCCESS)


  private def buildFeatureSetRowStruct(features: mutable.Buffer[IndexConfig],
                                       dimFieldCode: String): StructType = {
    val structType = new StructType().add(dimFieldCode, DataTypes.StringType)
    features.foreach(r => {
      val dataType = r.getDataType match {
        case INTEGER => DataTypes.IntegerType
        case FLOAT => DataTypes.FloatType
        case BOOLEAN => DataTypes.BooleanType
        case DATE => DataTypes.DateType
        case _ => DataTypes.StringType
      }
      structType.add(r.getName, dataType)
    })
    structType.add(PARTITION_COLUMN_NAME, DataTypes.StringType)
  }


  def loadMessages(context: MainArgsContext): DataFrame
}
