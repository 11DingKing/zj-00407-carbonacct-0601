package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import com.carbonacct.common.converter.YearMonthTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_electricity_data", autoResultMap = true)
public class ElectricityData extends BaseEntity {
    private Long unitId;
    private Long boosterStationId;

    @TableField(typeHandler = YearMonthTypeHandler.class)
    private YearMonth statisticsMonth;
    private BigDecimal gridConnectedElectricity;
    private BigDecimal stationServiceElectricity;
    private String dataSource;
    private String operator;
    private String remark;
}
