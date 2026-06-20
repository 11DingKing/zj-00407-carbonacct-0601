package com.carbonacct.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonacct.common.exception.BusinessException;
import com.carbonacct.domain.entity.Unit;
import com.carbonacct.domain.mapper.UnitMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnitService extends ServiceImpl<UnitMapper, Unit> {

    public String getUnitName(Long unitId) {
        Unit unit = getById(unitId);
        return unit != null ? unit.getUnitName() : "未知机组";
    }

    public void validateUnitExists(Long unitId) {
        if (unitId == null) {
            return;
        }
        Unit unit = getById(unitId);
        if (unit == null) {
            throw new BusinessException("机组不存在: " + unitId);
        }
    }

    public List<Unit> listUnits(Long stationId, Integer status) {
        LambdaQueryWrapper<Unit> wrapper = new LambdaQueryWrapper<>();
        if (stationId != null) {
            wrapper.eq(Unit::getBoosterStationId, stationId);
        }
        if (status != null) {
            wrapper.eq(Unit::getStatus, status);
        }
        wrapper.orderByAsc(Unit::getUnitCode);
        return list(wrapper);
    }

    public Long saveUnit(Unit unit) {
        saveOrUpdate(unit);
        return unit.getId();
    }
}
