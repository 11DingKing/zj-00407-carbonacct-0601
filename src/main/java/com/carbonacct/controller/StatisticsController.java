package com.carbonacct.controller;

import com.carbonacct.common.base.Result;
import com.carbonacct.domain.vo.AnnualStatisticsVO;
import com.carbonacct.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/annual")
    public Result<AnnualStatisticsVO> getAnnualStatistics(@RequestParam(required = false) Integer year) {
        return Result.success(statisticsService.getAnnualStatistics(year));
    }

    @GetMapping("/forecast")
    public Result<Map<String, Object>> getAnnualForecast(@RequestParam(required = false) Integer year) {
        int y = year != null ? year : java.time.YearMonth.now().getYear();
        return Result.success(statisticsService.getAnnualForecast(y));
    }
}
