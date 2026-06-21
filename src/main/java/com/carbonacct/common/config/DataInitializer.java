package com.carbonacct.common.config;

import com.carbonacct.common.enums.AdjustmentType;
import com.carbonacct.common.enums.ApprovalStatus;
import com.carbonacct.common.enums.ReportStatus;
import com.carbonacct.domain.dto.ElectricityAdjustmentDTO;
import com.carbonacct.domain.dto.ElectricityDataDTO;
import com.carbonacct.domain.dto.ReportGenerateDTO;
import com.carbonacct.domain.entity.BoosterStation;
import com.carbonacct.domain.entity.CleanRevenueReport;
import com.carbonacct.domain.entity.ConversionCoefficient;
import com.carbonacct.domain.entity.Unit;
import com.carbonacct.service.BoosterStationService;
import com.carbonacct.service.ConversionCoefficientService;
import com.carbonacct.service.ElectricityCollectionService;
import com.carbonacct.service.ReportService;
import com.carbonacct.service.UnitService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UnitService unitService,
                                      BoosterStationService boosterStationService,
                                      ConversionCoefficientService coefficientService,
                                      ElectricityCollectionService electricityCollectionService,
                                      ReportService reportService) {
        return args -> {
            if (boosterStationService.count() == 0) {
                BoosterStation station1 = new BoosterStation();
                station1.setStationCode("BS001");
                station1.setStationName("第一升压站");
                station1.setCapacity(new BigDecimal("110"));
                station1.setVoltageLevel("110kV");
                station1.setLocation("浙江省宁波市");
                station1.setStatus(1);
                boosterStationService.save(station1);

                BoosterStation station2 = new BoosterStation();
                station2.setStationCode("BS002");
                station2.setStationName("第二升压站");
                station2.setCapacity(new BigDecimal("220"));
                station2.setVoltageLevel("220kV");
                station2.setLocation("浙江省杭州市");
                station2.setStatus(1);
                boosterStationService.save(station2);
            }

            if (unitService.count() == 0) {
                Unit unit1 = new Unit();
                unit1.setUnitCode("U001");
                unit1.setUnitName("1号机组");
                unit1.setBoosterStationId(1L);
                unit1.setCapacity(new BigDecimal("50"));
                unit1.setUnitType("光伏");
                unit1.setLocation("浙江省宁波市");
                unit1.setStatus(1);
                unitService.save(unit1);

                Unit unit2 = new Unit();
                unit2.setUnitCode("U002");
                unit2.setUnitName("2号机组");
                unit2.setBoosterStationId(1L);
                unit2.setCapacity(new BigDecimal("50"));
                unit2.setUnitType("光伏");
                unit2.setLocation("浙江省宁波市");
                unit2.setStatus(1);
                unitService.save(unit2);

                Unit unit3 = new Unit();
                unit3.setUnitCode("U003");
                unit3.setUnitName("3号机组");
                unit3.setBoosterStationId(2L);
                unit3.setCapacity(new BigDecimal("100"));
                unit3.setUnitType("风电");
                unit3.setLocation("浙江省杭州市");
                unit3.setStatus(1);
                unitService.save(unit3);
            }

            if (coefficientService.count() == 0) {
                ConversionCoefficient coefficient = new ConversionCoefficient();
                coefficient.setVersion("V1.0");
                coefficient.setEffectiveDate(LocalDate.of(2024, 1, 1));
                coefficient.setStandardCoalCoefficient(new BigDecimal("0.309"));
                coefficient.setCarbonDioxideCoefficient(new BigDecimal("0.986"));
                coefficient.setHouseholdElectricityConsumption(new BigDecimal("4500"));
                coefficient.setApprovalStatus(ApprovalStatus.APPROVED);
                coefficient.setApprover("系统管理员");
                coefficient.setApprovalDate(LocalDate.of(2023, 12, 25));
                coefficient.setApprovalOpinion("初始版本，同意生效");
                coefficient.setOperator("system");
                coefficient.setIsCurrent(true);
                coefficientService.save(coefficient);

                ConversionCoefficient coefficient2 = new ConversionCoefficient();
                coefficient2.setVersion("V2.0");
                coefficient2.setEffectiveDate(LocalDate.of(2025, 1, 1));
                coefficient2.setStandardCoalCoefficient(new BigDecimal("0.312"));
                coefficient2.setCarbonDioxideCoefficient(new BigDecimal("0.992"));
                coefficient2.setHouseholdElectricityConsumption(new BigDecimal("4600"));
                coefficient2.setApprovalStatus(ApprovalStatus.PENDING);
                coefficient2.setOperator("system");
                coefficient2.setIsCurrent(false);
                coefficientService.save(coefficient2);
            }

            if (electricityCollectionService.count() == 0) {
                initElectricityData(electricityCollectionService);
                initElectricityAdjustments(electricityCollectionService);
            }

            if (reportService.count() == 0) {
                initReports(reportService);
            }
        };
    }

    private void initElectricityData(ElectricityCollectionService service) {
        int[][] unit1Data = {
            {4200, 126}, {3800, 114}, {5100, 153}, {5800, 174},
            {6200, 186}, {6800, 204}, {7200, 216}, {7000, 210},
            {6500, 195}, {5500, 165}, {4500, 135}, {4000, 120}
        };
        int[][] unit2Data = {
            {4000, 120}, {3600, 108}, {4900, 147}, {5600, 168},
            {6000, 180}, {6600, 198}, {7000, 210}, {6800, 204},
            {6300, 189}, {5300, 159}, {4300, 129}, {3800, 114}
        };
        int[][] unit3Data = {
            {8500, 255}, {7800, 234}, {9200, 276}, {10500, 315},
            {11200, 336}, {12000, 360}, {11500, 345}, {11800, 354},
            {10800, 324}, {9500, 285}, {8800, 264}, {8200, 246}
        };

        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(2026, month);
            saveElectricityData(service, 1L, 1L, ym, unit1Data[month - 1][0], unit1Data[month - 1][1]);
            saveElectricityData(service, 2L, 1L, ym, unit2Data[month - 1][0], unit2Data[month - 1][1]);
            saveElectricityData(service, 3L, 2L, ym, unit3Data[month - 1][0], unit3Data[month - 1][1]);
        }
    }

    private void saveElectricityData(ElectricityCollectionService service, Long unitId, Long stationId,
                                     YearMonth month, int grid, int station) {
        ElectricityDataDTO dto = new ElectricityDataDTO();
        dto.setUnitId(unitId);
        dto.setBoosterStationId(stationId);
        dto.setStatisticsMonth(month);
        dto.setGridConnectedElectricity(new BigDecimal(grid));
        dto.setStationServiceElectricity(new BigDecimal(station));
        dto.setDataSource("系统初始化");
        dto.setOperator("system");
        service.saveElectricityData(dto);
    }

    private void initElectricityAdjustments(ElectricityCollectionService service) {
        saveAdjustment(service, 1L, 1L, YearMonth.of(2026, 2), AdjustmentType.EQUIPMENT_FAULT, 200);
        saveAdjustment(service, 1L, 1L, YearMonth.of(2026, 6), AdjustmentType.CURTAILMENT, 300);
        saveAdjustment(service, 2L, 1L, YearMonth.of(2026, 4), AdjustmentType.MAINTENANCE, 150);
        saveAdjustment(service, 2L, 1L, YearMonth.of(2026, 9), AdjustmentType.CURTAILMENT, 250);
        saveAdjustment(service, 3L, 2L, YearMonth.of(2026, 3), AdjustmentType.TRIAL_OPERATION, 500);
        saveAdjustment(service, 3L, 2L, YearMonth.of(2026, 7), AdjustmentType.CURTAILMENT, 400);
        saveAdjustment(service, 3L, 2L, YearMonth.of(2026, 11), AdjustmentType.EQUIPMENT_FAULT, 350);
    }

    private void saveAdjustment(ElectricityCollectionService service, Long unitId, Long stationId,
                                YearMonth month, AdjustmentType type, int amount) {
        ElectricityAdjustmentDTO dto = new ElectricityAdjustmentDTO();
        dto.setUnitId(unitId);
        dto.setBoosterStationId(stationId);
        dto.setStatisticsMonth(month);
        dto.setAdjustmentType(type);
        dto.setAdjustmentElectricity(new BigDecimal(amount));
        dto.setDescription("系统初始化样本数据");
        dto.setOperator("system");
        service.saveElectricityAdjustment(dto);
    }

    private void initReports(ReportService reportService) {
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(2026, month);
            ReportGenerateDTO dto = new ReportGenerateDTO();
            dto.setStatisticsMonth(ym);
            dto.setOperator("system");
            dto.setRemark("系统初始化生成");
            List<CleanRevenueReport> reports = reportService.generateReport(dto);
            for (CleanRevenueReport report : reports) {
                reportService.transitionStatus(report.getId(), ReportStatus.PENDING_REVIEW, "system");
                reportService.transitionStatus(report.getId(), ReportStatus.PUBLISHED, "system");
            }
        }
    }
}
