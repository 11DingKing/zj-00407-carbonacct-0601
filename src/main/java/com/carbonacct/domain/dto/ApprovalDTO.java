package com.carbonacct.domain.dto;

import com.carbonacct.common.enums.ApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalDTO {
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @NotNull(message = "业务ID不能为空")
    private Long businessId;

    private String businessNo;

    @NotNull(message = "审批状态不能为空")
    private ApprovalStatus approvalStatus;

    @NotBlank(message = "审批人不能为空")
    private String approver;

    private String approvalOpinion;
    private String operator;
    private String remark;
}
