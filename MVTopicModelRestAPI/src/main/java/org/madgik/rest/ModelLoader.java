package org.madgik.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class ModelLoader implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    @Value("${model.path}")
    private String modelPath;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        // TODO load from disk on application startup using model path variable
        // TODO set the model.path in MVTopicModelRestAPI pom.xml in properties in profile
        logger.info("Model path is: " + modelPath);
    }
}
