package org.madgik.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "org.madgik")
public class WebConfig {

    /**
     * Load properties files. Check classpath first and then catalina base.
     * Override the properties in the application.properties file in catalina base.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer p = new PropertySourcesPlaceholderConfigurer();
        p.setIgnoreResourceNotFound(true);
        Resource[] resourceLocations = new Resource[]{
                new ClassPathResource("application.properties"),
                new FileSystemResource(System.getProperty("catalina.base") + "/application.properties")
        };
        p.setLocations(resourceLocations);
        return p;
    }
}
