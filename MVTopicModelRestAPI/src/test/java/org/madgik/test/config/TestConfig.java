package org.madgik.test.config;

import org.madgik.config.NamingConfig;
import org.madgik.utils.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = {Constants.MADGIK_PACKAGE})
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = Constants.PERSISTENCE_PACKAGE)
public class TestConfig {

    public static final String TRANSACTION = "transactionManager";
    public static final String MY_DATASOURCE = "dataSource";

    @Bean(name = MY_DATASOURCE)
    public DataSource dataSource(){
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
                .continueOnError(false).ignoreFailedDrops(true).build();
    }

    @Bean(Constants.ENTITY_MANAGER_FACTORY)
    public EntityManagerFactory entityManagerFactory() {
        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource());
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan(Constants.PERSISTENCE_PACKAGE);
        factory.setPersistenceUnitName("pUnitName");
        factory.setJpaProperties(getJPAProperties());
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

    private Properties getJPAProperties() {
        final Properties jpaProperties = new Properties();
        jpaProperties.setProperty("hibernate.hbm2ddl.auto", "create");
        jpaProperties.setProperty("hibernate.show_sql", "true");
        jpaProperties.setProperty("hibernate.format_sql", "true");
        jpaProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        jpaProperties.setProperty(Constants.HIBERNATE_NAMING_STRATEGY, NamingConfig.class.getName());
        jpaProperties.setProperty(Constants.HIBERNATE_ENTITY_MANAGER_FACTORY_NAME, Constants.ENTITY_MANAGER_FACTORY);
        return jpaProperties;
    }

    @Bean(name = Constants.MODEL_MAPPER)
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}

