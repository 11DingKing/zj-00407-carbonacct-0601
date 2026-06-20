package com.carbonacct;

import com.carbonacct.common.enums.ReportStatus;
import com.carbonacct.domain.dto.ElectricityDataDTO;
import com.carbonacct.domain.dto.ReportGenerateDTO;
import com.carbonacct.domain.entity.CleanRevenueReport;
import com.carbonacct.domain.vo.AnnualStatisticsVO;
import com.carbonacct.domain.vo.UnitContributionVO;
import com.carbonacct.service.ElectricityCollectionService;
import com.carbonacct.service.ReportService;
import com.carbonacct.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ElectricityCollectionService electricityService;

    @Test
    void testGetAnnualStatistics() {
        int year = 2026;
        prepareYearData(year);

        AnnualStatisticsVO stats = statisticsService.getAnnualStatistics(year);
        assertNotNull(stats);
        assertEquals(year, stats.getYear());
        assertNotNull(stats.getTotalEffectiveElectricity());
        assertNotNull(stats.getTotalStandardCoalSaving());
        assertNotNull(stats.getTotalCarbonDioxideReduction());
        assertNotNull(stats.getTotalHouseholdCount());

        assertTrue(stats.getTotalEffectiveElectricity().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(stats.getTotalStandardCoalSaving().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(stats.getTotalCarbonDioxideReduction().compareTo(BigDecimal.ZERO) > 0);

        List<UnitContributionVO> contributions = stats.getUnitContributions();
        assertNotNull(contributions);
        assertFalse(contributions.isEmpty());

        for (int i = 0; i < contributions.size(); i++) {
            assertEquals(i + 1, contributions.get(i).getRank());
        }

        if (contributions.size() >= 2) {
            assertTrue(contributions.get(0).getEffectiveElectricity()
                    .compareTo(contributions.get(1).getEffectiveElectricity()) >= 0);
        }
    }

    @Test
    void testUnitContributionRate() {
        int year = 2026;
        prepareYearData(year);

        AnnualStatisticsVO stats = statisticsService.getAnnualStatistics(year);
        List<UnitContributionVO> contributions = stats.getUnitContributions();

        BigDecimal totalRate = contributions.stream()
                .map(UnitContributionVO::getContributionRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertTrue(totalRate.compareTo(new BigDecimal("0.99")) >= 0);
        assertTrue(totalRate.compareTo(new BigDecimal("1.01")) <= 0);
    }

    @Test
    void testAbnormalMonthDetection() {
        int year = 2026;
        prepareYearData(year);

        AnnualStatisticsVO stats = statisticsService.getAnnualStatistics(year);
        assertNotNull(stats.getAbnormalMonths());

        stats.getAbnormalMonths().forEach(vo -> {
            assertNotNull(vo.getStatisticsMonth());
            assertNotNull(vo.getEffectiveElectricity());
            assertNotNull(vo.getFluctuationRate());
            assertNotNull(vo.getAvgElectricity());
            assertNotNull(vo.getAnomalyType());
            assertTrue(vo.getFluctuationRate().compareTo(new BigDecimal("0.30")) >= 0);
        });
    }

    @Test
    void testGetAnnualForecast() {
        int year = 2026;
        prepareYearData(year);

        Map<String, Object> forecast = statisticsService.getAnnualForecast(year);
        assertNotNull(forecast);
        assertNotNull(forecast.get("actualElectricity"));
        assertNotNull(forecast.get("monthsWithData"));

        Integer monthsWithData = (Integer) forecast.get("monthsWithData");
        assertTrue(monthsWithData >= 3);

        if (monthsWithData > 0 && monthsWithData < 12) {
            assertNotNull(forecast.get("forecastElectricity"));
            assertNotNull(forecast.get("forecastCoal"));
            assertNotNull(forecast.get("forecastCo2"));
            assertNotNull(forecast.get("forecastHouseholds"));
        }
    }

    private void prepareYearData(int year) {
        for (int month = 1; month <= 6; month++) {
            YearMonth ym = YearMonth.of(year, month);
            BigDecimal gridElectricity = month == 3 ? new BigDecimal("5000") : new BigDecimal("10000");
            try {
                ElectricityDataDTO dataDTO = new ElectricityDataDTO();
                dataDTO.setUnitId(1L);
                dataDTO.setBoosterStationId(1L);
                dataDTO.setStatisticsMonth(ym);
                dataDTO.setGridConnectedElectricity(gridElectricity);
                dataDTO.setStationServiceElectricity(new BigDecimal("300"));
                dataDTO.setOperator("test");
                electricityService.saveElectricityData(dataDTO);
            } catch (Exception ignored) {
            }

            try {
                ElectricityDataDTO dataDTO2 = new ElectricityDataDTO();
                dataDTO2.setUnitId(2L);
                dataDTO2.setBoosterStationId(1L);
                dataDTO2.setStatisticsMonth(ym);
                dataDTO2.setGridConnectedElectricity(new BigDecimal("8000"));
                dataDTO2.setStationServiceElectricity(new BigDecimal("240"));
                dataDTO2.setOperator("test");
                electricityService.saveElectricityData(dataDTO2);
            } catch (Exception ignored) {
            }

            try {
                ReportGenerateDTO generateDTO = new ReportGenerateDTO();
                generateDTO.setStatisticsMonth(ym);
                generateDTO.setOperator("test");
                List<CleanRevenueReport> reports = reportService.generateReport(generateDTO);
                for (CleanRevenueReport report : reports) {
                    reportService.transitionStatus(report.getId(), ReportStatus.PENDING_REVIEW, "reviewer");
                    reportService.transitionStatus(report.getId(), ReportStatus.PUBLISHED, "publisher");
                }
            } catch (Exception ignored) {
            }
        }
    }
}
