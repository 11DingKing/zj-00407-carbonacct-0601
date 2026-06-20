CREATE TABLE IF NOT EXISTS t_booster_station (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_code VARCHAR(50) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    capacity DECIMAL(18,4),
    voltage_level VARCHAR(20),
    location VARCHAR(200),
    status INT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_code VARCHAR(50) NOT NULL,
    unit_name VARCHAR(100) NOT NULL,
    booster_station_id BIGINT,
    capacity DECIMAL(18,4),
    unit_type VARCHAR(20),
    location VARCHAR(200),
    status INT,
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_electricity_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    booster_station_id BIGINT NOT NULL,
    statistics_month VARCHAR(7) NOT NULL,
    grid_connected_electricity DECIMAL(18,4) NOT NULL,
    station_service_electricity DECIMAL(18,4) NOT NULL,
    data_source VARCHAR(100),
    operator VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_elec_unit_month (unit_id, statistics_month)
);

CREATE TABLE IF NOT EXISTS t_electricity_adjustment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    booster_station_id BIGINT NOT NULL,
    statistics_month VARCHAR(7) NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL,
    adjustment_electricity DECIMAL(18,4) NOT NULL,
    start_date DATE,
    end_date DATE,
    description VARCHAR(500),
    approval_doc VARCHAR(200),
    operator VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_conversion_coefficient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(20) NOT NULL UNIQUE,
    effective_date DATE NOT NULL,
    expiry_date DATE,
    standard_coal_coefficient DECIMAL(18,6) NOT NULL,
    carbon_dioxide_coefficient DECIMAL(18,6) NOT NULL,
    household_electricity_consumption DECIMAL(18,4) NOT NULL,
    approval_status VARCHAR(20) NOT NULL,
    approver VARCHAR(50),
    approval_date DATE,
    approval_opinion VARCHAR(500),
    operator VARCHAR(50),
    remark VARCHAR(500),
    is_current BOOLEAN,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_type VARCHAR(50) NOT NULL,
    business_id BIGINT NOT NULL,
    business_no VARCHAR(50),
    approval_status VARCHAR(20) NOT NULL,
    approver VARCHAR(50),
    approval_opinion VARCHAR(500),
    operator VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_clean_revenue_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_no VARCHAR(50) NOT NULL UNIQUE,
    statistics_month VARCHAR(7) NOT NULL,
    unit_id BIGINT NOT NULL,
    booster_station_id BIGINT NOT NULL,
    coefficient_id BIGINT NOT NULL,
    total_grid_electricity DECIMAL(18,4) NOT NULL,
    total_station_service_electricity DECIMAL(18,4) NOT NULL,
    total_adjustment_electricity DECIMAL(18,4) NOT NULL,
    effective_clean_electricity DECIMAL(18,4) NOT NULL,
    standard_coal_saving DECIMAL(18,4) NOT NULL,
    carbon_dioxide_reduction DECIMAL(18,4) NOT NULL,
    household_count DECIMAL(18,0) NOT NULL,
    report_status VARCHAR(20) NOT NULL,
    prepared_by VARCHAR(50),
    reviewed_by VARCHAR(50),
    published_by VARCHAR(50),
    corrected_by VARCHAR(50),
    remark VARCHAR(500),
    version INT DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_report_unit_month (unit_id, statistics_month)
);

CREATE TABLE IF NOT EXISTS t_report_correction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    report_no VARCHAR(50) NOT NULL,
    original_version INT NOT NULL,
    corrected_version INT NOT NULL,
    before_effective_electricity DECIMAL(18,4) NOT NULL,
    after_effective_electricity DECIMAL(18,4) NOT NULL,
    before_standard_coal_saving DECIMAL(18,4) NOT NULL,
    after_standard_coal_saving DECIMAL(18,4) NOT NULL,
    before_carbon_dioxide_reduction DECIMAL(18,4) NOT NULL,
    after_carbon_dioxide_reduction DECIMAL(18,4) NOT NULL,
    before_household_count DECIMAL(18,0) NOT NULL,
    after_household_count DECIMAL(18,0) NOT NULL,
    correction_reason VARCHAR(500) NOT NULL,
    corrected_by VARCHAR(50),
    approval_opinion VARCHAR(500),
    remark VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);
