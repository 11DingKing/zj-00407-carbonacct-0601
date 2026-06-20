package com.carbonacct.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class CleanRevenueVO {
    private YearMonth statisticsMonth;
    private Long unitId;
    private String unitName;
    private Long boosterStationId;
    private String stationName;
    private BigDecimal effectiveCleanElectricity;
    private BigDecimal standardCoalSaving;
    private BigDecimal carbonDioxideReduction;
    private BigDecimal householdCount;
    private String coefficientVersion;
}
