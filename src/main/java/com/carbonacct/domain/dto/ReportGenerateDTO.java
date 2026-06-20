package com.carbonacct.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.YearMonth;

@Data
public class ReportGenerateDTO {
    @NotNull(message = "统计月份不能为空")
    private YearMonth statisticsMonth;

    private Long unitId;

    private Long boosterStationId;

    private String operator;

    private String remark;
}
