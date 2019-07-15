package org.madgik.config;

import org.madgik.utils.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Configuration
@ComponentScan(basePackages = Constants.MADGIK_PACKAGE,
        excludeFilters =
                {@ComponentScan.Filter(type = FilterType.ANNOTATION, value = Configuration.class),
                        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Controller.class),
                        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = ControllerAdvice.class)})
public class ApplicationConfig {

    @Bean(name = Constants.MODEL_MAPPER)
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}

