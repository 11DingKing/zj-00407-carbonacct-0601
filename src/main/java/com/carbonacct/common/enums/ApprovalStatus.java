package com.carbonacct.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ApprovalStatus {
    PENDING("待审批", 0, "PENDING"),
    APPROVED("已通过", 1, "APPROVED"),
    REJECTED("已驳回", 2, "REJECTED");

    private final String desc;
    private final int code;

    @EnumValue
    private final String name;

    ApprovalStatus(String desc, int code, String name) {
        this.desc = desc;
        this.code = code;
        this.name = name;
    }
}
