package com.carbonacct.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonacct.common.exception.BusinessException;
import com.carbonacct.domain.entity.BoosterStation;
import com.carbonacct.domain.mapper.BoosterStationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoosterStationService extends ServiceImpl<BoosterStationMapper, BoosterStation> {

    public String getStationName(Long stationId) {
        BoosterStation station = getById(stationId);
        return station != null ? station.getStationName() : "未知升压站";
    }

    public void validateStationExists(Long stationId) {
        if (stationId == null) {
            return;
        }
        BoosterStation station = getById(stationId);
        if (station == null) {
            throw new BusinessException("升压站不存在: " + stationId);
        }
    }

    public List<BoosterStation> listStations(Integer status) {
        LambdaQueryWrapper<BoosterStation> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(BoosterStation::getStatus, status);
        }
        wrapper.orderByAsc(BoosterStation::getStationCode);
        return list(wrapper);
    }

    public Long saveStation(BoosterStation station) {
        saveOrUpdate(station);
        return station.getId();
    }
}
