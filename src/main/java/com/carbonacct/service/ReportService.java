package com.carbonacct.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonacct.common.enums.ApprovalStatus;
import com.carbonacct.common.enums.ReportStatus;
import com.carbonacct.common.exception.BusinessException;
import com.carbonacct.domain.dto.ApprovalDTO;
import com.carbonacct.domain.dto.ReportCorrectionDTO;
import com.carbonacct.domain.dto.ReportGenerateDTO;
import com.carbonacct.domain.entity.CleanRevenueReport;
import com.carbonacct.domain.entity.ConversionCoefficient;
import com.carbonacct.domain.entity.ReportCorrection;
import com.carbonacct.domain.mapper.CleanRevenueReportMapper;
import com.carbonacct.domain.mapper.ReportCorrectionMapper;
import com.carbonacct.domain.vo.CleanRevenueVO;
import com.carbonacct.domain.vo.EffectiveElectricityVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ReportService extends ServiceImpl<CleanRevenueReportMapper, CleanRevenueReport> {

    private final CleanRevenueCalculationService calculationService;
    private final ElectricityCollectionService electricityCollectionService;
    private final ConversionCoefficientService coefficientService;
    private final UnitService unitService;
    private final BoosterStationService boosterStationService;
    private final ReportCorrectionMapper reportCorrectionMapper;

    private final Map<String, AtomicInteger> reportSequenceMap = new ConcurrentHashMap<>();

    public ReportService(CleanRevenueCalculationService calculationService,
                         ElectricityCollectionService electricityCollectionService,
                         ConversionCoefficientService coefficientService,
                         UnitService unitService,
                         BoosterStationService boosterStationService,
                         ReportCorrectionMapper reportCorrectionMapper) {
        this.calculationService = calculationService;
        this.electricityCollectionService = electricityCollectionService;
        this.coefficientService = coefficientService;
        this.unitService = unitService;
        this.boosterStationService = boosterStationService;
        this.reportCorrectionMapper = reportCorrectionMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<CleanRevenueReport> generateReport(ReportGenerateDTO dto) {
        List<EffectiveElectricityVO> effectiveList = electricityCollectionService
                .calculateEffectiveElectricity(dto.getStatisticsMonth(), dto.getUnitId(), dto.getBoosterStationId());

        if (effectiveList.isEmpty()) {
            throw new BusinessException("未找到有效电量数据");
        }

        List<CleanRevenueReport> reports = new ArrayList<>();
        for (EffectiveElectricityVO effective : effectiveList) {
            LambdaQueryWrapper<CleanRevenueReport> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CleanRevenueReport::getStatisticsMonth, effective.getStatisticsMonth())
                    .eq(CleanRevenueReport::getUnitId, effective.getUnitId());
            CleanRevenueReport existing = getOne(wrapper);
            if (existing != null && existing.getReportStatus() != ReportStatus.DRAFT) {
                throw new BusinessException(StrUtil.format("机组{} {}月报表已存在且状态为{}，不允许重复生成",
                        effective.getUnitName(), effective.getStatisticsMonth(), existing.getReportStatus().getDesc()));
            }

            ConversionCoefficient coefficient = coefficientService
                    .getCoefficientForMonth(effective.getStatisticsMonth());

            CleanRevenueVO revenue = calculationService.calculateByEffectiveAndCoefficient(
                    effective.getEffectiveCleanElectricity(),
                    effective.getStatisticsMonth(),
                    effective.getUnitId(),
                    effective.getBoosterStationId(),
                    coefficient);

            CleanRevenueReport report;
            if (existing != null) {
                report = existing;
            } else {
                report = new CleanRevenueReport();
                report.setReportNo(generateReportNo(effective.getStatisticsMonth()));
                report.setStatisticsMonth(effective.getStatisticsMonth());
                report.setUnitId(effective.getUnitId());
                report.setBoosterStationId(effective.getBoosterStationId());
                report.setVersion(1);
            }

            report.setCoefficientId(coefficient.getId());
            report.setTotalGridElectricity(effective.getTotalGridElectricity());
            report.setTotalStationServiceElectricity(effective.getTotalStationServiceElectricity());
            report.setTotalAdjustmentElectricity(effective.getTotalAdjustmentElectricity());
            report.setEffectiveCleanElectricity(revenue.getEffectiveCleanElectricity());
            report.setStandardCoalSaving(revenue.getStandardCoalSaving());
            report.setCarbonDioxideReduction(revenue.getCarbonDioxideReduction());
            report.setHouseholdCount(revenue.getHouseholdCount());
            report.setReportStatus(ReportStatus.DRAFT);
            report.setPreparedBy(dto.getOperator());
            report.setRemark(dto.getRemark());

            saveOrUpdate(report);
            reports.add(report);
        }
        return reports;
    }

    private String generateReportNo(YearMonth month) {
        String prefix = "CRR-" + month.format(DateTimeFormatter.ofPattern("yyyyMM"));
        AtomicInteger sequence = reportSequenceMap.computeIfAbsent(prefix, k -> new AtomicInteger(0));
        int seq = sequence.incrementAndGet();
        LambdaQueryWrapper<CleanRevenueReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CleanRevenueReport::getReportNo, prefix + "-");
        Long count = count(wrapper);
        seq = Math.max(seq, count.intValue() + 1);
        return prefix + "-" + String.format("%04d", seq);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transitionStatus(Long reportId, ReportStatus targetStatus, String operator) {
        CleanRevenueReport report = getById(reportId);
        if (report == null) {
            throw new BusinessException("报表不存在");
        }

        ReportStatus currentStatus = report.getReportStatus();
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessException(StrUtil.format("状态流转不允许: {} -> {}",
                    currentStatus.getDesc(), targetStatus.getDesc()));
        }

        report.setReportStatus(targetStatus);

        switch (targetStatus) {
            case PENDING_REVIEW:
                break;
            case PUBLISHED:
                report.setReviewedBy(operator);
                report.setPublishedBy(operator);
                electricityCollectionService.lockElectricityData(
                        report.getStatisticsMonth(), report.getUnitId());
                break;
            default:
                break;
        }

        updateById(report);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportCorrection submitCorrection(ReportCorrectionDTO dto) {
        CleanRevenueReport report = getById(dto.getReportId());
        if (report == null) {
            throw new BusinessException("报表不存在");
        }

        if (report.getReportStatus() != ReportStatus.PUBLISHED
                && report.getReportStatus() != ReportStatus.CORRECTED) {
            throw new BusinessException("只有已发布或已更正的报表才能申请更正");
        }

        LambdaQueryWrapper<ReportCorrection> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(ReportCorrection::getReportId, report.getId())
                .eq(ReportCorrection::getApprovalStatus, ApprovalStatus.PENDING);
        Long pendingCount = reportCorrectionMapper.selectCount(pendingWrapper);
        if (pendingCount > 0) {
            throw new BusinessException("该报表存在待审批的更正申请，请先处理");
        }

        ConversionCoefficient beforeCoefficient = coefficientService.getCoefficientById(report.getCoefficientId());

        ConversionCoefficient afterCoefficient = beforeCoefficient;
        if (dto.getAfterCoefficientId() != null && !dto.getAfterCoefficientId().equals(report.getCoefficientId())) {
            afterCoefficient = coefficientService.getCoefficientById(dto.getAfterCoefficientId());
            if (afterCoefficient.getApprovalStatus() != ApprovalStatus.APPROVED) {
                throw new BusinessException("更正使用的折算系数必须是已审批通过的");
            }
        }

        if (dto.getAfterCoefficientId() != null && !dto.getAfterCoefficientId().equals(report.getCoefficientId())) {
            CleanRevenueVO recalculated = calculationService.calculateByEffectiveAndCoefficient(
                    dto.getEffectiveCleanElectricity(),
                    report.getStatisticsMonth(),
                    report.getUnitId(),
                    report.getBoosterStationId(),
                    afterCoefficient);
            dto.setStandardCoalSaving(recalculated.getStandardCoalSaving());
            dto.setCarbonDioxideReduction(recalculated.getCarbonDioxideReduction());
            dto.setHouseholdCount(recalculated.getHouseholdCount());
        }

        ReportCorrection correction = new ReportCorrection();
        correction.setReportId(report.getId());
        correction.setReportNo(report.getReportNo());
        correction.setOriginalVersion(report.getVersion());
        correction.setCorrectedVersion(report.getVersion() + 1);
        correction.setBeforeCoefficientId(report.getCoefficientId());
        correction.setAfterCoefficientId(afterCoefficient.getId());
        correction.setBeforeCoefficientVersion(beforeCoefficient.getVersion());
        correction.setAfterCoefficientVersion(afterCoefficient.getVersion());
        correction.setBeforeEffectiveElectricity(report.getEffectiveCleanElectricity());
        correction.setAfterEffectiveElectricity(dto.getEffectiveCleanElectricity());
        correction.setBeforeStandardCoalSaving(report.getStandardCoalSaving());
        correction.setAfterStandardCoalSaving(dto.getStandardCoalSaving());
        correction.setBeforeCarbonDioxideReduction(report.getCarbonDioxideReduction());
        correction.setAfterCarbonDioxideReduction(dto.getCarbonDioxideReduction());
        correction.setBeforeHouseholdCount(report.getHouseholdCount());
        correction.setAfterHouseholdCount(dto.getHouseholdCount());
        correction.setCorrectionReason(dto.getCorrectionReason());
        correction.setCorrectedBy(dto.getCorrectedBy());
        correction.setApprovalStatus(ApprovalStatus.PENDING);
        correction.setRemark(dto.getRemark());
        reportCorrectionMapper.insert(correction);

        return correction;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportCorrection approveCorrection(ApprovalDTO dto) {
        ReportCorrection correction = reportCorrectionMapper.selectById(dto.getBusinessId());
        if (correction == null) {
            throw new BusinessException("更正记录不存在");
        }

        if (correction.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("只有待审批状态的更正申请才能审批");
        }

        CleanRevenueReport report = getById(correction.getReportId());
        if (report == null) {
            throw new BusinessException("报表不存在");
        }

        correction.setApprovalStatus(dto.getApprovalStatus());
        correction.setApprover(dto.getApprover());
        correction.setApprovalTime(LocalDateTime.now());
        correction.setApprovalOpinion(dto.getApprovalOpinion());
        reportCorrectionMapper.updateById(correction);

        if (dto.getApprovalStatus() == ApprovalStatus.APPROVED) {
            report.setCoefficientId(correction.getAfterCoefficientId());
            report.setEffectiveCleanElectricity(correction.getAfterEffectiveElectricity());
            report.setStandardCoalSaving(correction.getAfterStandardCoalSaving());
            report.setCarbonDioxideReduction(correction.getAfterCarbonDioxideReduction());
            report.setHouseholdCount(correction.getAfterHouseholdCount());
            report.setReportStatus(ReportStatus.CORRECTED);
            report.setCorrectedBy(correction.getCorrectedBy());
            report.setVersion(correction.getCorrectedVersion());
            updateById(report);
        }

        return correction;
    }

    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public CleanRevenueReport correctReport(ReportCorrectionDTO dto) {
        CleanRevenueReport report = getById(dto.getReportId());
        if (report == null) {
            throw new BusinessException("报表不存在");
        }

        if (report.getReportStatus() != ReportStatus.PUBLISHED
                && report.getReportStatus() != ReportStatus.CORRECTED) {
            throw new BusinessException("只有已发布或已更正的报表才能更正");
        }

        ConversionCoefficient beforeCoefficient = coefficientService.getById(report.getCoefficientId());
        if (beforeCoefficient == null) {
            throw new BusinessException("报表使用的折算系数不存在");
        }

        ConversionCoefficient afterCoefficient = beforeCoefficient;
        if (dto.getAfterCoefficientId() != null && !dto.getAfterCoefficientId().equals(report.getCoefficientId())) {
            afterCoefficient = coefficientService.getById(dto.getAfterCoefficientId());
            if (afterCoefficient == null) {
                throw new BusinessException("更正使用的折算系数不存在");
            }
        }

        ReportCorrection correction = new ReportCorrection();
        correction.setReportId(report.getId());
        correction.setReportNo(report.getReportNo());
        correction.setOriginalVersion(report.getVersion());
        correction.setCorrectedVersion(report.getVersion() + 1);
        correction.setBeforeCoefficientId(report.getCoefficientId());
        correction.setAfterCoefficientId(afterCoefficient.getId());
        correction.setBeforeCoefficientVersion(beforeCoefficient.getVersion());
        correction.setAfterCoefficientVersion(afterCoefficient.getVersion());
        correction.setBeforeEffectiveElectricity(report.getEffectiveCleanElectricity());
        correction.setAfterEffectiveElectricity(dto.getEffectiveCleanElectricity());
        correction.setBeforeStandardCoalSaving(report.getStandardCoalSaving());
        correction.setAfterStandardCoalSaving(dto.getStandardCoalSaving());
        correction.setBeforeCarbonDioxideReduction(report.getCarbonDioxideReduction());
        correction.setAfterCarbonDioxideReduction(dto.getCarbonDioxideReduction());
        correction.setBeforeHouseholdCount(report.getHouseholdCount());
        correction.setAfterHouseholdCount(dto.getHouseholdCount());
        correction.setCorrectionReason(dto.getCorrectionReason());
        correction.setCorrectedBy(dto.getCorrectedBy());
        correction.setApprovalStatus(ApprovalStatus.APPROVED);
        correction.setApprover(dto.getApprover() != null ? dto.getApprover() : dto.getCorrectedBy());
        correction.setApprovalTime(LocalDateTime.now());
        correction.setApprovalOpinion(dto.getApprovalOpinion());
        correction.setRemark(dto.getRemark());
        reportCorrectionMapper.insert(correction);

        report.setCoefficientId(afterCoefficient.getId());
        report.setEffectiveCleanElectricity(dto.getEffectiveCleanElectricity());
        report.setStandardCoalSaving(dto.getStandardCoalSaving());
        report.setCarbonDioxideReduction(dto.getCarbonDioxideReduction());
        report.setHouseholdCount(dto.getHouseholdCount());
        report.setReportStatus(ReportStatus.CORRECTED);
        report.setCorrectedBy(dto.getCorrectedBy());
        report.setVersion(report.getVersion() + 1);
        updateById(report);

        return report;
    }

    public List<CleanRevenueReport> listReports(YearMonth month, Long unitId, ReportStatus status) {
        LambdaQueryWrapper<CleanRevenueReport> wrapper = new LambdaQueryWrapper<>();
        if (month != null) {
            wrapper.eq(CleanRevenueReport::getStatisticsMonth, month);
        }
        if (unitId != null) {
            wrapper.eq(CleanRevenueReport::getUnitId, unitId);
        }
        if (status != null) {
            wrapper.eq(CleanRevenueReport::getReportStatus, status);
        }
        wrapper.orderByDesc(CleanRevenueReport::getStatisticsMonth)
                .orderByAsc(CleanRevenueReport::getUnitId);
        return list(wrapper);
    }

    public CleanRevenueReport getReportDetail(Long reportId) {
        CleanRevenueReport report = getById(reportId);
        if (report == null) {
            throw new BusinessException("报表不存在");
        }
        return report;
    }

    public List<ReportCorrection> listReportCorrections(Long reportId) {
        LambdaQueryWrapper<ReportCorrection> wrapper = new LambdaQueryWrapper<>();
        if (reportId != null) {
            wrapper.eq(ReportCorrection::getReportId, reportId);
        }
        wrapper.orderByDesc(ReportCorrection::getCreateTime);
        return reportCorrectionMapper.selectList(wrapper);
    }

    public ReportCorrection getCorrectionDetail(Long correctionId) {
        ReportCorrection correction = reportCorrectionMapper.selectById(correctionId);
        if (correction == null) {
            throw new BusinessException("更正记录不存在");
        }
        return correction;
    }

    public Map<String, Object> getReportWithCorrections(Long reportId) {
        CleanRevenueReport report = getReportDetail(reportId);
        List<ReportCorrection> corrections = listReportCorrections(reportId);

        ConversionCoefficient coefficient = coefficientService.getCoefficientById(report.getCoefficientId());
        String coefficientVersion = coefficient != null ? coefficient.getVersion() : "未知";

        CleanRevenueVO revenueSnapshot = calculationService.calculateByEffectiveAndCoefficientId(
                report.getEffectiveCleanElectricity(),
                report.getStatisticsMonth(),
                report.getUnitId(),
                report.getBoosterStationId(),
                report.getCoefficientId());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("report", report);
        result.put("unitName", unitService.getUnitName(report.getUnitId()));
        result.put("stationName", boosterStationService.getStationName(report.getBoosterStationId()));
        result.put("coefficientVersion", coefficientVersion);
        result.put("coefficient", coefficient);
        result.put("revenueSnapshot", revenueSnapshot);
        result.put("corrections", corrections);
        return result;
    }

    public List<CleanRevenueReport> listPublishedReportsForYear(int year) {
        YearMonth start = YearMonth.of(year, 1);
        YearMonth end = YearMonth.of(year, 12);

        LambdaQueryWrapper<CleanRevenueReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CleanRevenueReport::getStatisticsMonth, start)
                .le(CleanRevenueReport::getStatisticsMonth, end)
                .and(w -> w.eq(CleanRevenueReport::getReportStatus, ReportStatus.PUBLISHED)
                        .or()
                        .eq(CleanRevenueReport::getReportStatus, ReportStatus.CORRECTED));
        wrapper.orderByAsc(CleanRevenueReport::getStatisticsMonth)
                .orderByAsc(CleanRevenueReport::getUnitId);
        return list(wrapper);
    }
}
