package io.github.wenbozhang.spark.demo

import cn.tongdun.tiance.job.client.enumeration.JobReasonCodeEnum
import cn.tongdun.tiance.job.client.model.job.JobResult
import cn.tongdun.tiance.job.client.util.Base64Utils
import cn.tongdun.tiance.job.common.exception.ServiceException
import cn.tongdun.tiance.job.common.job.SparkJobLogger
import com.alibaba.fastjson.JSONPath
import io.github.wenbozhang.spark.demo.enums.TianZuoInputTypeEnum
import io.github.wenbozhang.spark.demo.job.{HiveInputSourceJob, TianZuoSparkJobExt, ZipInputSourceJob}

import java.util.concurrent.CountDownLatch

object Main {

  private val INPUT_TYPE_KEY: String = "$.inputType"

  private val JOB_LOG_PREFIX: String = "[天座指标取数]"

  val logger: SparkJobLogger = new SparkJobLogger

  private var args: String = "{\\\"featureSets\\\":[{\\\"features\\\":[{\\\"uuid\\\":\\\"228d9f18ae4a4a43856c55c3dd5742d7\\\",\\\"gmtCreate\\\":1692272553000,\\\"gmtModify\\\":1692352860000,\\\"createBy\\\":\\\"zht\\\",\\\"modifyBy\\\":\\\"zht\\\",\\\"name\\\":\\\"renwuliu_copy_copy_copy_copy_copy_copy_copy\\\",\\\"displayName\\\":\\\"我修改好了\\\",\\\"documentTypeUuid\\\":\\\"955952849bec479f93c206de0d46ad6c\\\",\\\"status\\\":\\\"SAVED\\\",\\\"indexType\\\":\\\"CUSTOM\\\",\\\"dataType\\\":\\\"STRING\\\",\\\"version\\\":4,\\\"indexCategoryUuid\\\":\\\"f4504f9a012f48fc8674ee406f58eef9\\\",\\\"paramTemplate\\\":\\\"[]\\\",\\\"content\\\":\\\"/**\\\\n * 1111返回参数为包装类，与数据类型一一对应不可以更改。\\\\n * 模板采用Janio进行编译，部分Java语法无法兼容，\\\\n * 具体兼容语法请参考 @link http://janino-compiler.github.io/janino/#janino-as-a-code-manipulator\\\\n * @params 为参数配置中的值 ，doc为待计算的报文，会根据报文结构的不同被转成CommonJsonDocument/CommonXmlDocument/CommonTextDocument这三个对象\\\\n */\\\\n//例：\\\\npublic String execute(Map params,IDocInterface doc) {\\\\n       return \\\\\\\"76231\\\\\\\";\\\\n}\\\",\\\"description\\\":\\\"2132\\\",\\\"appName\\\":\\\"test_app\\\",\\\"paramMap\\\":{\\\"defaultValue\\\":\\\"-999\\\"}},{\\\"uuid\\\":\\\"9c56a164c96843d5b4f57d8135cb2c30\\\",\\\"gmtCreate\\\":1692349612000,\\\"gmtModify\\\":1692350107000,\\\"createBy\\\":\\\"bbq\\\",\\\"modifyBy\\\":\\\"bbq\\\",\\\"name\\\":\\\"renwuliu\\\",\\\"displayName\\\":\\\"renwuliu\\\",\\\"documentTypeUuid\\\":\\\"955952849bec479f93c206de0d46ad6c\\\",\\\"status\\\":\\\"SAVED\\\",\\\"indexType\\\":\\\"VAR\\\",\\\"dataType\\\":\\\"INTEGER\\\",\\\"version\\\":2,\\\"indexCategoryUuid\\\":\\\"f4504f9a012f48fc8674ee406f58eef9\\\",\\\"paramTemplate\\\":\\\"[{\\\\\\\"name\\\\\\\":\\\\\\\"field\\\\\\\",\\\\\\\"value\\\\\\\":\\\\\\\"$\\\\\\\"},{\\\\\\\"name\\\\\\\":\\\\\\\"num\\\\\\\",\\\\\\\"value\\\\\\\":\\\\\\\"exist\\\\\\\"}]\\\",\\\"defaultValue\\\":\\\"123\\\",\\\"content\\\":\\\"{\\\\\\\"uuid\\\\\\\":\\\\\\\"9a3f4fccde6945138050a0c2cbaf98a4\\\\\\\",\\\\\\\"name\\\\\\\":\\\\\\\"$\\\\\\\",\\\\\\\"path\\\\\\\":\\\\\\\"$\\\\\\\",\\\\\\\"displayName\\\\\\\":\\\\\\\"$\\\\\\\",\\\\\\\"sourceName\\\\\\\":\\\\\\\"$\\\\\\\",\\\\\\\"sourcePath\\\\\\\":\\\\\\\"$\\\\\\\",\\\\\\\"parentUuid\\\\\\\":null,\\\\\\\"parentDataType\\\\\\\":null,\\\\\\\"documentTypeUuid\\\\\\\":\\\\\\\"955952849bec479f93c206de0d46ad6c\\\\\\\",\\\\\\\"dataType\\\\\\\":\\\\\\\"OBJECT\\\\\\\",\\\\\\\"topDataType\\\\\\\":\\\\\\\"OBJECT\\\\\\\",\\\\\\\"dataTypes\\\\\\\":[\\\\\\\"OBJECT\\\\\\\",\\\\\\\"OBJECT\\\\\\\"],\\\\\\\"dynamic\\\\\\\":0,\\\\\\\"displayOrder\\\\\\\":0,\\\\\\\"config\\\\\\\":null,\\\\\\\"status\\\\\\\":\\\\\\\"ENABLED\\\\\\\",\\\\\\\"description\\\\\\\":null,\\\\\\\"createBy\\\\\\\":\\\\\\\"admin\\\\\\\",\\\\\\\"modifyBy\\\\\\\":\\\\\\\"admin\\\\\\\",\\\\\\\"children\\\\\\\":[{\\\\\\\"uuid\\\\\\\":\\\\\\\"75b72aaf43eb4468ab67069b0926f1c4\\\\\\\",\\\\\\\"name\\\\\\\":\\\\\\\"dasd\\\\\\\",\\\\\\\"path\\\\\\\":\\\\\\\"$.dasd\\\\\\\",\\\\\\\"displayName\\\\\\\":\\\\\\\"dasd\\\\\\\",\\\\\\\"sourceName\\\\\\\":\\\\\\\"dasd\\\\\\\",\\\\\\\"sourcePath\\\\\\\":\\\\\\\"$.dasd\\\\\\\",\\\\\\\"parentUuid\\\\\\\":\\\\\\\"9a3f4fccde6945138050a0c2cbaf98a4\\\\\\\",\\\\\\\"parentDataType\\\\\\\":\\\\\\\"OBJECT\\\\\\\",\\\\\\\"documentTypeUuid\\\\\\\":\\\\\\\"955952849bec479f93c206de0d46ad6c\\\\\\\",\\\\\\\"dataType\\\\\\\":\\\\\\\"INTEGER\\\\\\\",\\\\\\\"topDataType\\\\\\\":\\\\\\\"NUMBER\\\\\\\",\\\\\\\"dataTypes\\\\\\\":[\\\\\\\"NUMBER\\\\\\\",\\\\\\\"INTEGER\\\\\\\"],\\\\\\\"dynamic\\\\\\\":0,\\\\\\\"displayOrder\\\\\\\":1,\\\\\\\"config\\\\\\\":null,\\\\\\\"status\\\\\\\":\\\\\\\"ENABLED\\\\\\\",\\\\\\\"description\\\\\\\":null,\\\\\\\"createBy\\\\\\\":\\\\\\\"admin\\\\\\\",\\\\\\\"modifyBy\\\\\\\":\\\\\\\"admin\\\\\\\",\\\\\\\"children\\\\\\\":null}]}\\\",\\\"appName\\\":\\\"test_app\\\",\\\"paramMap\\\":{\\\"field\\\":\\\"$\\\",\\\"defaultValue\\\":\\\"123\\\",\\\"num\\\":\\\"exist\\\"}}],\\\"featureSetCode\\\":\\\"renwuliu\\\",\\\"featureSetVersion\\\":1,\\\"dimFeatureCode\\\":\\\"renwuliu\\\",\\\"outputTable\\\":\\\"tianzuofeaturesetresult_renwuliu_1\\\"}],\\\"outputDatabase\\\":\\\"default\\\",\\\"dimFieldCode\\\":\\\"S_S_CUSTNO\\\",\\\"inputType\\\":2,\\\"contentTypeCode\\\":2,\\\"documentTypeUuid\\\":\\\"955952849bec479f93c206de0d46ad6c\\\",\\\"executeDate\\\":{\\\"year\\\":2023,\\\"month\\\":\\\"AUGUST\\\",\\\"hour\\\":11,\\\"minute\\\":40,\\\"second\\\":49,\\\"dayOfMonth\\\":20,\\\"dayOfWeek\\\":\\\"SUNDAY\\\",\\\"dayOfYear\\\":232,\\\"nano\\\":350000000,\\\"monthValue\\\":8,\\\"chronology\\\":{\\\"calendarType\\\":\\\"iso8601\\\",\\\"id\\\":\\\"ISO\\\"}},\\\"docFields\\\":\\\"[{\\\\\\\"dataType\\\\\\\":\\\\\\\"INTEGER\\\\\\\",\\\\\\\"displayName\\\\\\\":\\\\\\\"dasd\\\\\\\",\\\\\\\"displayOrder\\\\\\\":1,\\\\\\\"documentTypeUuid\\\\\\\":\\\\\\\"955952849bec479f93c206de0d46ad6c\\\\\\\",\\\\\\\"dynamic\\\\\\\":false,\\\\\\\"name\\\\\\\":\\\\\\\"dasd\\\\\\\",\\\\\\\"parentUuid\\\\\\\":\\\\\\\"9a3f4fccde6945138050a0c2cbaf98a4\\\\\\\",\\\\\\\"sourceName\\\\\\\":\\\\\\\"dasd\\\\\\\",\\\\\\\"status\\\\\\\":\\\\\\\"ENABLED\\\\\\\",\\\\\\\"topDataType\\\\\\\":\\\\\\\"NUMBER\\\\\\\",\\\\\\\"uuid\\\\\\\":\\\\\\\"75b72aaf43eb4468ab67069b0926f1c4\\\\\\\"},{\\\\\\\"dataType\\\\\\\":\\\\\\\"OBJECT\\\\\\\",\\\\\\\"displayName\\\\\\\":\\\\\\\"$\\\\\\\",\\\\\\\"displayOrder\\\\\\\":0,\\\\\\\"documentTypeUuid\\\\\\\":\\\\\\\"955952849bec479f93c206de0d46ad6c\\\\\\\",\\\\\\\"dynamic\\\\\\\":false,\\\\\\\"name\\\\\\\":\\\\\\\"$\\\\\\\",\\\\\\\"sourceName\\\\\\\":\\\\\\\"$\\\\\\\",\\\\\\\"status\\\\\\\":\\\\\\\"ENABLED\\\\\\\",\\\\\\\"topDataType\\\\\\\":\\\\\\\"OBJECT\\\\\\\",\\\\\\\"uuid\\\\\\\":\\\\\\\"9a3f4fccde6945138050a0c2cbaf98a4\\\\\\\"}]\\\",\\\"paths\\\":[\\\"/tianzuo/*.zip\\\"]}";

  def main(arg: Array[String]): Unit = {
    logger.info(s"$JOB_LOG_PREFIX 任务启动，参数列表为：${args.mkString(",")}")

    val param: String = this.tryDecodeParam(args)

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
    new CountDownLatch(1).await()
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