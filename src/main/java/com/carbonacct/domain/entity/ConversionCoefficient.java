package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import com.carbonacct.common.enums.ApprovalStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_conversion_coefficient")
public class ConversionCoefficient extends BaseEntity {
    private String version;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private BigDecimal standardCoalCoefficient;
    private BigDecimal carbonDioxideCoefficient;
    private BigDecimal householdElectricityConsumption;
    private ApprovalStatus approvalStatus;
    private String approver;
    private LocalDate approvalDate;
    private String approvalOpinion;
    private String operator;
    private String remark;
    private Boolean isCurrent;
}
