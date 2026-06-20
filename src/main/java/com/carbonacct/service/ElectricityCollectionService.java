package com.carbonacct.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonacct.common.enums.AdjustmentType;
import com.carbonacct.common.exception.BusinessException;
import com.carbonacct.domain.dto.ElectricityAdjustmentDTO;
import com.carbonacct.domain.dto.ElectricityDataDTO;
import com.carbonacct.domain.entity.ElectricityAdjustment;
import com.carbonacct.domain.entity.ElectricityData;
import com.carbonacct.domain.mapper.ElectricityAdjustmentMapper;
import com.carbonacct.domain.mapper.ElectricityDataMapper;
import com.carbonacct.domain.vo.EffectiveElectricityVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElectricityCollectionService extends ServiceImpl<ElectricityDataMapper, ElectricityData> {

    private final ElectricityAdjustmentMapper adjustmentMapper;
    private final UnitService unitService;
    private final BoosterStationService boosterStationService;

    public ElectricityCollectionService(ElectricityAdjustmentMapper adjustmentMapper,
                                        UnitService unitService,
                                        BoosterStationService boosterStationService) {
        this.adjustmentMapper = adjustmentMapper;
        this.unitService = unitService;
        this.boosterStationService = boosterStationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveElectricityData(ElectricityDataDTO dto) {
        unitService.validateUnitExists(dto.getUnitId());
        boosterStationService.validateStationExists(dto.getBoosterStationId());

        LambdaQueryWrapper<ElectricityData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElectricityData::getUnitId, dto.getUnitId())
                .eq(ElectricityData::getStatisticsMonth, dto.getStatisticsMonth());
        ElectricityData existing = getOne(wrapper);
        if (existing != null) {
            throw new BusinessException("该机组当月电量数据已存在");
        }

        ElectricityData data = new ElectricityData();
        BeanUtils.copyProperties(dto, data);
        save(data);
        return data.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveElectricityAdjustment(ElectricityAdjustmentDTO dto) {
        unitService.validateUnitExists(dto.getUnitId());
        boosterStationService.validateStationExists(dto.getBoosterStationId());

        ElectricityAdjustment adjustment = new ElectricityAdjustment();
        BeanUtils.copyProperties(dto, adjustment);
        adjustmentMapper.insert(adjustment);
        return adjustment.getId();
    }

    public List<EffectiveElectricityVO> calculateEffectiveElectricity(YearMonth month, Long unitId, Long stationId) {
        LambdaQueryWrapper<ElectricityData> dataWrapper = new LambdaQueryWrapper<>();
        if (month != null) {
            dataWrapper.eq(ElectricityData::getStatisticsMonth, month);
        }
        if (unitId != null) {
            dataWrapper.eq(ElectricityData::getUnitId, unitId);
        }
        if (stationId != null) {
            dataWrapper.eq(ElectricityData::getBoosterStationId, stationId);
        }
        List<ElectricityData> dataList = list(dataWrapper);
        if (dataList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<YearMonth> months = dataList.stream()
                .map(ElectricityData::getStatisticsMonth)
                .collect(Collectors.toSet());
        Set<Long> unitIds = dataList.stream()
                .map(ElectricityData::getUnitId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<ElectricityAdjustment> adjWrapper = new LambdaQueryWrapper<>();
        adjWrapper.in(ElectricityAdjustment::getStatisticsMonth, months)
                .in(ElectricityAdjustment::getUnitId, unitIds);
        List<ElectricityAdjustment> adjustments = adjustmentMapper.selectList(adjWrapper);

        Map<String, List<ElectricityAdjustment>> adjMap = adjustments.stream()
                .collect(Collectors.groupingBy(adj -> adj.getUnitId() + "_" + adj.getStatisticsMonth()));

        List<EffectiveElectricityVO> result = new ArrayList<>();
        for (ElectricityData data : dataList) {
            EffectiveElectricityVO vo = new EffectiveElectricityVO();
            vo.setUnitId(data.getUnitId());
            vo.setUnitName(unitService.getUnitName(data.getUnitId()));
            vo.setBoosterStationId(data.getBoosterStationId());
            vo.setStationName(boosterStationService.getStationName(data.getBoosterStationId()));
            vo.setStatisticsMonth(data.getStatisticsMonth());
            vo.setTotalGridElectricity(data.getGridConnectedElectricity());
            vo.setTotalStationServiceElectricity(data.getStationServiceElectricity());

            String key = data.getUnitId() + "_" + data.getStatisticsMonth();
            List<ElectricityAdjustment> adjs = adjMap.getOrDefault(key, Collections.emptyList());
            BigDecimal totalAdj = adjs.stream()
                    .map(ElectricityAdjustment::getAdjustmentElectricity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setTotalAdjustmentElectricity(totalAdj);

            BigDecimal netElectricity = data.getGridConnectedElectricity()
                    .subtract(data.getStationServiceElectricity());
            BigDecimal effective = netElectricity.subtract(totalAdj);
            if (effective.compareTo(BigDecimal.ZERO) < 0) {
                effective = BigDecimal.ZERO;
            }
            vo.setEffectiveCleanElectricity(effective.setScale(4, RoundingMode.HALF_UP));

            vo.setTraceabilityRemark(buildTraceabilityRemark(data, adjs));
            result.add(vo);
        }
        return result;
    }

    private String buildTraceabilityRemark(ElectricityData data, List<ElectricityAdjustment> adjs) {
        StringBuilder sb = new StringBuilder();
        sb.append(StrUtil.format("上网电量: {} MWh, 厂用电: {} MWh",
                data.getGridConnectedElectricity(), data.getStationServiceElectricity()));

        Map<AdjustmentType, BigDecimal> adjByType = adjs.stream()
                .collect(Collectors.groupingBy(
                        ElectricityAdjustment::getAdjustmentType,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                ElectricityAdjustment::getAdjustmentElectricity,
                                BigDecimal::add
                        )
                ));

        for (Map.Entry<AdjustmentType, BigDecimal> entry : adjByType.entrySet()) {
            sb.append("; ").append(entry.getKey().getDesc())
                    .append(": ").append(entry.getValue()).append(" MWh");
        }

        if (StrUtil.isNotBlank(data.getDataSource())) {
            sb.append("; 数据来源: ").append(data.getDataSource());
        }
        return sb.toString();
    }

    public List<ElectricityData> listElectricityData(YearMonth month, Long unitId) {
        LambdaQueryWrapper<ElectricityData> wrapper = new LambdaQueryWrapper<>();
        if (month != null) {
            wrapper.eq(ElectricityData::getStatisticsMonth, month);
        }
        if (unitId != null) {
            wrapper.eq(ElectricityData::getUnitId, unitId);
        }
        wrapper.orderByDesc(ElectricityData::getStatisticsMonth);
        return list(wrapper);
    }

    public List<ElectricityAdjustment> listAdjustments(YearMonth month, Long unitId, AdjustmentType type) {
        LambdaQueryWrapper<ElectricityAdjustment> wrapper = new LambdaQueryWrapper<>();
        if (month != null) {
            wrapper.eq(ElectricityAdjustment::getStatisticsMonth, month);
        }
        if (unitId != null) {
            wrapper.eq(ElectricityAdjustment::getUnitId, unitId);
        }
        if (type != null) {
            wrapper.eq(ElectricityAdjustment::getAdjustmentType, type);
        }
        wrapper.orderByDesc(ElectricityAdjustment::getStatisticsMonth);
        return adjustmentMapper.selectList(wrapper);
    }
}
