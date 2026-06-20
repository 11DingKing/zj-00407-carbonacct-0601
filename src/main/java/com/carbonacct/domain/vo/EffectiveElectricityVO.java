package com.carbonacct.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class EffectiveElectricityVO {
    private Long unitId;
    private String unitName;
    private Long boosterStationId;
    private String stationName;
    private YearMonth statisticsMonth;
    private BigDecimal totalGridElectricity;
    private BigDecimal totalStationServiceElectricity;
    private BigDecimal totalAdjustmentElectricity;
    private BigDecimal effectiveCleanElectricity;
    private String traceabilityRemark;
}
