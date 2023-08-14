package io.github.wenbozhang.spark.demo.job

import io.github.wenbozhang.spark.demo.data.MainArgsContext
import org.apache.spark.sql.DataFrame

import scala.collection.JavaConverters.asScalaBufferConverter

/**
 * @author wenbo.zhangw
 * @date 2023/8/8 14:40
 */
class ZipInputSourceJob extends TianZuoSparkJobExt {

  override val jobName: String = "TianZuoZipJob"

  override def loadMessages(config: MainArgsContext): DataFrame = {
    val paths: Seq[String] = config.getPaths.asScala
    sparkSession.read
      .text(paths:_*)
      .withColumnRenamed("value", MESSAGE_COLUMN_NAME)
  }
}
