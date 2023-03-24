package com.example.demo.db;

import lombok.Data;

/**
 * @auther hjt
 * date 2021/8/23
 * 审核信息扩展(包含了业务维度的信息)
 */
@Data
public class AuditExtensionDO extends AuditDO {

    /**
     * 策略标识
     */
    private String policyCode;

    /**
     * 策略名称
     */
    private String policyName;

    /**
     * 规则集标识
     */
    private String ruleSetUuid;
    /**
     * 规则集标识
     */
    private String ruleSetCode;

    /**
     * 规则集名称
     */
    private String ruleSetName;

    /**
     * 函数库标识
     */
    private String functionCode;

    /**
     * 函数库名称
     */
    private String functionName;

    /**
     * 决策工具标识
     */
    private String decisionToolCode;
    /**
     * 决策工具类型
     */
    private String decisionToolType;
    /**
     * 决策工具名称
     */
    private String decisionToolName;




}
