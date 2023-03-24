package com.example.demo.db.mapper.audit;


import com.example.demo.db.AuditDO;
import com.example.demo.db.AuditExtensionDO;
import com.example.demo.db.AuditQuery;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

import java.util.List;

public interface AuditDOMapper extends Mapper<AuditDO>, InsertListMapper<AuditDO> {

    /**
     * 审核查询-策略
     *
     * @param auditQuery
     * @return
     */
    List<AuditExtensionDO> selectForPolicy(AuditQuery auditQuery);

    /**
     * 审核查询-规则集
     *
     * @param auditQuery
     * @return
     */
    List<AuditExtensionDO> selectForRuleSet(AuditQuery auditQuery);

    /**
     * 审核查询-函数库
     *
     * @param auditQuery
     * @return
     */
    List<AuditExtensionDO> selectForFunction(AuditQuery auditQuery);

    /**
     * 审核查询-决策工具
     *
     * @param auditQuery
     * @return
     */
    List<AuditExtensionDO> selectForDecisionTool(AuditQuery auditQuery);
}