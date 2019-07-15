package org.madgik.config;

import org.apache.commons.lang3.StringUtils;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = Constants.PERSISTENCE_PACKAGE)
public class DatabaseConfig {

    @Value("${jdbc.jndi.url}")
    private String jndiUrl;

    @Value("${application.database}")
    private String database;

    @Value("${jdbc.driverClassName}")
    private String driverClassName;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    @Value("${jdbc.url}")
    private String url;


    @Bean(name = Constants.DATASOURCE)
    @Primary
    public DataSource dataSource() {
        if (StringUtils.isBlank(jndiUrl)) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(driverClassName);
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            return dataSource;
        } else {
            final JndiDataSourceLookup dsLookup = new JndiDataSourceLookup();
            dsLookup.setResourceRef(true);
            return dsLookup.getDataSource(jndiUrl);
        }

    }

    @Bean(name = Constants.ENTITY_MANAGER_FACTORY)
    @Primary
    public EntityManagerFactory entityManagerFactory() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(Database.valueOf(database));
        vendorAdapter.setGenerateDdl(true);
        if (database.equalsIgnoreCase(Constants.SQL_SERVER)) {
            vendorAdapter.setDatabasePlatform(Constants.HIBERNATE_SQL_SERVER_DIALECT);
        } else if (database.equalsIgnoreCase(Constants.ORACLE)) {
            vendorAdapter.setDatabasePlatform(Constants.HIBERNATE_ORACLE_DIALECT);
        } else if(database.equalsIgnoreCase(Constants.POSTGRESQL)) {
            vendorAdapter.setDatabasePlatform(Constants.HIBERNATE_POSTGRESQL_DIALECT);
        }
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan(Constants.PERSISTENCE_PACKAGE);
        factory.setDataSource(dataSource());
        factory.setJpaDialect(new HibernateJpaDialect());

        Map<String, Object> properties = factory.getJpaPropertyMap();
        properties.put(Constants.HIBERNATE_NAMING_STRATEGY, NamingConfig.class.getName());
        properties.put(Constants.HIBERNATE_ENTITY_MANAGER_FACTORY_NAME, Constants.ENTITY_MANAGER_FACTORY);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean(name = Constants.TRANSACTION_MANAGER)
    @Primary
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory());
        return txManager;
    }

    @Bean(name = Constants.ENTITY_MANAGER)
    @Primary
    public EntityManager entityManager() {
        EntityManagerFactory entityManagerFactory = entityManagerFactory();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        return entityManager;
    }
}
