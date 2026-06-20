package com.carbonacct.common.config;

import com.carbonacct.common.enums.ApprovalStatus;
import com.carbonacct.domain.entity.BoosterStation;
import com.carbonacct.domain.entity.ConversionCoefficient;
import com.carbonacct.domain.entity.Unit;
import com.carbonacct.service.BoosterStationService;
import com.carbonacct.service.ConversionCoefficientService;
import com.carbonacct.service.UnitService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UnitService unitService,
                                      BoosterStationService boosterStationService,
                                      ConversionCoefficientService coefficientService) {
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
        };
    }
}
