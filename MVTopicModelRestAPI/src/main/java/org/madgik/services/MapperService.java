package org.madgik.services;

import org.apache.commons.collections4.CollectionUtils;
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

}
