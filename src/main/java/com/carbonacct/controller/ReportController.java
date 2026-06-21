package com.carbonacct.controller;

import com.carbonacct.common.base.Result;
import com.carbonacct.common.enums.ReportStatus;
import com.carbonacct.domain.dto.ApprovalDTO;
import com.carbonacct.domain.dto.ReportCorrectionDTO;
import com.carbonacct.domain.dto.ReportGenerateDTO;
import com.carbonacct.domain.entity.CleanRevenueReport;
import com.carbonacct.domain.entity.ReportCorrection;
import com.carbonacct.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/generate")
    public Result<List<CleanRevenueReport>> generateReport(@Valid @RequestBody ReportGenerateDTO dto) {
        return Result.success(reportService.generateReport(dto));
    }

    @GetMapping
    public Result<List<CleanRevenueReport>> listReports(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) ReportStatus status) {
        return Result.success(reportService.listReports(month, unitId, status));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getReportDetail(@PathVariable Long id) {
        return Result.success(reportService.getReportWithCorrections(id));
    }

    @PostMapping("/{id}/transition")
    public Result<Void> transitionStatus(
            @PathVariable Long id,
            @RequestParam ReportStatus targetStatus,
            @RequestParam String operator) {
        reportService.transitionStatus(id, targetStatus, operator);
        return Result.success();
    }

    @PostMapping("/correct")
    @Deprecated
    public Result<CleanRevenueReport> correctReport(@Valid @RequestBody ReportCorrectionDTO dto) {
        return Result.success(reportService.correctReport(dto));
    }

    @PostMapping("/correction/submit")
    public Result<ReportCorrection> submitCorrection(@Valid @RequestBody ReportCorrectionDTO dto) {
        return Result.success(reportService.submitCorrection(dto));
    }

    @PostMapping("/correction/approve")
    public Result<ReportCorrection> approveCorrection(@Valid @RequestBody ApprovalDTO dto) {
        return Result.success(reportService.approveCorrection(dto));
    }

    @GetMapping("/{id}/corrections")
    public Result<List<ReportCorrection>> listReportCorrections(@PathVariable Long id) {
        return Result.success(reportService.listReportCorrections(id));
    }

    @GetMapping("/corrections/{correctionId}")
    public Result<ReportCorrection> getCorrectionDetail(@PathVariable Long correctionId) {
        return Result.success(reportService.getCorrectionDetail(correctionId));
    }
}
