package org.madgik.services;

import org.apache.commons.collections4.CollectionUtils;
import org.madgik.dtos.TopicSimilarityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TopicCategorySimilaritiesService {

    private static final Logger logger = LoggerFactory.getLogger(TopicCategorySimilaritiesService.class);

    public List<List<Integer>> calculateTopicCategorySimilarity(List<TopicSimilarityDto> topicSimilarityDtos,
                                                                List<String> categories,
                                                                Map<Integer,List<Integer>> topicCategoriesIdxMapping,
                                                                Double threshold) {
        if (CollectionUtils.isEmpty(topicSimilarityDtos)) return null;
        if (threshold == null) threshold = 0.8;

        List<Integer> categoryIndexes = new ArrayList<>();
        if (topicCategoriesIdxMapping == null || topicCategoriesIdxMapping.isEmpty()) {
            // make dummy single-category assignments
            topicCategoriesIdxMapping = new HashMap<>();
            for (Integer topicid: topicCategoriesIdxMapping.keySet()){
                List<Integer> assignments = new ArrayList<>(); assignments.add(0);
                topicCategoriesIdxMapping.put(topicid, assignments);
            }
            categories = new ArrayList<>();
            categories.add("<no category>");
        }
        for(int i=0;i<categories.size();++i) categoryIndexes.add(new Integer(i));
        // make categories to topics map
        HashMap<Integer, List<Integer>> categoriesToTopics = new HashMap<>();
        for (Integer topicid: topicCategoriesIdxMapping.keySet()){
            for(Integer cat: topicCategoriesIdxMapping.get(topicid)){
                if (! categoriesToTopics.containsKey(cat)) categoriesToTopics.put(cat, new ArrayList<>());
                categoriesToTopics.get(cat).add(topicid);
            }
        }

        // figure out all categories
        int num_categories = categories.size();


        Set<Integer> topicIds = topicSimilarityDtos.stream().flatMap(c -> Stream.of(c.getTopicId1(), c.getTopicId2())).collect(Collectors.toSet());
        int numItems = topicIds.size();

        double[][] similarities = new double[numItems][numItems];
        List<Integer> topicIdsList = new ArrayList<>(topicIds);
        topicIds.clear();
        Collections.sort(topicIdsList);

        for (TopicSimilarityDto dto : topicSimilarityDtos) {
            similarities[topicIdsList.indexOf(dto.getTopicId1())]
                    [topicIdsList.indexOf(dto.getTopicId2())] = dto.getSimilarity();
        }


        int[][] booleanSims = new int[numItems][numItems];
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                if (similarities[i][j] > threshold) {
                    booleanSims[i][j] = 1;
                } else {
                    booleanSims[i][j] = 0;
                }
            }
        }

        List<List<Integer>> categorySimilarities = new ArrayList<>();
        for (int i = 0; i < num_categories; ++i) {
            categorySimilarities.add(new ArrayList<>());
            for (int j = 0; j < num_categories; ++j)
                categorySimilarities.get(i).add(0);
        }
        for (int i = 0; i < num_categories; i++) {
            Integer currentCategory = categoryIndexes.get(i);
            // get all other categories
            for (int j = 0; j < num_categories; j++) {
                Integer otherCategory = categoryIndexes.get(j);
                // get similarity between their topics
                int sumsim = 0;
                for (Integer topic1 : categoriesToTopics.get(currentCategory)) {
                    for (Integer topic2 : categoriesToTopics.get(otherCategory)) {
                        int t1 = topicIdsList.indexOf(topic1);
                        int t2 = topicIdsList.indexOf(topic2);
                        sumsim += booleanSims[t1][t2];
                    }
                }
                categorySimilarities.get(currentCategory).set(otherCategory, sumsim);
            }
        }
        return categorySimilarities;
    }
}
