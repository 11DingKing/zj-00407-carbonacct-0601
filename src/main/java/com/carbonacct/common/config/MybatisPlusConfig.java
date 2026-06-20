package com.carbonacct.common.config;

import com.carbonacct.common.converter.YearMonthTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public YearMonthTypeHandler yearMonthTypeHandler() {
        return new YearMonthTypeHandler();
    }
}
