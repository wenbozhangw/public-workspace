package com.example.demo.db;

/**
 * @author wenbo.zhangw
 * @date 2023/3/20 17:21
 */

import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(
        name = "tiangong_audit_info"
)
public class AuditDO {
    @Id
    @Column(
            name = "id"
    )
    @KeySql(
            useGeneratedKeys = true
    )
    private Long id;
    @Column(
            name = "uuid"
    )
    private String uuid;
    @Column(
            name = "source_type"
    )
    private Integer sourceType;
    @Column(
            name = "source_id"
    )
    private String sourceId;
    @Column(
            name = "status"
    )
    private Integer status;
    @Column(
            name = "app_code"
    )
    private String appCode;
    @Column(
            name = "org_code"
    )
    private String orgCode;
    @Column(
            name = "audit_type"
    )
    private Integer auditType;
    @Column(
            name = "gmt_create"
    )
    private Date gmtCreate;
    @Column(
            name = "gmt_modify"
    )
    private Date gmtModify;
    @Column(
            name = "extend"
    )
    private String extend;
    @Column(
            name = "submit_user"
    )
    private String submitUser;
    @Column(
            name = "remark"
    )
    private String remark;
    @Column(
            name = "audit_user"
    )
    private String auditUser;
    @Column(
            name = "re_audit_user"
    )
    private String reAuditUser;

    public AuditDO() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getSourceType() {
        return this.sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return this.sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAppCode() {
        return this.appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getOrgCode() {
        return this.orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public Integer getAuditType() {
        return this.auditType;
    }

    public void setAuditType(Integer auditType) {
        this.auditType = auditType;
    }

    public Date getGmtCreate() {
        return this.gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModify() {
        return this.gmtModify;
    }

    public void setGmtModify(Date gmtModify) {
        this.gmtModify = gmtModify;
    }

    public String getExtend() {
        return this.extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }

    public String getSubmitUser() {
        return this.submitUser;
    }

    public void setSubmitUser(String submitUser) {
        this.submitUser = submitUser;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getAuditUser() {
        return this.auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    public String getReAuditUser() {
        return this.reAuditUser;
    }

    public void setReAuditUser(String reAuditUser) {
        this.reAuditUser = reAuditUser;
    }
}
