package com.carbonacct.controller;

import com.carbonacct.common.base.Result;
import com.carbonacct.domain.entity.Unit;
import com.carbonacct.service.UnitService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    public Result<List<Unit>> listUnits(@RequestParam(required = false) Long stationId,
                                        @RequestParam(required = false) Integer status) {
        return Result.success(unitService.listUnits(stationId, status));
    }

    @GetMapping("/{id}")
    public Result<Unit> getUnit(@PathVariable Long id) {
        return Result.success(unitService.getById(id));
    }

    @PostMapping
    public Result<Long> saveUnit(@RequestBody Unit unit) {
        return Result.success(unitService.saveUnit(unit));
    }
}
