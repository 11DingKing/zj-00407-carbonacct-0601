package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import com.carbonacct.common.enums.ApprovalStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_report_correction")
public class ReportCorrection extends BaseEntity {
    private Long reportId;
    private String reportNo;
    private Integer originalVersion;
    private Integer correctedVersion;
    private Long beforeCoefficientId;
    private Long afterCoefficientId;
    private String beforeCoefficientVersion;
    private String afterCoefficientVersion;
    private BigDecimal beforeEffectiveElectricity;
    private BigDecimal afterEffectiveElectricity;
    private BigDecimal beforeStandardCoalSaving;
    private BigDecimal afterStandardCoalSaving;
    private BigDecimal beforeCarbonDioxideReduction;
    private BigDecimal afterCarbonDioxideReduction;
    private BigDecimal beforeHouseholdCount;
    private BigDecimal afterHouseholdCount;
    private String correctionReason;
    private String correctedBy;
    private ApprovalStatus approvalStatus;
    private String approver;
    private LocalDateTime approvalTime;
    private String approvalOpinion;
    private String remark;
}
