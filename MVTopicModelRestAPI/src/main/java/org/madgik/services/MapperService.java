package org.madgik.services;

import org.apache.commons.collections4.CollectionUtils;
import org.madgik.dtos.TopicCurationDto;
import org.madgik.dtos.TopicDto;
import org.madgik.persistence.entities.Topic;
import org.madgik.persistence.entities.TopicCuration;
import org.madgik.persistence.entities.TopicCurationId;
import org.madgik.persistence.entities.TopicId;
import org.madgik.utils.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service(Constants.MAPPER_SERVICE)
public class MapperService {

    @Autowired
    @Qualifier(Constants.MODEL_MAPPER)
    private ModelMapper modelMapper;

    public <T,S> List<T> getDtos(List<S> entities, Class<T> dtoClass) {
        return getObjectList(entities, dtoClass);
    }

    public <T,S> List<T> getEntities(List<S> dtos, Class<T> entityClass) {
        return getObjectList(dtos, entityClass);
    }

    public <T, S> T getDto(S entity, Class<T> dtoClass) {
        return getMappedObject(entity, dtoClass);
    }

    public <T, S> T getEntity(S dto, Class<T> entityClass) {
        return getMappedObject(dto, entityClass);
    }

    private <T,S> List<T> getObjectList(List<S> initialList, Class<T> mappingClass) {
        if(CollectionUtils.isEmpty(initialList)) return null;
        List<T> finalList = new ArrayList<>();
        initialList.forEach(initialObject-> {
            T finalObject = getMappedObject(initialObject, mappingClass);
            finalList.add(finalObject);
        });
        return finalList;
    }

    private <T,S> T getMappedObject(S initialObject, Class<T> mappingClass) {
        if(initialObject == null) return null;
        return modelMapper.map(initialObject, mappingClass);
    }

    public TopicCurationDto convertTopicCurationEntityToDto(TopicCuration topicCuration) {
        if (topicCuration == null) return null;
        TopicCurationDto topicCurationDto = new TopicCurationDto();
        if (topicCuration.getTopicCurationId() != null &&
                topicCuration.getTopicCurationId().getTopicId() != null) {
            topicCurationDto.setTopicId(topicCuration.getTopicCurationId().getTopicId().getId());
            topicCurationDto.setExperimentId(topicCuration.getTopicCurationId().getTopicId().getExperimentId());
        }
        topicCurationDto.setTopic(getDto(topicCuration.getTopic(), TopicDto.class));
        topicCurationDto.setCuratedDescription(topicCuration.getCuratedDescription());
        return topicCurationDto;
    }

    public TopicCuration convertTopicCurationDtoToEntity(TopicCurationDto topicCurationDto) {
        if (topicCurationDto == null) return null;
        TopicCuration topicCuration = new TopicCuration();
        TopicId topicId = new TopicId();
        TopicCurationId topicCurationId=new TopicCurationId();
        topicId.setId(topicCurationDto.getTopicId());
        topicId.setExperimentId(topicCurationDto.getExperimentId());
        topicCurationId.setTopicId(topicId);
        topicCuration.setTopicCurationId(topicCurationId);
        topicCuration.setTopic(getEntity(topicCurationDto.getTopic(), Topic.class));
        return topicCuration;
    }

    public List<TopicCurationDto> convertTopicCurationEntityListToDto(List<TopicCuration> topicCurations) {
        if (CollectionUtils.isEmpty(topicCurations)) return null;
        List<TopicCurationDto> topicCurationDtos = new ArrayList<>();
        topicCurations.forEach(topicCuration -> {
            if (topicCuration != null) {
                topicCurationDtos.add(convertTopicCurationEntityToDto(topicCuration));
            }
        });
        return topicCurationDtos;
    }

    public List<TopicCuration> convertTopicCurationDtoListToEntity(List<TopicCurationDto> topicCurationDtos) {
        if (CollectionUtils.isEmpty(topicCurationDtos)) return null;
        List<TopicCuration> topicCurations = new ArrayList<>();
        topicCurationDtos.forEach(topicCurationDto -> {
            if (topicCurationDto != null) {
                topicCurations.add(convertTopicCurationDtoToEntity(topicCurationDto));
            }
        });
        return topicCurations;
    }

}
