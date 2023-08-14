package io.github.wenbozhang.spark.deom;

import cn.tongdun.tiance.job.client.enumeration.JobReasonCodeEnum
import cn.tongdun.tiance.job.client.model.job.JobResult
import cn.tongdun.tiance.job.client.util.Base64Utils
import cn.tongdun.tiance.job.common.exception.ServiceException
import cn.tongdun.tiance.job.common.job.{SparkJob, SparkJobLogger}
import com.alibaba.fastjson.JSONPath
import io.github.wenbozhang.spark.demo.enums.TianZuoInputTypeEnum
import io.github.wenbozhang.spark.demo.job.{HiveInputSourceJob, TianZuoSparkJobExt, ZipInputSourceJob}
import org.apache.spark.sql.SparkSession

import java.util.concurrent.CountDownLatch;

object Main {

  private val INPUT_TYPE_KEY: String = "$.inputType"

  private val JOB_LOG_PREFIX: String = "[天座指标取数]"

  val logger: SparkJobLogger = new SparkJobLogger

  def main(args: Array[String]): Unit = {
    logger.info(s"$JOB_LOG_PREFIX 任务启动，参数列表为：${args.mkString(",")}")

    val param: String = this.tryDecodeParam(args(0))

    logger.info(s"$JOB_LOG_PREFIX 参数解码后：$param")
    val inputType: Integer = JSONPath.read(param, INPUT_TYPE_KEY, classOf[Int])
    val inputTypeEnum: TianZuoInputTypeEnum = TianZuoInputTypeEnum.getByCode(inputType)

    val sparkJob: TianZuoSparkJobExt[Nothing] = inputTypeEnum match {
      case TianZuoInputTypeEnum.ZIP => new ZipInputSourceJob
      case TianZuoInputTypeEnum.HIVE => new HiveInputSourceJob
      case _ => throw new ServiceException("未指定 InputType");
    }

    var jobResult: JobResult = JobResult.of(JobReasonCodeEnum.UNKNOWN_ERROR)
    try {
      jobResult = sparkJob.runJob(Array(param))
    } catch {
      case e: Exception =>
        logger.error(s"$JOB_LOG_PREFIX 未处理的任务运行时异常", e)
        throw new ServiceException(e)
    }
    if (jobResult.getCode != JobReasonCodeEnum.SPARK_RUN_SUCCESS.getCode) {
      logger.error(s"$JOB_LOG_PREFIX ${jobResult.getMessage}")
      throw new ServiceException(s"$JOB_LOG_PREFIX ${jobResult.getMessage}")
    }
    logger.info("execute success!")
    new CountDownLatch(1).await();
  }

  private def tryDecodeParam(param: String): String = {
    var decoded: String = null
    try {
      decoded = Base64Utils.decode(param)
    } catch {
      case _: Exception =>
        logger.warn(s"$param is not base64 encoded, this may indicate some problems")
        decoded = param
    }
    decoded
  }
}