package com.carbonacct.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportCorrectionDTO {
    @NotNull(message = "报表ID不能为空")
    private Long reportId;

    private Long afterCoefficientId;

    @NotNull(message = "更正后有效清洁电量不能为空")
    private BigDecimal effectiveCleanElectricity;

    @NotNull(message = "更正后节约标准煤不能为空")
    private BigDecimal standardCoalSaving;

    @NotNull(message = "更正后减少二氧化碳不能为空")
    private BigDecimal carbonDioxideReduction;

    @NotNull(message = "更正后可供居民户数不能为空")
    private BigDecimal householdCount;

    @NotBlank(message = "更正原因不能为空")
    private String correctionReason;

    private String correctedBy;
    private String approver;
    private String approvalOpinion;
    private String remark;
}
