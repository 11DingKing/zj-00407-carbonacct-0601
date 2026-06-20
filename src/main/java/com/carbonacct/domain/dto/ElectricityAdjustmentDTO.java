package com.carbonacct.domain.dto;

import com.carbonacct.common.enums.AdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Data
public class ElectricityAdjustmentDTO {
    @NotNull(message = "机组ID不能为空")
    private Long unitId;

    @NotNull(message = "升压站ID不能为空")
    private Long boosterStationId;

    @NotNull(message = "统计月份不能为空")
    private YearMonth statisticsMonth;

    @NotNull(message = "调整类型不能为空")
    private AdjustmentType adjustmentType;

    @NotNull(message = "调整电量不能为空")
    @DecimalMin(value = "0", message = "调整电量不能为负数")
    private BigDecimal adjustmentElectricity;

    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String approvalDoc;
    private String operator;
    private String remark;
}
