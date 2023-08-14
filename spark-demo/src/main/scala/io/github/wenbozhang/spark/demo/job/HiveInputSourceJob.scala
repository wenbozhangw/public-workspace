package io.github.wenbozhang.spark.demo.job

import io.github.wenbozhang.spark.demo.data.MainArgsContext
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col

/**
 * @author wenbo.zhangw
 * @date 2023/8/8 14:42
 */
class HiveInputSourceJob extends TianZuoSparkJobExt {

  override val jobName: String = "TianZuoHiveJob"

  override def loadMessages(config: MainArgsContext): DataFrame = {
    val inputTable = config.getInputTable
    val columnName = config.getColumnName

    sparkSession.sqlContext
      .table(inputTable)
      .withColumn(columnName, col(columnName))
      .withColumnRenamed(columnName, MESSAGE_COLUMN_NAME)
  }


}
