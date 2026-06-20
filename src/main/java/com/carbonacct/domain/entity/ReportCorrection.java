package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_report_correction")
public class ReportCorrection extends BaseEntity {
    private Long reportId;
    private String reportNo;
    private Integer originalVersion;
    private Integer correctedVersion;
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
    private String approvalOpinion;
    private String remark;
}
