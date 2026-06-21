package com.carbonacct.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class AbnormalMonthVO {
    private YearMonth statisticsMonth;
    private BigDecimal effectiveElectricity;
    private BigDecimal fluctuationRate;
    private BigDecimal avgElectricity;
    private String anomalyType;
    private Boolean hasCorrections;
    private BigDecimal originalEffectiveElectricity;
    private BigDecimal originalFluctuationRate;
    private String originalAnomalyType;
    private BigDecimal diffEffectiveElectricity;
    private Boolean anomalyChanged;
}
