package io.github.wenbozhang.spark.demo.job

import io.github.wenbozhang.spark.demo.data.MainArgsContext
import org.apache.spark.sql.types.{DataTypes, StructType}
import org.apache.spark.sql.{DataFrame, Row, RowFactory}

import java.util.stream.Collectors
import java.util.zip.ZipInputStream
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.io.Source

/**
 * @author wenbo.zhangw
 * @date 2023/8/8 14:40
 */
class ZipInputSourceJob extends TianZuoSparkJobExt {

  override val jobName: String = "TianZuoZipJob"

  override def loadMessages(config: MainArgsContext): DataFrame = {
    val paths = config.getPaths.stream().collect(Collectors.joining(","));
    val rows = sparkSession.sparkContext
      .binaryFiles(paths)
      .map { case (name, pds) =>
        val zis = new ZipInputStream(pds.open());
        val content = Stream.continually(zis.getNextEntry)
          .takeWhile(_ != null)
          .map(_ => {
            Source.fromInputStream(zis, "UTF-8").getLines().toStream.mkString("\n")
          })#::: {zis.close(); Stream.empty[String]}
        Row(content:_*)
      }
    val structType = new StructType().add(MESSAGE_COLUMN_NAME, DataTypes.StringType)
    sparkSession.createDataFrame(rows, structType)
  }
}
