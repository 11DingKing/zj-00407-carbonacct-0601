package com.carbonacct;

import com.carbonacct.common.enums.AdjustmentType;
import com.carbonacct.domain.dto.ElectricityAdjustmentDTO;
import com.carbonacct.domain.dto.ElectricityDataDTO;
import com.carbonacct.domain.vo.EffectiveElectricityVO;
import com.carbonacct.service.ElectricityCollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ElectricityCollectionServiceTest {

    @Autowired
    private ElectricityCollectionService service;

    private YearMonth testMonth;

    @BeforeEach
    void setUp() {
        testMonth = YearMonth.of(2025, 1);
    }

    @Test
    void testSaveElectricityData() {
        ElectricityDataDTO dto = new ElectricityDataDTO();
        dto.setUnitId(1L);
        dto.setBoosterStationId(1L);
        dto.setStatisticsMonth(testMonth);
        dto.setGridConnectedElectricity(new BigDecimal("1000"));
        dto.setStationServiceElectricity(new BigDecimal("50"));
        dto.setDataSource("电网计量系统");
        dto.setOperator("test");

        Long id = service.saveElectricityData(dto);
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    void testSaveAdjustment() {
        ElectricityAdjustmentDTO dto = new ElectricityAdjustmentDTO();
        dto.setUnitId(1L);
        dto.setBoosterStationId(1L);
        dto.setStatisticsMonth(testMonth);
        dto.setAdjustmentType(AdjustmentType.MAINTENANCE);
        dto.setAdjustmentElectricity(new BigDecimal("100"));
        dto.setDescription("1号机组例行检修");
        dto.setOperator("test");

        Long id = service.saveElectricityAdjustment(dto);
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    void testCalculateEffectiveElectricity() {
        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(2L);
        dataDTO.setBoosterStationId(1L);
        dataDTO.setStatisticsMonth(testMonth);
        dataDTO.setGridConnectedElectricity(new BigDecimal("2000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("100"));
        dataDTO.setOperator("test");
        service.saveElectricityData(dataDTO);

        ElectricityAdjustmentDTO adjDTO = new ElectricityAdjustmentDTO();
        adjDTO.setUnitId(2L);
        adjDTO.setBoosterStationId(1L);
        adjDTO.setStatisticsMonth(testMonth);
        adjDTO.setAdjustmentType(AdjustmentType.TRIAL_OPERATION);
        adjDTO.setAdjustmentElectricity(new BigDecimal("200"));
        adjDTO.setOperator("test");
        service.saveElectricityAdjustment(adjDTO);

        List<EffectiveElectricityVO> result = service.calculateEffectiveElectricity(testMonth, 2L, null);
        assertFalse(result.isEmpty());

        EffectiveElectricityVO vo = result.get(0);
        assertEquals(0, vo.getTotalGridElectricity().compareTo(new BigDecimal("2000")));
        assertEquals(0, vo.getTotalStationServiceElectricity().compareTo(new BigDecimal("100")));
        assertEquals(0, vo.getTotalAdjustmentElectricity().compareTo(new BigDecimal("200")));

        BigDecimal expected = new BigDecimal("2000").subtract(new BigDecimal("100")).subtract(new BigDecimal("200"));
        assertEquals(0, vo.getEffectiveCleanElectricity().compareTo(expected));
        assertNotNull(vo.getTraceabilityRemark());
        assertTrue(vo.getTraceabilityRemark().contains("试运行"));
    }

    @Test
    void testCalculateEffectiveElectricity_MultipleAdjustments() {
        YearMonth month = YearMonth.of(2025, 2);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(3L);
        dataDTO.setBoosterStationId(2L);
        dataDTO.setStatisticsMonth(month);
        dataDTO.setGridConnectedElectricity(new BigDecimal("5000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("200"));
        dataDTO.setOperator("test");
        service.saveElectricityData(dataDTO);

        ElectricityAdjustmentDTO adj1 = new ElectricityAdjustmentDTO();
        adj1.setUnitId(3L);
        adj1.setBoosterStationId(2L);
        adj1.setStatisticsMonth(month);
        adj1.setAdjustmentType(AdjustmentType.CURTAILMENT);
        adj1.setAdjustmentElectricity(new BigDecimal("300"));
        adj1.setOperator("test");
        service.saveElectricityAdjustment(adj1);

        ElectricityAdjustmentDTO adj2 = new ElectricityAdjustmentDTO();
        adj2.setUnitId(3L);
        adj2.setBoosterStationId(2L);
        adj2.setStatisticsMonth(month);
        adj2.setAdjustmentType(AdjustmentType.EQUIPMENT_FAULT);
        adj2.setAdjustmentElectricity(new BigDecimal("150"));
        adj2.setOperator("test");
        service.saveElectricityAdjustment(adj2);

        List<EffectiveElectricityVO> result = service.calculateEffectiveElectricity(month, 3L, null);
        assertFalse(result.isEmpty());

        EffectiveElectricityVO vo = result.get(0);
        assertEquals(0, vo.getTotalAdjustmentElectricity().compareTo(new BigDecimal("450")));

        BigDecimal expected = new BigDecimal("5000").subtract(new BigDecimal("200")).subtract(new BigDecimal("450"));
        assertEquals(0, vo.getEffectiveCleanElectricity().compareTo(expected));
    }
}
