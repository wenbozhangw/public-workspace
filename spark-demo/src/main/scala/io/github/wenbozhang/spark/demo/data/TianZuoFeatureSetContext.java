package io.github.wenbozhang.spark.demo.data;

import cn.tongdun.captain.engine.operator.model.Index;
import cn.tongdun.captain.engine.operator.model.Template;
import cn.tongdun.tianzuo.captain.client.entity.dto.IndexConfig;

import java.io.Serializable;
import java.util.List;

/**
 * @author wenbo.zhangw
 * @date 2023/8/9 20:31
 */
public class TianZuoFeatureSetContext implements Serializable {

    private static final long serialVersionUID = -7934994400434335335L;

    private List<Index> features;

    private List<Template> templateConfigList;

    private String featureSetCode;

    private Integer featureSetVersion;

    private String dimFeatureCode;

    private String outputTable;

    public void setFeatures(List<Index> features) {
        this.features = features;
    }

    public void setFeatureSetCode(String featureSetCode) {
        this.featureSetCode = featureSetCode;
    }

    public void setFeatureSetVersion(Integer featureSetVersion) {
        this.featureSetVersion = featureSetVersion;
    }

    public void setOutputTable(String outputTable) {
        this.outputTable = outputTable;
    }

    public void setDimFeatureCode(String dimFeatureCode) {
        this.dimFeatureCode = dimFeatureCode;
    }

    public List<Index> getFeatures() {
        return features;
    }

    public String getFeatureSetCode() {
        return featureSetCode;
    }

    public Integer getFeatureSetVersion() {
        return featureSetVersion;
    }

    public String getDimFeatureCode() {
        return dimFeatureCode;
    }

    public String getOutputTable() {
        return outputTable;
    }

    public List<Template> getTemplateConfigList() {
        return templateConfigList;
    }

    public void setTemplateConfigList(List<Template> templateConfigList) {
        this.templateConfigList = templateConfigList;
    }
}

