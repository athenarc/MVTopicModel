package org.madgik.services;

import org.apache.commons.lang.StringUtils;
import org.madgik.dtos.VisualizationCurationTopicLabelsDto;
import org.madgik.persistence.repositories.VisualizationCurationTopicLabelsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VisualizationCurationTopicLabelsService {

    @Autowired
    private VisualizationCurationTopicLabelsRepository repo;

    @Autowired
    private MapperService mapperService;

    public VisualizationCurationTopicLabelsDto getVisualizationCurationTopicLabelsByExperimentId(String experimentId, String curator) {
        if (StringUtils.isBlank(curator)) curator = "frontend";
        Set<String> categories = new HashSet<>();
        VisualizationCurationTopicLabelsDto newDto = new VisualizationCurationTopicLabelsDto();
        List<VisualizationCurationTopicLabelsDto> visualizationCurationTopicLabelsDtos =
                mapperService.getDtos(repo.findAllByExperimentIdAndCurator(experimentId, curator),
                        VisualizationCurationTopicLabelsDto.class);
        visualizationCurationTopicLabelsDtos.forEach(dto -> {
            List<String> categs = Arrays.asList(dto.getCategoryLabel().split("==>"));
            categs = categs.stream().map(String::trim).collect(Collectors.toList());
            newDto.getTopicCategoriesMapping().put(dto.getCurationDetailsId().getTopicId(), categs);
            categories.addAll(categs);
        });
        newDto.getCategories().addAll(categories);
        for(Map.Entry<Integer,List<String>> entry : newDto.getTopicCategoriesMapping().entrySet()) {
            List<Integer> indices = new ArrayList<>();
            for(String category : entry.getValue()) {
                int index = newDto.getCategories().indexOf(category);
                indices.add(index);
            }
            newDto.getTopicCategoriesIdxMapping().put(entry.getKey(), indices);
        }
        return newDto;
    }
}
