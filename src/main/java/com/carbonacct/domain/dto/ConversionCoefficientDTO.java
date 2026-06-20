package com.carbonacct.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ConversionCoefficientDTO {
    @NotNull(message = "版本号不能为空")
    private String version;

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    @NotNull(message = "标准煤折算系数不能为空")
    @DecimalMin(value = "0", message = "标准煤折算系数不能为负数")
    private BigDecimal standardCoalCoefficient;

    @NotNull(message = "二氧化碳折算系数不能为空")
    @DecimalMin(value = "0", message = "二氧化碳折算系数不能为负数")
    private BigDecimal carbonDioxideCoefficient;

    @NotNull(message = "居民家庭年均用电量不能为空")
    @DecimalMin(value = "0", message = "居民家庭年均用电量不能为负数")
    private BigDecimal householdElectricityConsumption;

    private String approver;
    private LocalDate approvalDate;
    private String approvalOpinion;
    private String operator;
    private String remark;
}
