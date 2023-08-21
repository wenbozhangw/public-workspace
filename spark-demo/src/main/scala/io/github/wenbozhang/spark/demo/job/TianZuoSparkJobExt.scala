package io.github.wenbozhang.spark.demo.job

import cn.tongdun.captain.engine.operator.cmd.IndexOfflineCalculateCmd
import cn.tongdun.captain.engine.operator.execute.IndexOfflineExecutor
import cn.tongdun.captain.engine.operator.model.{Field, Index}
import cn.tongdun.tiance.job.client.enumeration.JobReasonCodeEnum
import cn.tongdun.tiance.job.client.model.job.JobResult
import cn.tongdun.tiance.job.common.constant.DateFormatterConstants
import cn.tongdun.tiance.job.common.job.SparkJobLogger
import cn.tongdun.tiance.job.connector.api.egress.DataFrameSink
import cn.tongdun.tiance.job.connector.api.ingress.Column
import cn.tongdun.tiance.job.connector.impl.egress.{HiveDataFrameSink, HiveDataFrameSinkConfig}
import cn.tongdun.tianzuo.captain.common.constant.ContentType
import cn.tongdun.tianzuo.captain.common.constant.DataType._
import com.alibaba.fastjson.{JSON, JSONArray}
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
abstract class TianZuoSparkJobExt extends Serializable {

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


  val MESSAGE_COLUMN_NAME: String = "message"

  private val PARTITION_COLUMN_NAME: String = "ds"

  def runJob(args: Array[String]): JobResult = {
    val config: MainArgsContext = JSON.parseObject(args(0), classOf[MainArgsContext])

    val dimFieldCode = config.getDimFieldCode

    val ds: String = config.getPartitionValue

    val messageDf = loadMessages(config).cache()
    val docFields = JSON.parseArray(config.getDocFields, classOf[Field])
    val documentTypeUuid = config.getDocumentTypeUuid

    val contentType = ContentType.of(config.getMessageType)
    val executor = new IndexOfflineExecutor()

    val errorList = config.getFeatureSets.asScala.map(ctx => {
      val dimFeatureCode = ctx.getDimFeatureCode
      val templateConfigList = ctx.getTemplateConfigList
      val features = ctx.getFeatures.asScala
      val resultDF = messageDf.map(new MapFunction[Row, Row] {
        override def call(content: Row): Row = {
          val fileContent: String = content.getAs(MESSAGE_COLUMN_NAME)
          val resultMapping: Map[String, Any] = features.map(feature => {
            // calc
            val cmd = new IndexOfflineCalculateCmd(
              fileContent,
              contentType,
              documentTypeUuid,
              ctx.getFeatureSetCode,
              ctx.getFeatures,
              docFields,
              feature,
              templateConfigList
            );
            val featureValue = executor.execute(cmd).get(0).getResult
            (feature.getName, featureValue)
          }).toMap

          val dimValue = resultMapping(dimFeatureCode).toString

          val resultList: Seq[Object] = dimValue +: features
            .map(f => resultMapping.getOrElse(f.getName, null).asInstanceOf[Object])
            .toList :+ ds
          RowFactory.create(resultList: _*)
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

    if (errorList.isEmpty) JobResult.of(JobReasonCodeEnum.SPARK_RUN_SUCCESS)
    else new JobResult(JobReasonCodeEnum.UNKNOWN_ERROR.getCode, errorList.map(e => s"${e.getCode}: ${e.getMessage}").mkString("\n,"))
  }

  private def jobSink(sink: DataFrameSink,
                      df: DataFrame,
                      errorCode: JobReasonCodeEnum = JobReasonCodeEnum.SINK_ERROR)
                     (implicit sparkSession: SparkSession, logger: SparkJobLogger): JobResult = {
    sink.sink(df) match {
      case Failure(t) => jobFail(errorCode, t)
      case Success(x) => logger.info(s"$x"); jobSuccess()
    }
  }

  private def jobFail(reason: JobReasonCodeEnum, throwable: Throwable = null): JobResult = {
    if (throwable != null) {
      new JobResult(reason.getCode, jobName + reason.getDesc + throwable.getMessage + ExceptionUtils.getStackTrace(throwable))
    } else {
      new JobResult(reason.getCode, jobName + reason.getDesc)
    }
  }

  private def jobSuccess(): JobResult = JobResult.of(JobReasonCodeEnum.SPARK_RUN_SUCCESS)


  private def buildFeatureSetRowStruct(features: mutable.Buffer[Index],
                                       dimFieldCode: String): StructType = {
    var structType = new StructType().add(dimFieldCode, DataTypes.StringType)
    features.foreach(r => {
      val dataType = r.getDataType match {
        case INTEGER => DataTypes.IntegerType
        case FLOAT => DataTypes.FloatType
        case BOOLEAN => DataTypes.BooleanType
        case DATE => DataTypes.DateType
        case _ => DataTypes.StringType
      }
      structType = structType.add(r.getName, dataType)
    })
    structType.add(PARTITION_COLUMN_NAME, DataTypes.StringType)
  }


  def loadMessages(context: MainArgsContext): DataFrame
}
