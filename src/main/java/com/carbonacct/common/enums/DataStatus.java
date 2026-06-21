package com.carbonacct.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum DataStatus {
    DRAFT("草稿", 0, "DRAFT"),
    LOCKED("已锁定", 1, "LOCKED");

    private final String desc;
    private final int code;

    @EnumValue
    private final String name;

    DataStatus(String desc, int code, String name) {
        this.desc = desc;
        this.code = code;
        this.name = name;
    }
}
