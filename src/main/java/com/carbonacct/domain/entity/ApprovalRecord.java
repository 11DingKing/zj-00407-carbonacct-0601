package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import com.carbonacct.common.enums.ApprovalStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_approval_record")
public class ApprovalRecord extends BaseEntity {
    private String businessType;
    private Long businessId;
    private String businessNo;
    private ApprovalStatus approvalStatus;
    private String approver;
    private String approvalOpinion;
    private String operator;
    private String remark;
}
