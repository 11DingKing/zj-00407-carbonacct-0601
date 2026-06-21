package com.carbonacct.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AnnualStatisticsVO {
    private Integer year;
    private BigDecimal totalEffectiveElectricity;
    private BigDecimal totalStandardCoalSaving;
    private BigDecimal totalCarbonDioxideReduction;
    private BigDecimal totalHouseholdCount;
    private List<UnitContributionVO> unitContributions;
    private List<AbnormalMonthVO> abnormalMonths;
    private Boolean hasCorrections;
    private BigDecimal originalTotalEffectiveElectricity;
    private BigDecimal originalTotalStandardCoalSaving;
    private BigDecimal originalTotalCarbonDioxideReduction;
    private BigDecimal originalTotalHouseholdCount;
    private BigDecimal diffEffectiveElectricity;
    private BigDecimal diffStandardCoalSaving;
    private BigDecimal diffCarbonDioxideReduction;
    private BigDecimal diffHouseholdCount;
    private Integer correctionCount;
}
