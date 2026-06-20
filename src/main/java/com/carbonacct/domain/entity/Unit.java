package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_unit")
public class Unit extends BaseEntity {
    private String unitCode;
    private String unitName;
    private Long boosterStationId;
    private BigDecimal capacity;
    private String unitType;
    private String location;
    private Integer status;
    private String remark;
}
