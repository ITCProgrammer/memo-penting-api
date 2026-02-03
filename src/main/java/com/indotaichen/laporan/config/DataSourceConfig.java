package com.indotaichen.laporan.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // =====================================================
    // DB2 NOWPRD DataSource - Primary for ITX data
    // This is conn1 in PHP (db2_connect)
    // =====================================================
    @Primary
    @Bean(name = "db2DataSource")
    @ConfigurationProperties(prefix = "datasource.db2")
    public DataSource db2DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "db2JdbcTemplate")
    public JdbcTemplate db2JdbcTemplate(@Qualifier("db2DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // =====================================================
    // SQL Server nowprd DataSource (SVR19 - 10.0.0.221)
    // This is con_nowprd in PHP
    // =====================================================
    @Bean(name = "nowprdDataSource")
    @ConfigurationProperties(prefix = "datasource.sqlserver-nowprd")
    public DataSource nowprdDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "nowprdJdbcTemplate")
    public JdbcTemplate nowprdJdbcTemplate(@Qualifier("nowprdDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // =====================================================
    // MySQL db_qc DataSource (10.0.0.10)
    // This is con_db_qc in PHP (still using MySQL)
    // =====================================================
    @Bean(name = "qcDataSource")
    @ConfigurationProperties(prefix = "datasource.sqlserver-qc")
    public DataSource qcDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "qcJdbcTemplate")
    public JdbcTemplate qcJdbcTemplate(@Qualifier("qcDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // =====================================================
    // SQL Server db_dying DataSource (SVR19 - 10.0.0.221)
    // This is con_db_dyeing in PHP
    // =====================================================
    @Bean(name = "dyeingDataSource")
    @ConfigurationProperties(prefix = "datasource.sqlserver-dyeing")
    public DataSource dyeingDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "dyeingJdbcTemplate")
    public JdbcTemplate dyeingJdbcTemplate(@Qualifier("dyeingDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // =====================================================
    // SQL Server db_finishing DataSource (SVR19 - 10.0.0.221)
    // This is con_finishing / con_db_finishing in PHP
    // =====================================================
    @Bean(name = "finishingDataSource")
    @ConfigurationProperties(prefix = "datasource.sqlserver-finishing")
    public DataSource finishingDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "finishingJdbcTemplate")
    public JdbcTemplate finishingJdbcTemplate(@Qualifier("finishingDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
