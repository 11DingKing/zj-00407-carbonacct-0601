package com.carbonacct.controller;

import com.carbonacct.common.base.Result;
import com.carbonacct.domain.dto.ApprovalDTO;
import com.carbonacct.domain.dto.ConversionCoefficientDTO;
import com.carbonacct.domain.entity.ApprovalRecord;
import com.carbonacct.domain.entity.ConversionCoefficient;
import com.carbonacct.service.ConversionCoefficientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coefficients")
public class CoefficientController {

    private final ConversionCoefficientService coefficientService;

    public CoefficientController(ConversionCoefficientService coefficientService) {
        this.coefficientService = coefficientService;
    }

    @PostMapping
    public Result<Long> createCoefficient(@Valid @RequestBody ConversionCoefficientDTO dto) {
        return Result.success(coefficientService.createCoefficient(dto));
    }

    @GetMapping
    public Result<List<ConversionCoefficient>> listCoefficients() {
        return Result.success(coefficientService.listAllCoefficients());
    }

    @GetMapping("/current")
    public Result<ConversionCoefficient> getCurrentCoefficient() {
        return Result.success(coefficientService.getCurrentCoefficient());
    }

    @GetMapping("/{id}")
    public Result<ConversionCoefficient> getCoefficient(@PathVariable Long id) {
        return Result.success(coefficientService.getById(id));
    }

    @PostMapping("/approve")
    public Result<Void> approveCoefficient(@Valid @RequestBody ApprovalDTO dto) {
        coefficientService.approveCoefficient(dto);
        return Result.success();
    }

    @PostMapping("/{id}/set-current")
    public Result<Void> setAsCurrent(@PathVariable Long id) {
        coefficientService.setAsCurrent(id);
        return Result.success();
    }

    @GetMapping("/history")
    public Result<List<ConversionCoefficient>> listHistoryVersions() {
        return Result.success(coefficientService.listHistoryVersions());
    }

    @GetMapping("/approvals")
    public Result<List<ApprovalRecord>> listApprovalRecords(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId) {
        return Result.success(coefficientService.listApprovalRecords(businessType, businessId));
    }
}
