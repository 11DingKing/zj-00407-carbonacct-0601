package com.carbonacct.service;

import com.carbonacct.domain.entity.ConversionCoefficient;
import com.carbonacct.domain.vo.CleanRevenueVO;
import com.carbonacct.domain.vo.EffectiveElectricityVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class CleanRevenueCalculationService {

    private final ElectricityCollectionService electricityCollectionService;
    private final ConversionCoefficientService coefficientService;
    private final UnitService unitService;
    private final BoosterStationService boosterStationService;

    public CleanRevenueCalculationService(ElectricityCollectionService electricityCollectionService,
                                          ConversionCoefficientService coefficientService,
                                          UnitService unitService,
                                          BoosterStationService boosterStationService) {
        this.electricityCollectionService = electricityCollectionService;
        this.coefficientService = coefficientService;
        this.unitService = unitService;
        this.boosterStationService = boosterStationService;
    }

    public List<CleanRevenueVO> calculateCleanRevenue(YearMonth month, Long unitId, Long stationId) {
        List<EffectiveElectricityVO> effectiveList = electricityCollectionService
                .calculateEffectiveElectricity(month, unitId, stationId);

        if (effectiveList.isEmpty()) {
            return new ArrayList<>();
        }

        List<CleanRevenueVO> result = new ArrayList<>();
        for (EffectiveElectricityVO effective : effectiveList) {
            ConversionCoefficient coefficient = coefficientService
                    .getCoefficientForMonth(effective.getStatisticsMonth());

            CleanRevenueVO vo = new CleanRevenueVO();
            vo.setStatisticsMonth(effective.getStatisticsMonth());
            vo.setUnitId(effective.getUnitId());
            vo.setUnitName(effective.getUnitName());
            vo.setBoosterStationId(effective.getBoosterStationId());
            vo.setStationName(effective.getStationName());
            vo.setEffectiveCleanElectricity(effective.getEffectiveCleanElectricity());
            vo.setCoefficientVersion(coefficient.getVersion());

            BigDecimal standardCoal = effective.getEffectiveCleanElectricity()
                    .multiply(coefficient.getStandardCoalCoefficient())
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
            vo.setStandardCoalSaving(standardCoal);

            BigDecimal co2 = effective.getEffectiveCleanElectricity()
                    .multiply(coefficient.getCarbonDioxideCoefficient())
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
            vo.setCarbonDioxideReduction(co2);

            BigDecimal annualConsumption = coefficient.getHouseholdElectricityConsumption();
            BigDecimal households = effective.getEffectiveCleanElectricity()
                    .multiply(BigDecimal.valueOf(12))
                    .divide(annualConsumption, 0, RoundingMode.DOWN);
            vo.setHouseholdCount(households);

            result.add(vo);
        }
        return result;
    }

    public CleanRevenueVO calculateByEffective(BigDecimal effectiveElectricity, YearMonth month,
                                               Long unitId, Long stationId) {
        ConversionCoefficient coefficient = coefficientService.getCoefficientForMonth(month);

        CleanRevenueVO vo = new CleanRevenueVO();
        vo.setStatisticsMonth(month);
        vo.setUnitId(unitId);
        vo.setUnitName(unitService.getUnitName(unitId));
        vo.setBoosterStationId(stationId);
        vo.setStationName(boosterStationService.getStationName(stationId));
        vo.setEffectiveCleanElectricity(effectiveElectricity);
        vo.setCoefficientVersion(coefficient.getVersion());

        BigDecimal standardCoal = effectiveElectricity
                .multiply(coefficient.getStandardCoalCoefficient())
                .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
        vo.setStandardCoalSaving(standardCoal);

        BigDecimal co2 = effectiveElectricity
                .multiply(coefficient.getCarbonDioxideCoefficient())
                .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
        vo.setCarbonDioxideReduction(co2);

        BigDecimal annualConsumption = coefficient.getHouseholdElectricityConsumption();
        BigDecimal households = effectiveElectricity
                .multiply(BigDecimal.valueOf(12))
                .divide(annualConsumption, 0, RoundingMode.DOWN);
        vo.setHouseholdCount(households);

        return vo;
    }
}
