package com.carbonacct.service;

import com.carbonacct.common.enums.ApprovalStatus;
import com.carbonacct.common.enums.ReportStatus;
import com.carbonacct.domain.entity.CleanRevenueReport;
import com.carbonacct.domain.entity.ReportCorrection;
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
        return getAnnualStatistics(year, false);
    }

    public AnnualStatisticsVO getAnnualStatisticsWithCorrectionDiff(Integer year) {
        return getAnnualStatistics(year, true);
    }

    private AnnualStatisticsVO getAnnualStatistics(Integer year, boolean includeCorrectionDiff) {
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

        vo.setUnitContributions(calculateUnitContributions(reports, totalElectricity, false));
        vo.setAbnormalMonths(detectAbnormalMonths(reports, false));

        if (includeCorrectionDiff) {
            fillCorrectionDiff(vo, reports, year);
        }

        return vo;
    }

    private void fillCorrectionDiff(AnnualStatisticsVO vo, List<CleanRevenueReport> reports, int year) {
        Map<Long, List<ReportCorrection>> correctionsByReport = getApprovedCorrectionsForYear(year);

        boolean hasCorrections = correctionsByReport.values().stream().anyMatch(list -> !list.isEmpty());
        vo.setHasCorrections(hasCorrections);

        int totalCorrectionCount = correctionsByReport.values().stream().mapToInt(List::size).sum();
        vo.setCorrectionCount(totalCorrectionCount);

        if (!hasCorrections) {
            return;
        }

        List<CleanRevenueReport> originalReports = computeOriginalReports(reports, correctionsByReport);

        BigDecimal originalTotalElectricity = BigDecimal.ZERO;
        BigDecimal originalTotalCoal = BigDecimal.ZERO;
        BigDecimal originalTotalCo2 = BigDecimal.ZERO;
        BigDecimal originalTotalHouseholds = BigDecimal.ZERO;

        for (CleanRevenueReport report : originalReports) {
            originalTotalElectricity = originalTotalElectricity.add(report.getEffectiveCleanElectricity());
            originalTotalCoal = originalTotalCoal.add(report.getStandardCoalSaving());
            originalTotalCo2 = originalTotalCo2.add(report.getCarbonDioxideReduction());
            originalTotalHouseholds = originalTotalHouseholds.add(report.getHouseholdCount());
        }

        vo.setOriginalTotalEffectiveElectricity(originalTotalElectricity.setScale(4, RoundingMode.HALF_UP));
        vo.setOriginalTotalStandardCoalSaving(originalTotalCoal.setScale(4, RoundingMode.HALF_UP));
        vo.setOriginalTotalCarbonDioxideReduction(originalTotalCo2.setScale(4, RoundingMode.HALF_UP));
        vo.setOriginalTotalHouseholdCount(originalTotalHouseholds.setScale(0, RoundingMode.DOWN));

        vo.setDiffEffectiveElectricity(
                vo.getTotalEffectiveElectricity().subtract(vo.getOriginalTotalEffectiveElectricity())
                        .setScale(4, RoundingMode.HALF_UP));
        vo.setDiffStandardCoalSaving(
                vo.getTotalStandardCoalSaving().subtract(vo.getOriginalTotalStandardCoalSaving())
                        .setScale(4, RoundingMode.HALF_UP));
        vo.setDiffCarbonDioxideReduction(
                vo.getTotalCarbonDioxideReduction().subtract(vo.getOriginalTotalCarbonDioxideReduction())
                        .setScale(4, RoundingMode.HALF_UP));
        vo.setDiffHouseholdCount(
                vo.getTotalHouseholdCount().subtract(vo.getOriginalTotalHouseholdCount())
                        .setScale(0, RoundingMode.DOWN));

        List<UnitContributionVO> originalUnitContributions = calculateUnitContributions(originalReports, originalTotalElectricity, true);
        List<UnitContributionVO> currentUnitContributions = vo.getUnitContributions();
        mergeUnitContributionDiff(currentUnitContributions, originalUnitContributions);

        List<AbnormalMonthVO> originalAbnormalMonths = detectAbnormalMonths(originalReports, true);
        List<AbnormalMonthVO> currentAbnormalMonths = vo.getAbnormalMonths();
        mergeAbnormalMonthDiff(currentAbnormalMonths, originalAbnormalMonths);
    }

    private Map<Long, List<ReportCorrection>> getApprovedCorrectionsForYear(int year) {
        Map<Long, List<ReportCorrection>> result = new HashMap<>();
        List<CleanRevenueReport> reports = reportService.listPublishedReportsForYear(year);
        for (CleanRevenueReport report : reports) {
            List<ReportCorrection> corrections = reportService.listReportCorrections(report.getId());
            List<ReportCorrection> approved = corrections.stream()
                    .filter(c -> c.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .sorted(Comparator.comparing(ReportCorrection::getCreateTime))
                    .collect(Collectors.toList());
            if (!approved.isEmpty()) {
                result.put(report.getId(), approved);
            }
        }
        return result;
    }

    private List<CleanRevenueReport> computeOriginalReports(List<CleanRevenueReport> currentReports,
                                                            Map<Long, List<ReportCorrection>> correctionsByReport) {
        List<CleanRevenueReport> originalReports = new ArrayList<>();
        for (CleanRevenueReport current : currentReports) {
            CleanRevenueReport original = new CleanRevenueReport();
            original.setId(current.getId());
            original.setReportNo(current.getReportNo());
            original.setStatisticsMonth(current.getStatisticsMonth());
            original.setUnitId(current.getUnitId());
            original.setBoosterStationId(current.getBoosterStationId());
            original.setCoefficientId(current.getCoefficientId());
            original.setReportStatus(current.getReportStatus());
            original.setVersion(current.getVersion());

            List<ReportCorrection> corrections = correctionsByReport.get(current.getId());
            if (corrections == null || corrections.isEmpty()) {
                original.setEffectiveCleanElectricity(current.getEffectiveCleanElectricity());
                original.setStandardCoalSaving(current.getStandardCoalSaving());
                original.setCarbonDioxideReduction(current.getCarbonDioxideReduction());
                original.setHouseholdCount(current.getHouseholdCount());
            } else {
                ReportCorrection firstCorrection = corrections.get(0);
                original.setEffectiveCleanElectricity(firstCorrection.getBeforeEffectiveElectricity());
                original.setStandardCoalSaving(firstCorrection.getBeforeStandardCoalSaving());
                original.setCarbonDioxideReduction(firstCorrection.getBeforeCarbonDioxideReduction());
                original.setHouseholdCount(firstCorrection.getBeforeHouseholdCount());
                original.setCoefficientId(firstCorrection.getBeforeCoefficientId());
            }

            originalReports.add(original);
        }
        return originalReports;
    }

    private List<UnitContributionVO> calculateUnitContributions(List<CleanRevenueReport> reports,
                                                               BigDecimal totalElectricity,
                                                               boolean isOriginal) {
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

    private void mergeUnitContributionDiff(List<UnitContributionVO> currentList,
                                           List<UnitContributionVO> originalList) {
        Map<Long, UnitContributionVO> originalMap = new HashMap<>();
        for (UnitContributionVO vo : originalList) {
            originalMap.put(vo.getUnitId(), vo);
        }

        for (UnitContributionVO current : currentList) {
            UnitContributionVO original = originalMap.get(current.getUnitId());
            if (original != null) {
                current.setHasCorrections(true);
                current.setOriginalRank(original.getRank());
                current.setOriginalEffectiveElectricity(original.getEffectiveElectricity());
                current.setOriginalContributionRate(original.getContributionRate());
                current.setDiffEffectiveElectricity(
                        current.getEffectiveElectricity().subtract(original.getEffectiveElectricity())
                                .setScale(4, RoundingMode.HALF_UP));
                current.setRankChange(original.getRank() - current.getRank());
            } else {
                current.setHasCorrections(false);
            }
        }
    }

    private List<AbnormalMonthVO> detectAbnormalMonths(List<CleanRevenueReport> reports, boolean isOriginal) {
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

    private void mergeAbnormalMonthDiff(List<AbnormalMonthVO> currentList,
                                        List<AbnormalMonthVO> originalList) {
        Map<YearMonth, AbnormalMonthVO> originalMap = new HashMap<>();
        Set<YearMonth> originalMonths = new HashSet<>();
        for (AbnormalMonthVO vo : originalList) {
            originalMap.put(vo.getStatisticsMonth(), vo);
            originalMonths.add(vo.getStatisticsMonth());
        }

        Set<YearMonth> currentMonths = currentList.stream()
                .map(AbnormalMonthVO::getStatisticsMonth)
                .collect(Collectors.toSet());

        for (AbnormalMonthVO current : currentList) {
            AbnormalMonthVO original = originalMap.get(current.getStatisticsMonth());
            if (original != null) {
                current.setHasCorrections(true);
                current.setOriginalEffectiveElectricity(original.getEffectiveElectricity());
                current.setOriginalFluctuationRate(original.getFluctuationRate());
                current.setOriginalAnomalyType(original.getAnomalyType());
                current.setDiffEffectiveElectricity(
                        current.getEffectiveElectricity().subtract(original.getEffectiveElectricity())
                                .setScale(4, RoundingMode.HALF_UP));
                current.setAnomalyChanged(!current.getAnomalyType().equals(original.getAnomalyType()));
            } else {
                current.setHasCorrections(true);
                current.setAnomalyChanged(true);
                current.setDiffEffectiveElectricity(current.getEffectiveElectricity());
            }
        }
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
