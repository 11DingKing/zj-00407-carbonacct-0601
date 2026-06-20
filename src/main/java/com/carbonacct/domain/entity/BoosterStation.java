package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_booster_station")
public class BoosterStation extends BaseEntity {
    private String stationCode;
    private String stationName;
    private BigDecimal capacity;
    private String voltageLevel;
    private String location;
    private Integer status;
    private String remark;
}
