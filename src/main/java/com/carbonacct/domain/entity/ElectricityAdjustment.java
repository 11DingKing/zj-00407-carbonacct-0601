package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import com.carbonacct.common.converter.YearMonthTypeHandler;
import com.carbonacct.common.enums.AdjustmentType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_electricity_adjustment", autoResultMap = true)
public class ElectricityAdjustment extends BaseEntity {
    private Long unitId;
    private Long boosterStationId;

    @TableField(typeHandler = YearMonthTypeHandler.class)
    private YearMonth statisticsMonth;
    private AdjustmentType adjustmentType;
    private BigDecimal adjustmentElectricity;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String approvalDoc;
    private String operator;
    private String remark;
}
