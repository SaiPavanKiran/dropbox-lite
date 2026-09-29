package org.rspk.dropbox_lite.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.rspk.dropbox_lite.utils.logs.CommonLogging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.hibernate.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Properties;

@Configuration
@PropertySource("classpath:application.properties")
@EnableTransactionManagement(proxyTargetClass = true)
public class HibernateConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.driver.classname}")
    private String driverClassName;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Bean
    public DataSource getDataSource() {
        HikariConfig config = new HikariConfig();
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(dbUrl);
        return new HikariDataSource(config);
    }

    @Bean
    public LocalSessionFactoryBean getSessionFactory(
            DataSource dataSource
    ) {
        LocalSessionFactoryBean localSessionFactoryBean = new LocalSessionFactoryBean();
        localSessionFactoryBean.setHibernateProperties(getHibernateProperties());
        localSessionFactoryBean.setPackagesToScan("org/rspk/dropbox_lite/model");
        localSessionFactoryBean.setDataSource(dataSource);
        return localSessionFactoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            SessionFactory sessionFactory
    ) {
        return new JpaTransactionManager(sessionFactory);
    }

    private Properties getHibernateProperties() {
        Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        return props;
    }

}
