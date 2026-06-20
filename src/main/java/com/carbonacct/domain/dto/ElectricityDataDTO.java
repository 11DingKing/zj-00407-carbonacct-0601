package com.carbonacct.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class ElectricityDataDTO {
    @NotNull(message = "机组ID不能为空")
    private Long unitId;

    @NotNull(message = "升压站ID不能为空")
    private Long boosterStationId;

    @NotNull(message = "统计月份不能为空")
    private YearMonth statisticsMonth;

    @NotNull(message = "上网电量不能为空")
    @DecimalMin(value = "0", message = "上网电量不能为负数")
    private BigDecimal gridConnectedElectricity;

    @NotNull(message = "厂用电不能为空")
    @DecimalMin(value = "0", message = "厂用电不能为负数")
    private BigDecimal stationServiceElectricity;

    private String dataSource;
    private String operator;
    private String remark;
}
