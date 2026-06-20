package com.carbonacct.service;

import com.carbonacct.common.enums.ReportStatus;
import com.carbonacct.domain.entity.CleanRevenueReport;
import com.carbonacct.domain.vo.AnnualStatisticsVO;
import com.carbonacct.domain.vo.AbnormalMonthVO;
import com.carbonacct.domain.vo.UnitContributionVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final ReportService reportService;
    private final UnitService unitService;

    private static final BigDecimal ABNORMAL_THRESHOLD = new BigDecimal("0.30");

    public StatisticsService(ReportService reportService, UnitService unitService) {
        this.reportService = reportService;
        this.unitService = unitService;
    }

    public AnnualStatisticsVO getAnnualStatistics(Integer year) {
        if (year == null) {
            year = YearMonth.now().getYear();
        }

        List<CleanRevenueReport> reports = reportService.listPublishedReportsForYear(year);

        AnnualStatisticsVO vo = new AnnualStatisticsVO();
        vo.setYear(year);

        BigDecimal totalElectricity = BigDecimal.ZERO;
        BigDecimal totalCoal = BigDecimal.ZERO;
        BigDecimal totalCo2 = BigDecimal.ZERO;
        BigDecimal totalHouseholds = BigDecimal.ZERO;

        for (CleanRevenueReport report : reports) {
            totalElectricity = totalElectricity.add(report.getEffectiveCleanElectricity());
            totalCoal = totalCoal.add(report.getStandardCoalSaving());
            totalCo2 = totalCo2.add(report.getCarbonDioxideReduction());
            totalHouseholds = totalHouseholds.add(report.getHouseholdCount());
        }

        vo.setTotalEffectiveElectricity(totalElectricity.setScale(4, RoundingMode.HALF_UP));
        vo.setTotalStandardCoalSaving(totalCoal.setScale(4, RoundingMode.HALF_UP));
        vo.setTotalCarbonDioxideReduction(totalCo2.setScale(4, RoundingMode.HALF_UP));
        vo.setTotalHouseholdCount(totalHouseholds.setScale(0, RoundingMode.DOWN));

        vo.setUnitContributions(calculateUnitContributions(reports, totalElectricity));
        vo.setAbnormalMonths(detectAbnormalMonths(reports));

        return vo;
    }

    private List<UnitContributionVO> calculateUnitContributions(List<CleanRevenueReport> reports,
                                                               BigDecimal totalElectricity) {
        Map<Long, UnitContributionVO> unitMap = new HashMap<>();

        for (CleanRevenueReport report : reports) {
            Long unitId = report.getUnitId();
            UnitContributionVO vo = unitMap.computeIfAbsent(unitId, k -> {
                UnitContributionVO v = new UnitContributionVO();
                v.setUnitId(unitId);
                v.setUnitName(unitService.getUnitName(unitId));
                v.setEffectiveElectricity(BigDecimal.ZERO);
                v.setStandardCoalSaving(BigDecimal.ZERO);
                v.setCarbonDioxideReduction(BigDecimal.ZERO);
                return v;
            });

            vo.setEffectiveElectricity(vo.getEffectiveElectricity().add(report.getEffectiveCleanElectricity()));
            vo.setStandardCoalSaving(vo.getStandardCoalSaving().add(report.getStandardCoalSaving()));
            vo.setCarbonDioxideReduction(vo.getCarbonDioxideReduction().add(report.getCarbonDioxideReduction()));
        }

        List<UnitContributionVO> result = new ArrayList<>(unitMap.values());

        for (UnitContributionVO vo : result) {
            BigDecimal rate = totalElectricity.compareTo(BigDecimal.ZERO) > 0
                    ? vo.getEffectiveElectricity().divide(totalElectricity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            vo.setContributionRate(rate);
        }

        result.sort((a, b) -> b.getEffectiveElectricity().compareTo(a.getEffectiveElectricity()));

        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }

    private List<AbnormalMonthVO> detectAbnormalMonths(List<CleanRevenueReport> reports) {
        Map<YearMonth, BigDecimal> monthlyTotals = reports.stream()
                .collect(Collectors.groupingBy(
                        CleanRevenueReport::getStatisticsMonth,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                CleanRevenueReport::getEffectiveCleanElectricity,
                                BigDecimal::add
                        )
                ));

        if (monthlyTotals.size() < 3) {
            return Collections.emptyList();
        }

        BigDecimal sum = monthlyTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(monthlyTotals.size()), 4, RoundingMode.HALF_UP);

        List<AbnormalMonthVO> abnormalMonths = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal> entry : monthlyTotals.entrySet()) {
            YearMonth month = entry.getKey();
            BigDecimal value = entry.getValue();

            BigDecimal diff = value.subtract(avg).abs();
            BigDecimal fluctuationRate = avg.compareTo(BigDecimal.ZERO) > 0
                    ? diff.divide(avg, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            if (fluctuationRate.compareTo(ABNORMAL_THRESHOLD) >= 0) {
                AbnormalMonthVO vo = new AbnormalMonthVO();
                vo.setStatisticsMonth(month);
                vo.setEffectiveElectricity(value);
                vo.setFluctuationRate(fluctuationRate);
                vo.setAvgElectricity(avg);
                vo.setAnomalyType(value.compareTo(avg) > 0 ? "异常偏高" : "异常偏低");
                abnormalMonths.add(vo);
            }
        }

        abnormalMonths.sort(Comparator.comparing(AbnormalMonthVO::getStatisticsMonth));
        return abnormalMonths;
    }

    public Map<String, Object> getAnnualForecast(int year) {
        List<CleanRevenueReport> reports = reportService.listPublishedReportsForYear(year);

        YearMonth currentMonth = YearMonth.now();
        int monthsWithData = 0;
        BigDecimal totalElectricity = BigDecimal.ZERO;
        BigDecimal totalCoal = BigDecimal.ZERO;
        BigDecimal totalCo2 = BigDecimal.ZERO;
        BigDecimal totalHouseholds = BigDecimal.ZERO;

        for (CleanRevenueReport report : reports) {
            if (report.getStatisticsMonth().isBefore(currentMonth) || report.getStatisticsMonth().equals(currentMonth)) {
                totalElectricity = totalElectricity.add(report.getEffectiveCleanElectricity());
                totalCoal = totalCoal.add(report.getStandardCoalSaving());
                totalCo2 = totalCo2.add(report.getCarbonDioxideReduction());
                totalHouseholds = totalHouseholds.add(report.getHouseholdCount());
                monthsWithData++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        if (monthsWithData > 0) {
            BigDecimal multiplier = BigDecimal.valueOf(12).divide(BigDecimal.valueOf(monthsWithData), 4, RoundingMode.HALF_UP);
            result.put("forecastElectricity", totalElectricity.multiply(multiplier).setScale(4, RoundingMode.HALF_UP));
            result.put("forecastCoal", totalCoal.multiply(multiplier).setScale(4, RoundingMode.HALF_UP));
            result.put("forecastCo2", totalCo2.multiply(multiplier).setScale(4, RoundingMode.HALF_UP));
            result.put("forecastHouseholds", totalHouseholds.multiply(multiplier).setScale(0, RoundingMode.DOWN));
        }
        result.put("monthsWithData", monthsWithData);
        result.put("actualElectricity", totalElectricity.setScale(4, RoundingMode.HALF_UP));
        result.put("actualCoal", totalCoal.setScale(4, RoundingMode.HALF_UP));
        result.put("actualCo2", totalCo2.setScale(4, RoundingMode.HALF_UP));
        result.put("actualHouseholds", totalHouseholds.setScale(0, RoundingMode.DOWN));

        return result;
    }
}
