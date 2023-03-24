package com.example.demo.db;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @auther hjt
 * date 2021/8/23
 * 审核查询
 */
@Getter
@Setter
public class AuditQuery {

    /**
     * 应用标识
     */
    private String appCode;

    /**
     * 机构标识，多个之间用,隔开
     */
    private String orgCode;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 当前页
     */
    private Integer curPage;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 业务来源类型
     */
    private Integer sourceType;

    /**
     * 审核类型
     */
    private Integer auditType;

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
    private String ruleSetCode;

    /**
     * 规则集名称
     */
    private String ruleSetName;

    /**
     * 操作人
     */
    private String submitUser;

    /**
     * 状态集合
     */
    private List<Integer> statusList;

    /**
     * 机构编码集合
     */
    private List<String> orgCodeList;

    /**
     * 应用code集合
     */
    private List<String> appCodeList;

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
     * 决策工具名称
     */
    private String decisionToolName;

    /**
     * uuid列表
     */
    private List<String> uuidList;
}
