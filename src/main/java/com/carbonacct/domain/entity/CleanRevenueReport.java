package com.carbonacct.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.carbonacct.common.base.BaseEntity;
import com.carbonacct.common.converter.YearMonthTypeHandler;
import com.carbonacct.common.enums.ReportStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_clean_revenue_report", autoResultMap = true)
public class CleanRevenueReport extends BaseEntity {
    private String reportNo;

    @TableField(typeHandler = YearMonthTypeHandler.class)
    private YearMonth statisticsMonth;
    private Long unitId;
    private Long boosterStationId;
    private Long coefficientId;
    private BigDecimal totalGridElectricity;
    private BigDecimal totalStationServiceElectricity;
    private BigDecimal totalAdjustmentElectricity;
    private BigDecimal effectiveCleanElectricity;
    private BigDecimal standardCoalSaving;
    private BigDecimal carbonDioxideReduction;
    private BigDecimal householdCount;
    private ReportStatus reportStatus;
    private String preparedBy;
    private String reviewedBy;
    private String publishedBy;
    private String correctedBy;
    private String remark;
    private Integer version;
}
