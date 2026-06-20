package com.carbonacct.controller;

import com.carbonacct.common.base.Result;
import com.carbonacct.common.enums.AdjustmentType;
import com.carbonacct.domain.dto.ElectricityAdjustmentDTO;
import com.carbonacct.domain.dto.ElectricityDataDTO;
import com.carbonacct.domain.entity.ElectricityAdjustment;
import com.carbonacct.domain.entity.ElectricityData;
import com.carbonacct.domain.vo.EffectiveElectricityVO;
import com.carbonacct.service.ElectricityCollectionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/electricity")
public class ElectricityController {

    private final ElectricityCollectionService electricityCollectionService;

    public ElectricityController(ElectricityCollectionService electricityCollectionService) {
        this.electricityCollectionService = electricityCollectionService;
    }

    @PostMapping("/data")
    public Result<Long> saveElectricityData(@Valid @RequestBody ElectricityDataDTO dto) {
        return Result.success(electricityCollectionService.saveElectricityData(dto));
    }

    @GetMapping("/data")
    public Result<List<ElectricityData>> listElectricityData(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Long unitId) {
        return Result.success(electricityCollectionService.listElectricityData(month, unitId));
    }

    @PostMapping("/adjustment")
    public Result<Long> saveElectricityAdjustment(@Valid @RequestBody ElectricityAdjustmentDTO dto) {
        return Result.success(electricityCollectionService.saveElectricityAdjustment(dto));
    }

    @GetMapping("/adjustment")
    public Result<List<ElectricityAdjustment>> listAdjustments(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) AdjustmentType type) {
        return Result.success(electricityCollectionService.listAdjustments(month, unitId, type));
    }

    @GetMapping("/effective")
    public Result<List<EffectiveElectricityVO>> calculateEffectiveElectricity(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long stationId) {
        return Result.success(electricityCollectionService.calculateEffectiveElectricity(month, unitId, stationId));
    }
}
