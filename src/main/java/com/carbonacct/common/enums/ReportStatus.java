package com.carbonacct.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ReportStatus {
    DRAFT("草稿", 0, "DRAFT"),
    PENDING_REVIEW("待复核", 1, "PENDING_REVIEW"),
    PUBLISHED("已发布", 2, "PUBLISHED"),
    CORRECTED("已更正", 3, "CORRECTED");

    private final String desc;
    private final int code;

    @EnumValue
    private final String name;

    ReportStatus(String desc, int code, String name) {
        this.desc = desc;
        this.code = code;
        this.name = name;
    }

    public boolean canTransitionTo(ReportStatus target) {
        return switch (this) {
            case DRAFT -> target == PENDING_REVIEW;
            case PENDING_REVIEW -> target == PUBLISHED || target == DRAFT;
            case PUBLISHED -> target == CORRECTED;
            case CORRECTED -> false;
        };
    }
}
