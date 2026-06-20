package com.carbonacct.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum AdjustmentType {
    TRIAL_OPERATION("试运行", "试运行期间发电量需扣除", "TRIAL_OPERATION"),
    CURTAILMENT("限发", "电网限发电量需扣除", "CURTAILMENT"),
    MAINTENANCE("检修停机", "检修停机期间发电量需扣除", "MAINTENANCE"),
    EQUIPMENT_FAULT("设备故障", "设备故障期间发电量需扣除", "EQUIPMENT_FAULT"),
    OTHER("其他", "其他原因需扣除", "OTHER");

    private final String desc;
    private final String remark;

    @EnumValue
    private final String name;

    AdjustmentType(String desc, String remark, String name) {
        this.desc = desc;
        this.remark = remark;
        this.name = name;
    }
}
