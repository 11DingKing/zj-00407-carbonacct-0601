package com.carbonacct.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonacct.common.enums.ApprovalStatus;
import com.carbonacct.common.exception.BusinessException;
import com.carbonacct.domain.dto.ApprovalDTO;
import com.carbonacct.domain.dto.ConversionCoefficientDTO;
import com.carbonacct.domain.entity.ApprovalRecord;
import com.carbonacct.domain.entity.ConversionCoefficient;
import com.carbonacct.domain.mapper.ApprovalRecordMapper;
import com.carbonacct.domain.mapper.ConversionCoefficientMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ConversionCoefficientService extends ServiceImpl<ConversionCoefficientMapper, ConversionCoefficient> {

    private final ApprovalRecordMapper approvalRecordMapper;

    public ConversionCoefficientService(ApprovalRecordMapper approvalRecordMapper) {
        this.approvalRecordMapper = approvalRecordMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCoefficient(ConversionCoefficientDTO dto) {
        LambdaQueryWrapper<ConversionCoefficient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversionCoefficient::getVersion, dto.getVersion());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("版本号已存在: " + dto.getVersion());
        }

        if (dto.getEffectiveDate() == null) {
            dto.setEffectiveDate(LocalDate.now());
        }

        ConversionCoefficient coefficient = new ConversionCoefficient();
        BeanUtils.copyProperties(dto, coefficient);
        coefficient.setApprovalStatus(ApprovalStatus.PENDING);
        coefficient.setIsCurrent(false);
        save(coefficient);
        return coefficient.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveCoefficient(ApprovalDTO dto) {
        ConversionCoefficient coefficient = getById(dto.getBusinessId());
        if (coefficient == null) {
            throw new BusinessException("折算系数不存在");
        }
        if (coefficient.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("当前状态不允许审批");
        }

        coefficient.setApprovalStatus(dto.getApprovalStatus());
        coefficient.setApprover(dto.getApprover());
        coefficient.setApprovalOpinion(dto.getApprovalOpinion());
        coefficient.setApprovalDate(LocalDate.now());
        updateById(coefficient);

        if (dto.getApprovalStatus() == ApprovalStatus.APPROVED) {
            setAsCurrent(coefficient.getId());
        }

        ApprovalRecord record = new ApprovalRecord();
        BeanUtils.copyProperties(dto, record);
        record.setBusinessType("CONVERSION_COEFFICIENT");
        approvalRecordMapper.insert(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setAsCurrent(Long id) {
        ConversionCoefficient coefficient = getById(id);
        if (coefficient == null) {
            throw new BusinessException("折算系数不存在");
        }
        if (coefficient.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("只有已审批通过的系数才能生效");
        }

        LocalDate effectiveDate = coefficient.getEffectiveDate();

        LambdaQueryWrapper<ConversionCoefficient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversionCoefficient::getIsCurrent, true);
        List<ConversionCoefficient> currentCoefficients = list(wrapper);

        for (ConversionCoefficient c : currentCoefficients) {
            if (!c.getId().equals(id)) {
                c.setIsCurrent(false);
                updateById(c);
            }
        }

        wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ConversionCoefficient::getEffectiveDate, effectiveDate);
        List<ConversionCoefficient> laterCoefficients = list(wrapper);

        coefficient.setIsCurrent(true);
        if (coefficient.getExpiryDate() == null) {
            Optional<ConversionCoefficient> next = laterCoefficients.stream()
                    .filter(c -> !c.getId().equals(id))
                    .filter(c -> c.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .filter(c -> c.getEffectiveDate().isAfter(effectiveDate))
                    .min(Comparator.comparing(ConversionCoefficient::getEffectiveDate));
            next.ifPresent(n -> coefficient.setExpiryDate(n.getEffectiveDate().minusDays(1)));
        }
        updateById(coefficient);
    }

    public ConversionCoefficient getCoefficientForMonth(YearMonth month) {
        LocalDate monthDate = month.atDay(1);

        LambdaQueryWrapper<ConversionCoefficient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversionCoefficient::getApprovalStatus, ApprovalStatus.APPROVED)
                .le(ConversionCoefficient::getEffectiveDate, monthDate)
                .and(w -> w.isNull(ConversionCoefficient::getExpiryDate)
                        .or()
                        .ge(ConversionCoefficient::getExpiryDate, monthDate));
        wrapper.orderByDesc(ConversionCoefficient::getEffectiveDate);
        List<ConversionCoefficient> list = list(wrapper);

        if (list.isEmpty()) {
            throw new BusinessException("未找到适用于 " + month + " 的折算系数");
        }
        return list.get(0);
    }

    public ConversionCoefficient getCurrentCoefficient() {
        LambdaQueryWrapper<ConversionCoefficient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversionCoefficient::getIsCurrent, true)
                .eq(ConversionCoefficient::getApprovalStatus, ApprovalStatus.APPROVED);
        ConversionCoefficient coefficient = getOne(wrapper);
        if (coefficient == null) {
            throw new BusinessException("当前没有生效的折算系数");
        }
        return coefficient;
    }

    public ConversionCoefficient getCoefficientById(Long id) {
        ConversionCoefficient coefficient = getById(id);
        if (coefficient == null) {
            throw new BusinessException("折算系数不存在: " + id);
        }
        return coefficient;
    }

    public List<ConversionCoefficient> listAllCoefficients() {
        LambdaQueryWrapper<ConversionCoefficient> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ConversionCoefficient::getEffectiveDate);
        return list(wrapper);
    }

    public List<ConversionCoefficient> listHistoryVersions() {
        LambdaQueryWrapper<ConversionCoefficient> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ConversionCoefficient::getEffectiveDate);
        return list(wrapper);
    }

    public List<ApprovalRecord> listApprovalRecords(String businessType, Long businessId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        if (businessType != null) {
            wrapper.eq(ApprovalRecord::getBusinessType, businessType);
        }
        if (businessId != null) {
            wrapper.eq(ApprovalRecord::getBusinessId, businessId);
        }
        wrapper.orderByDesc(ApprovalRecord::getCreateTime);
        return approvalRecordMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveApprovalRecord(ApprovalDTO dto) {
        ApprovalRecord record = new ApprovalRecord();
        BeanUtils.copyProperties(dto, record);
        approvalRecordMapper.insert(record);
        return record.getId();
    }
}
