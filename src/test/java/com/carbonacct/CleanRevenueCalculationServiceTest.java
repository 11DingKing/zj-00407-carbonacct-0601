package com.carbonacct;

import com.carbonacct.common.enums.AdjustmentType;
import com.carbonacct.domain.dto.ElectricityAdjustmentDTO;
import com.carbonacct.domain.dto.ElectricityDataDTO;
import com.carbonacct.domain.vo.CleanRevenueVO;
import com.carbonacct.service.CleanRevenueCalculationService;
import com.carbonacct.service.ElectricityCollectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CleanRevenueCalculationServiceTest {

    @Autowired
    private CleanRevenueCalculationService calculationService;

    @Autowired
    private ElectricityCollectionService electricityService;

    @Test
    void testCalculateCleanRevenue() {
        YearMonth month = YearMonth.of(2025, 3);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(1L);
        dataDTO.setBoosterStationId(1L);
        dataDTO.setStatisticsMonth(month);
        dataDTO.setGridConnectedElectricity(new BigDecimal("10000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("300"));
        dataDTO.setOperator("test");
        electricityService.saveElectricityData(dataDTO);

        ElectricityAdjustmentDTO adjDTO = new ElectricityAdjustmentDTO();
        adjDTO.setUnitId(1L);
        adjDTO.setBoosterStationId(1L);
        adjDTO.setStatisticsMonth(month);
        adjDTO.setAdjustmentType(AdjustmentType.CURTAILMENT);
        adjDTO.setAdjustmentElectricity(new BigDecimal("500"));
        adjDTO.setOperator("test");
        electricityService.saveElectricityAdjustment(adjDTO);

        List<CleanRevenueVO> result = calculationService.calculateCleanRevenue(month, 1L, null);
        assertFalse(result.isEmpty());

        CleanRevenueVO vo = result.get(0);
        assertNotNull(vo.getEffectiveCleanElectricity());
        assertNotNull(vo.getStandardCoalSaving());
        assertNotNull(vo.getCarbonDioxideReduction());
        assertNotNull(vo.getHouseholdCount());
        assertNotNull(vo.getCoefficientVersion());

        BigDecimal expectedEffective = new BigDecimal("10000")
                .subtract(new BigDecimal("300"))
                .subtract(new BigDecimal("500"));
        assertEquals(0, vo.getEffectiveCleanElectricity().compareTo(expectedEffective));

        assertTrue(vo.getStandardCoalSaving().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(vo.getCarbonDioxideReduction().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(vo.getHouseholdCount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculateByEffective() {
        YearMonth month = YearMonth.of(2025, 4);
        BigDecimal effective = new BigDecimal("9200");

        CleanRevenueVO vo = calculationService.calculateByEffective(effective, month, 2L, 1L);

        assertNotNull(vo);
        assertEquals(0, vo.getEffectiveCleanElectricity().compareTo(effective));
        assertTrue(vo.getStandardCoalSaving().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(vo.getCarbonDioxideReduction().compareTo(BigDecimal.ZERO) > 0);

        BigDecimal coalExpected = effective
                .multiply(new BigDecimal("0.309"))
                .divide(new BigDecimal("1000"), 4, java.math.RoundingMode.HALF_UP);
        assertEquals(0, vo.getStandardCoalSaving().compareTo(coalExpected));
    }
}
