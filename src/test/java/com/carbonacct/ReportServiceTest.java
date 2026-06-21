package com.carbonacct;

import com.carbonacct.common.enums.AdjustmentType;
import com.carbonacct.common.enums.ReportStatus;
import com.carbonacct.domain.dto.ElectricityAdjustmentDTO;
import com.carbonacct.domain.dto.ElectricityDataDTO;
import com.carbonacct.domain.dto.ReportCorrectionDTO;
import com.carbonacct.domain.dto.ReportGenerateDTO;
import com.carbonacct.domain.entity.CleanRevenueReport;
import com.carbonacct.domain.entity.ReportCorrection;
import com.carbonacct.service.ElectricityCollectionService;
import com.carbonacct.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ElectricityCollectionService electricityService;

    @Test
    void testGenerateReport() {
        YearMonth month = YearMonth.of(2025, 5);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(1L);
        dataDTO.setBoosterStationId(1L);
        dataDTO.setStatisticsMonth(month);
        dataDTO.setGridConnectedElectricity(new BigDecimal("15000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("450"));
        dataDTO.setOperator("test");
        electricityService.saveElectricityData(dataDTO);

        ReportGenerateDTO generateDTO = new ReportGenerateDTO();
        generateDTO.setStatisticsMonth(month);
        generateDTO.setUnitId(1L);
        generateDTO.setOperator("测试人员");
        generateDTO.setRemark("测试报表生成");

        List<CleanRevenueReport> reports = reportService.generateReport(generateDTO);
        assertFalse(reports.isEmpty());

        CleanRevenueReport report = reports.get(0);
        assertNotNull(report.getReportNo());
        assertTrue(report.getReportNo().startsWith("CRR-"));
        assertEquals(ReportStatus.DRAFT, report.getReportStatus());
        assertEquals(1, report.getVersion());
        assertNotNull(report.getEffectiveCleanElectricity());
        assertNotNull(report.getStandardCoalSaving());
        assertNotNull(report.getCarbonDioxideReduction());
        assertNotNull(report.getHouseholdCount());
    }

    @Test
    void testStatusTransition() {
        YearMonth month = YearMonth.of(2025, 6);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(2L);
        dataDTO.setBoosterStationId(1L);
        dataDTO.setStatisticsMonth(month);
        dataDTO.setGridConnectedElectricity(new BigDecimal("12000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("360"));
        dataDTO.setOperator("test");
        electricityService.saveElectricityData(dataDTO);

        ReportGenerateDTO generateDTO = new ReportGenerateDTO();
        generateDTO.setStatisticsMonth(month);
        generateDTO.setUnitId(2L);
        generateDTO.setOperator("测试人员");
        List<CleanRevenueReport> reports = reportService.generateReport(generateDTO);
        Long reportId = reports.get(0).getId();

        reportService.transitionStatus(reportId, ReportStatus.PENDING_REVIEW, "复核人A");
        CleanRevenueReport pending = reportService.getById(reportId);
        assertEquals(ReportStatus.PENDING_REVIEW, pending.getReportStatus());

        reportService.transitionStatus(reportId, ReportStatus.PUBLISHED, "发布人B");
        CleanRevenueReport published = reportService.getById(reportId);
        assertEquals(ReportStatus.PUBLISHED, published.getReportStatus());
        assertEquals("发布人B", published.getReviewedBy());
        assertEquals("发布人B", published.getPublishedBy());
    }

    @Test
    void testInvalidStatusTransition() {
        YearMonth month = YearMonth.of(2025, 7);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(3L);
        dataDTO.setBoosterStationId(2L);
        dataDTO.setStatisticsMonth(month);
        dataDTO.setGridConnectedElectricity(new BigDecimal("8000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("240"));
        dataDTO.setOperator("test");
        electricityService.saveElectricityData(dataDTO);

        ReportGenerateDTO generateDTO = new ReportGenerateDTO();
        generateDTO.setStatisticsMonth(month);
        generateDTO.setUnitId(3L);
        generateDTO.setOperator("测试人员");
        List<CleanRevenueReport> reports = reportService.generateReport(generateDTO);
        Long reportId = reports.get(0).getId();

        assertThrows(Exception.class, () ->
                reportService.transitionStatus(reportId, ReportStatus.PUBLISHED, "发布人"));
    }

    @Test
    void testCorrectReport() {
        YearMonth month = YearMonth.of(2025, 8);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(1L);
        dataDTO.setBoosterStationId(1L);
        dataDTO.setStatisticsMonth(month);
        dataDTO.setGridConnectedElectricity(new BigDecimal("20000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("600"));
        dataDTO.setOperator("test");
        electricityService.saveElectricityData(dataDTO);

        ElectricityAdjustmentDTO adjDTO = new ElectricityAdjustmentDTO();
        adjDTO.setUnitId(1L);
        adjDTO.setBoosterStationId(1L);
        adjDTO.setStatisticsMonth(month);
        adjDTO.setAdjustmentType(AdjustmentType.MAINTENANCE);
        adjDTO.setAdjustmentElectricity(new BigDecimal("1000"));
        adjDTO.setOperator("test");
        electricityService.saveElectricityAdjustment(adjDTO);

        ReportGenerateDTO generateDTO = new ReportGenerateDTO();
        generateDTO.setStatisticsMonth(month);
        generateDTO.setUnitId(1L);
        generateDTO.setOperator("测试人员");
        List<CleanRevenueReport> reports = reportService.generateReport(generateDTO);
        Long reportId = reports.get(0).getId();

        reportService.transitionStatus(reportId, ReportStatus.PENDING_REVIEW, "复核人");
        reportService.transitionStatus(reportId, ReportStatus.PUBLISHED, "发布人");

        CleanRevenueReport before = reportService.getById(reportId);
        assertEquals(1, before.getVersion());

        ReportCorrectionDTO correctionDTO = new ReportCorrectionDTO();
        correctionDTO.setReportId(reportId);
        correctionDTO.setEffectiveCleanElectricity(new BigDecimal("18500"));
        correctionDTO.setStandardCoalSaving(new BigDecimal("5.7165"));
        correctionDTO.setCarbonDioxideReduction(new BigDecimal("18.2410"));
        correctionDTO.setHouseholdCount(new BigDecimal("49"));
        correctionDTO.setCorrectionReason("发现原始数据录入有误，已核实更正");
        correctionDTO.setCorrectedBy("更正人C");
        correctionDTO.setApprovalOpinion("情况属实，同意更正");

        CleanRevenueReport corrected = reportService.correctReport(correctionDTO);
        assertEquals(ReportStatus.CORRECTED, corrected.getReportStatus());
        assertEquals(2, corrected.getVersion());
        assertEquals(0, corrected.getEffectiveCleanElectricity().compareTo(new BigDecimal("18500")));

        List<ReportCorrection> corrections = reportService.listReportCorrections(reportId);
        assertFalse(corrections.isEmpty());
        ReportCorrection correction = corrections.get(0);
        assertEquals(1, correction.getOriginalVersion());
        assertEquals(2, correction.getCorrectedVersion());
        assertNotNull(correction.getBeforeEffectiveElectricity());
        assertNotNull(correction.getAfterEffectiveElectricity());
        assertEquals("发现原始数据录入有误，已核实更正", correction.getCorrectionReason());
    }

    @Test
    void testGetReportWithCorrections() {
        YearMonth month = YearMonth.of(2025, 9);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(2L);
        dataDTO.setBoosterStationId(1L);
        dataDTO.setStatisticsMonth(month);
        dataDTO.setGridConnectedElectricity(new BigDecimal("25000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("750"));
        dataDTO.setOperator("test");
        electricityService.saveElectricityData(dataDTO);

        ReportGenerateDTO generateDTO = new ReportGenerateDTO();
        generateDTO.setStatisticsMonth(month);
        generateDTO.setUnitId(2L);
        generateDTO.setOperator("测试人员");
        List<CleanRevenueReport> reports = reportService.generateReport(generateDTO);
        Long reportId = reports.get(0).getId();

        Map<String, Object> detail = reportService.getReportWithCorrections(reportId);
        assertNotNull(detail.get("report"));
        assertNotNull(detail.get("unitName"));
        assertNotNull(detail.get("stationName"));
        assertNotNull(detail.get("coefficientVersion"));
    }

    @Test
    void testHistoricalReportsNotAffectedByNewCoefficients() {
        YearMonth month2024 = YearMonth.of(2024, 12);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(1L);
        dataDTO.setBoosterStationId(1L);
        dataDTO.setStatisticsMonth(month2024);
        dataDTO.setGridConnectedElectricity(new BigDecimal("30000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("900"));
        dataDTO.setOperator("test");
        electricityService.saveElectricityData(dataDTO);

        ReportGenerateDTO generateDTO = new ReportGenerateDTO();
        generateDTO.setStatisticsMonth(month2024);
        generateDTO.setUnitId(1L);
        generateDTO.setOperator("测试人员");
        List<CleanRevenueReport> reports = reportService.generateReport(generateDTO);
        CleanRevenueReport report = reports.get(0);

        BigDecimal coal2024 = report.getStandardCoalSaving();
        Long coefficientId2024 = report.getCoefficientId();

        reportService.transitionStatus(report.getId(), ReportStatus.PENDING_REVIEW, "复核人");
        reportService.transitionStatus(report.getId(), ReportStatus.PUBLISHED, "发布人");

        CleanRevenueReport afterPublish = reportService.getById(report.getId());
        assertEquals(0, afterPublish.getStandardCoalSaving().compareTo(coal2024));
        assertEquals(coefficientId2024, afterPublish.getCoefficientId());
    }

    @Test
    void testCorrectionWithCoefficientVersion() {
        YearMonth month = YearMonth.of(2025, 10);

        ElectricityDataDTO dataDTO = new ElectricityDataDTO();
        dataDTO.setUnitId(1L);
        dataDTO.setBoosterStationId(1L);
        dataDTO.setStatisticsMonth(month);
        dataDTO.setGridConnectedElectricity(new BigDecimal("22000"));
        dataDTO.setStationServiceElectricity(new BigDecimal("660"));
        dataDTO.setOperator("test");
        electricityService.saveElectricityData(dataDTO);

        ReportGenerateDTO generateDTO = new ReportGenerateDTO();
        generateDTO.setStatisticsMonth(month);
        generateDTO.setUnitId(1L);
        generateDTO.setOperator("测试人员");
        List<CleanRevenueReport> reports = reportService.generateReport(generateDTO);
        Long reportId = reports.get(0).getId();

        reportService.transitionStatus(reportId, ReportStatus.PENDING_REVIEW, "复核人");
        reportService.transitionStatus(reportId, ReportStatus.PUBLISHED, "发布人");

        CleanRevenueReport before = reportService.getById(reportId);
        assertEquals(1, before.getVersion());

        ReportCorrectionDTO correctionDTO = new ReportCorrectionDTO();
        correctionDTO.setReportId(reportId);
        correctionDTO.setEffectiveCleanElectricity(new BigDecimal("21000"));
        correctionDTO.setStandardCoalSaving(new BigDecimal("6.5"));
        correctionDTO.setCarbonDioxideReduction(new BigDecimal("20.5"));
        correctionDTO.setHouseholdCount(new BigDecimal("55"));
        correctionDTO.setCorrectionReason("测试更正记录折算系数版本");
        correctionDTO.setCorrectedBy("更正人C");
        correctionDTO.setApprover("审批人D");
        correctionDTO.setApprovalOpinion("情况属实，同意更正");

        CleanRevenueReport corrected = reportService.correctReport(correctionDTO);
        assertEquals(ReportStatus.CORRECTED, corrected.getReportStatus());
        assertEquals(2, corrected.getVersion());

        List<ReportCorrection> corrections = reportService.listReportCorrections(reportId);
        assertFalse(corrections.isEmpty());
        ReportCorrection correction = corrections.get(0);

        assertNotNull(correction.getBeforeCoefficientId());
        assertNotNull(correction.getAfterCoefficientId());
        assertNotNull(correction.getBeforeCoefficientVersion());
        assertNotNull(correction.getAfterCoefficientVersion());
        assertEquals(correction.getBeforeCoefficientVersion(), correction.getAfterCoefficientVersion());

        assertNotNull(correction.getApprovalStatus());
        assertNotNull(correction.getApprover());
        assertEquals("审批人D", correction.getApprover());
        assertNotNull(correction.getApprovalTime());
        assertEquals("情况属实，同意更正", correction.getApprovalOpinion());

        ReportCorrection detail = reportService.getCorrectionDetail(correction.getId());
        assertNotNull(detail);
        assertEquals(correction.getId(), detail.getId());
    }
}
