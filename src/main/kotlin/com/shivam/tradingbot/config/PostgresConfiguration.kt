package com.shivam.tradingbot.config

import com.zaxxer.hikari.HikariDataSource
import com.shivam.tradingbot.adapter.out.persistence.JdbcPaperFillStoreAdapter
import com.shivam.tradingbot.adapter.out.persistence.JdbcPaperPortfolioStoreAdapter
import com.shivam.tradingbot.adapter.out.persistence.JdbcSignalStoreAdapter
import com.shivam.tradingbot.adapter.out.persistence.JdbcOptionPaperFillStore
import com.shivam.tradingbot.adapter.out.persistence.JdbcOptionPaperPortfolioStore
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

/**
 * Infrastructure configuration only. Core trading code never depends on this.
 * Tests use the `paper` profile and therefore start without PostgreSQL.
 */
@Configuration
@Profile("!paper")
@EnableTransactionManagement
class PostgresConfiguration {
    @Bean
    fun dataSource(
        @Value("\${spring.datasource.url}") url: String,
        @Value("\${spring.datasource.username}") username: String,
        @Value("\${spring.datasource.password}") password: String,
    ): DataSource = HikariDataSource().apply {
        jdbcUrl = url
        this.username = username
        this.password = password
    }

    @Bean
    fun jdbcTemplate(dataSource: DataSource) = JdbcTemplate(dataSource)

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)

    @Bean(initMethod = "migrate")
    fun flyway(dataSource: DataSource): Flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()

    @Bean
    fun paperPortfolioStore(jdbcTemplate: JdbcTemplate): JdbcPaperPortfolioStoreAdapter = JdbcPaperPortfolioStoreAdapter(jdbcTemplate)

    @Bean
    fun paperFillStore(jdbcTemplate: JdbcTemplate): JdbcPaperFillStoreAdapter = JdbcPaperFillStoreAdapter(jdbcTemplate)

    @Bean
    fun signalStore(jdbcTemplate: JdbcTemplate): JdbcSignalStoreAdapter = JdbcSignalStoreAdapter(jdbcTemplate)

    @Bean
    fun optionPaperPortfolioStore(jdbcTemplate: JdbcTemplate) = JdbcOptionPaperPortfolioStore(jdbcTemplate)

    @Bean
    fun optionPaperFillStore(jdbcTemplate: JdbcTemplate) = JdbcOptionPaperFillStore(jdbcTemplate)
}
