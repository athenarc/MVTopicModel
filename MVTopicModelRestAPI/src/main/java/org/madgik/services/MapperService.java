package org.madgik.services;

import org.apache.commons.collections4.CollectionUtils;
import org.madgik.dtos.DocTopicDto;
import org.madgik.dtos.TopicCurationDto;
import org.madgik.dtos.TopicDto;
import org.madgik.dtos.VisualizationTopicDocsPerJournalDto;
import org.madgik.persistence.compositeIds.DocTopicId;
import org.madgik.persistence.compositeIds.TopicCurationId;
import org.madgik.persistence.compositeIds.TopicId;
import org.madgik.persistence.compositeIds.VisualizationTopicDocsPerJournalId;
import org.madgik.persistence.entities.*;
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
        topicCuration.setCuratedDescription(topicCurationDto.getCuratedDescription());
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

    public DocTopicDto convertDocTopicEntityToDto(DocTopic docTopic) {
        if (docTopic == null) return null;
        DocTopicDto docTopicDto = new DocTopicDto();
        if (docTopic.getDocTopicId() != null) {
            docTopicDto.setDocId(docTopic.getDocTopicId().getDocId());
            if (docTopic.getDocTopicId().getTopicId() != null) {
                docTopicDto.setTopicId(docTopic.getDocTopicId().getTopicId());
                docTopicDto.setExperimentId(docTopic.getDocTopicId().getExperimentId());
            }
        }
        docTopicDto.setWeight(docTopic.getWeight());
        docTopicDto.setInferred(docTopic.getInferred());
        return docTopicDto;
    }

    public DocTopic convertDocTopicDtoToEntity(DocTopicDto docTopicDto) {
        if (docTopicDto == null) return null;
        DocTopic docTopic = new DocTopic();
        DocTopicId docTopicId = new DocTopicId(docTopicDto.getDocId(), docTopicDto.getTopicId(),
                docTopicDto.getExperimentId());
        docTopic.setDocTopicId(docTopicId);
        docTopic.setWeight(docTopicDto.getWeight());
        docTopic.setInferred(docTopicDto.getInferred());
        return docTopic;
    }

    public List<DocTopicDto> convertDocTopicListEntityToDto(List<DocTopic> docTopics) {
        if (CollectionUtils.isEmpty(docTopics)) return null;
        List<DocTopicDto> docTopicDtos = new ArrayList<>();
        docTopics.forEach(docTopic -> docTopicDtos.add(convertDocTopicEntityToDto(docTopic)));
        return docTopicDtos;
    }

    public List<DocTopic> convertDocTopicListDtoToEntity(List<DocTopicDto> docTopicDtos) {
        if (CollectionUtils.isEmpty(docTopicDtos)) return null;
        List<DocTopic> docTopics = new ArrayList<>();
        docTopicDtos.forEach(docTopicDto -> docTopics.add(convertDocTopicDtoToEntity(docTopicDto)));
        return docTopics;
    }

    public VisualizationTopicDocsPerJournalDto convertVisualizationTopicDocsPerJournalEntityToDto(VisualizationTopicDocsPerJournal entity) {
        if (entity == null) return null;
        VisualizationTopicDocsPerJournalDto dto = new VisualizationTopicDocsPerJournalDto();
        if (entity.getVisualizationTopicDocsPerJournalId() != null) {
            dto.setTopicId(entity.getVisualizationTopicDocsPerJournalId().getTopicId());
            dto.setExperimentId(entity.getVisualizationTopicDocsPerJournalId().getExperimentId());
            dto.setJournalId(entity.getVisualizationTopicDocsPerJournalId().getJournalId());
        }
        dto.setCount(entity.getCount());
        dto.setJournalTitle(entity.getJournalTitle());
        dto.setDocTopicCount(entity.getDocTopicCount());
        return dto;
    }

    public VisualizationTopicDocsPerJournal convertVisualizationTopicDocsPerJournalDtoToEntity(VisualizationTopicDocsPerJournalDto dto) {
        if (dto == null) return null;
        VisualizationTopicDocsPerJournal entity = new VisualizationTopicDocsPerJournal();
        VisualizationTopicDocsPerJournalId id = new VisualizationTopicDocsPerJournalId(dto.getTopicId(), dto.getExperimentId(), dto.getJournalId());
        entity.setVisualizationTopicDocsPerJournalId(id);
        entity.setCount(dto.getCount());
        entity.setJournalTitle(dto.getJournalTitle());
        entity.setDocTopicCount(dto.getDocTopicCount());
        return entity;
    }

    public List<VisualizationTopicDocsPerJournalDto> convertVisualizationTopicDocsPerJournalEntityListToDto(List<VisualizationTopicDocsPerJournal> entities) {
        if (CollectionUtils.isEmpty(entities)) return null;
        List<VisualizationTopicDocsPerJournalDto> dtos = new ArrayList<>();
        entities.forEach(entity -> {
            if (entity != null) dtos.add(convertVisualizationTopicDocsPerJournalEntityToDto(entity));
        });
        return dtos;
    }

    public List<VisualizationTopicDocsPerJournal> convertVisualizationTopicDocsPerJournalDtoListToEntity(List<VisualizationTopicDocsPerJournalDto> dtos) {
        if (CollectionUtils.isEmpty(dtos)) return null;
        List<VisualizationTopicDocsPerJournal> entities = new ArrayList<>();
        dtos.forEach(dto -> {
            if (dto != null) entities.add(convertVisualizationTopicDocsPerJournalDtoToEntity(dto));
        });
        return entities;
    }
}
