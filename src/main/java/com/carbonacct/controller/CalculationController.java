package com.carbonacct.controller;

import com.carbonacct.common.base.Result;
import com.carbonacct.domain.vo.CleanRevenueVO;
import com.carbonacct.service.CleanRevenueCalculationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/calculation")
public class CalculationController {

    private final CleanRevenueCalculationService calculationService;

    public CalculationController(CleanRevenueCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @GetMapping("/revenue")
    public Result<List<CleanRevenueVO>> calculateCleanRevenue(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long stationId) {
        return Result.success(calculationService.calculateCleanRevenue(month, unitId, stationId));
    }
}
