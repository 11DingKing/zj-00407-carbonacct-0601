package com.carbonacct;

import com.carbonacct.common.enums.ApprovalStatus;
import com.carbonacct.domain.dto.ApprovalDTO;
import com.carbonacct.domain.dto.ConversionCoefficientDTO;
import com.carbonacct.domain.entity.ConversionCoefficient;
import com.carbonacct.service.ConversionCoefficientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConversionCoefficientServiceTest {

    @Autowired
    private ConversionCoefficientService service;

    @Test
    void testCreateCoefficient() {
        ConversionCoefficientDTO dto = new ConversionCoefficientDTO();
        dto.setVersion("V3.0-TEST");
        dto.setEffectiveDate(LocalDate.of(2026, 1, 1));
        dto.setStandardCoalCoefficient(new BigDecimal("0.315"));
        dto.setCarbonDioxideCoefficient(new BigDecimal("0.995"));
        dto.setHouseholdElectricityConsumption(new BigDecimal("4700"));
        dto.setOperator("test");

        Long id = service.createCoefficient(dto);
        assertNotNull(id);
        assertTrue(id > 0);

        ConversionCoefficient saved = service.getById(id);
        assertEquals(ApprovalStatus.PENDING, saved.getApprovalStatus());
        assertFalse(saved.getIsCurrent());
    }

    @Test
    void testApproveCoefficient() {
        ConversionCoefficientDTO dto = new ConversionCoefficientDTO();
        dto.setVersion("V4.0-TEST");
        dto.setEffectiveDate(LocalDate.of(2026, 6, 1));
        dto.setStandardCoalCoefficient(new BigDecimal("0.320"));
        dto.setCarbonDioxideCoefficient(new BigDecimal("1.000"));
        dto.setHouseholdElectricityConsumption(new BigDecimal("4800"));
        dto.setOperator("test");
        Long id = service.createCoefficient(dto);

        ApprovalDTO approvalDTO = new ApprovalDTO();
        approvalDTO.setBusinessType("CONVERSION_COEFFICIENT");
        approvalDTO.setBusinessId(id);
        approvalDTO.setApprovalStatus(ApprovalStatus.APPROVED);
        approvalDTO.setApprover("审批人A");
        approvalDTO.setApprovalOpinion("同意生效");
        approvalDTO.setOperator("test");

        service.approveCoefficient(approvalDTO);

        ConversionCoefficient approved = service.getById(id);
        assertEquals(ApprovalStatus.APPROVED, approved.getApprovalStatus());
        assertEquals("审批人A", approved.getApprover());
        assertNotNull(approved.getApprovalDate());
        assertTrue(approved.getIsCurrent());
    }

    @Test
    void testGetCoefficientForMonth() {
        ConversionCoefficient coefficient = service.getCoefficientForMonth(YearMonth.of(2025, 6));
        assertNotNull(coefficient);
        assertNotNull(coefficient.getStandardCoalCoefficient());
        assertNotNull(coefficient.getCarbonDioxideCoefficient());
    }

    @Test
    void testGetCurrentCoefficient() {
        ConversionCoefficient current = service.getCurrentCoefficient();
        assertNotNull(current);
        assertTrue(current.getIsCurrent());
        assertEquals(ApprovalStatus.APPROVED, current.getApprovalStatus());
    }

    @Test
    void testListHistoryVersions() {
        List<ConversionCoefficient> history = service.listHistoryVersions();
        assertFalse(history.isEmpty());
        assertTrue(history.size() >= 2);
    }

    @Test
    void testDuplicateVersion() {
        ConversionCoefficientDTO dto = new ConversionCoefficientDTO();
        dto.setVersion("V1.0");
        dto.setEffectiveDate(LocalDate.of(2026, 1, 1));
        dto.setStandardCoalCoefficient(new BigDecimal("0.315"));
        dto.setCarbonDioxideCoefficient(new BigDecimal("0.995"));
        dto.setHouseholdElectricityConsumption(new BigDecimal("4700"));

        assertThrows(Exception.class, () -> service.createCoefficient(dto));
    }
}
