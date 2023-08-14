package io.github.wenbozhang.spark.demo.data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author wenbo.zhangw
 * @date 2023/8/9 11:43
 * <p>
 * 1. index
 * 2. output database
 * 3. output table
 * 4. input type
 * 5. message type
 * 6. data
 * -4.1 path
 * -4.2 input table
 * -4.2 column name
 */
public class MainArgsContext implements Serializable {

    private static final long serialVersionUID = 8303620293257472492L;

    private List<TianZuoFeatureSetContext> featureSets;

    private String outputDatabase;

    private String dimFieldCode;

    private LocalDateTime executeDate;

    private Integer inputType;

    private Integer messageType;

    /**
     * input type of ZIP
     */
    private List<String> paths;

    /**
     * input type of HIVE
     */
    private String inputTable;

    private String columnName;

    public void setFeatureSets(List<TianZuoFeatureSetContext> featureSets) {
        this.featureSets = featureSets;
    }

    public void setOutputDatabase(String outputDatabase) {
        this.outputDatabase = outputDatabase;
    }

    public void setInputType(Integer inputType) {
        this.inputType = inputType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public void setInputTable(String inputTable) {
        this.inputTable = inputTable;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public void setDimFieldCode(String dimFieldCode) {
        this.dimFieldCode = dimFieldCode;
    }

    public List<TianZuoFeatureSetContext> getFeatureSets() {
        return featureSets;
    }

    public String getOutputDatabase() {
        return outputDatabase;
    }

    public String getDimFieldCode() {
        return dimFieldCode;
    }

    public Integer getInputType() {
        return inputType;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public List<String> getPaths() {
        return paths;
    }

    public String getInputTable() {
        return inputTable;
    }

    public String getColumnName() {
        return columnName;
    }

    public LocalDateTime getExecuteDate() {
        return executeDate;
    }

    public void setExecuteDate(LocalDateTime executeDate) {
        this.executeDate = executeDate;
    }
}
