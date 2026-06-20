package com.carbonacct.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnitContributionVO {
    private Integer rank;
    private Long unitId;
    private String unitName;
    private BigDecimal effectiveElectricity;
    private BigDecimal contributionRate;
    private BigDecimal standardCoalSaving;
    private BigDecimal carbonDioxideReduction;
}
