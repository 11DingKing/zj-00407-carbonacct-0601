package com.carbonacct.controller;

import com.carbonacct.common.base.Result;
import com.carbonacct.domain.entity.BoosterStation;
import com.carbonacct.service.BoosterStationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class BoosterStationController {

    private final BoosterStationService boosterStationService;

    public BoosterStationController(BoosterStationService boosterStationService) {
        this.boosterStationService = boosterStationService;
    }

    @GetMapping
    public Result<List<BoosterStation>> listStations(@RequestParam(required = false) Integer status) {
        return Result.success(boosterStationService.listStations(status));
    }

    @GetMapping("/{id}")
    public Result<BoosterStation> getStation(@PathVariable Long id) {
        return Result.success(boosterStationService.getById(id));
    }

    @PostMapping
    public Result<Long> saveStation(@RequestBody BoosterStation station) {
        return Result.success(boosterStationService.saveStation(station));
    }
}
