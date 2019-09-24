package org.madgik.services;

        import com.google.gson.Gson;
        import org.apache.commons.collections4.CollectionUtils;
        import org.madgik.dtos.TopicSimilarityDto;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.stereotype.Service;

        import java.io.FileReader;
        import java.util.*;
        import java.util.stream.Collectors;
        import java.util.stream.Stream;

@Service
public class TopicCategorySimilaritiesService {

    private static final Logger logger = LoggerFactory.getLogger(TopicCategorySimilaritiesService.class);


    public Map<Integer, Integer> getTopicCategoryAssignments(List<TopicSimilarityDto> topicSimilarityDtos) {
        // get the cat. assignments from the DB
        Map<Integer, Integer> assignments = new HashMap<>();
        return assignments;
    }

    public List<List<Integer>> calculateTopicCategorySimilarity(List<TopicSimilarityDto> topicSimilarityDtos, Map<Integer, Integer> assignments, Double threshold) {
        if (CollectionUtils.isEmpty(topicSimilarityDtos)) return null;
        if (threshold == null) threshold = 0.8;
        // collect topic ids
        Set<Integer> topicIds = topicSimilarityDtos.stream().flatMap(c -> Stream.of(c.getTopicId1(), c.getTopicId2())).collect(Collectors.toSet());
        int numItems = topicIds.size();
        List<Integer> topicIdsList = new ArrayList<>(topicIds);
        topicIds.clear();
        Collections.sort(topicIdsList);
        // collect topic similarities
        double[][] similarities = new double[numItems][numItems];
        for (TopicSimilarityDto dto : topicSimilarityDtos) {
            similarities[topicIdsList.indexOf(dto.getTopicId1())]
                    [topicIdsList.indexOf(dto.getTopicId2())] = dto.getSimilarity();
        }
        // get category assignments
        List<Integer> categories = new ArrayList<>();
        if (assignments == null || assignments.isEmpty()) {
            // single-category for all topics
            assignments = new HashMap<>();
            for(int i=0;i<topicIdsList.size();++i) assignments.put(topicIdsList.get(i), 0);
        }
        for(Integer cat: assignments.values()) if(!categories.contains(cat)) categories.add(cat);
        int num_categories = categories.size();


        // apply similarity threshold
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

        logger.debug("Assignments: " + assignments);
        List<List<Integer>> categorySimilarities = new ArrayList<>();
        for (int i=0;i< num_categories; ++i) {
            categorySimilarities.add(new ArrayList<>());
            for (int j = 0; j < num_categories; ++j)
                categorySimilarities.get(j).add(0);
        }

        for (int c=0; c<categories.size(); ++c){
            Integer category = categories.get(c);
            List<Integer> indexOfCategory = getCategoryIndices(topicIdsList, assignments, category);
                for (int oc=0; oc<categories.size(); ++oc){
                    Integer otherCategory = categories.get(oc);
                List<Integer> indexOfOtherCategory = getCategoryIndices(topicIdsList, assignments, otherCategory);
                categorySimilarities.get(c).set(oc, getCategorySimilaritySum(indexOfCategory, indexOfOtherCategory, booleanSims));
            }
        }
        return categorySimilarities;
    }

    private List<Integer> getCategoryIndices(List<Integer> topicIds, Map<Integer, Integer> assignments, int category) {
        List<Integer> indexOfCategory = new ArrayList<>();
        for(int i=0;i<topicIds.size();++i){
            if (assignments.get(topicIds.get(i)) == category) indexOfCategory.add(i);
        }
        return indexOfCategory;
    }

    private int getCategorySimilaritySum(List<Integer> firstCategoryIndices, List<Integer> secondCategoryIndices, int[][] booleanSimilarities) {
        int sum = 0;
        for (Integer firstCategoryIdx : firstCategoryIndices) {
            for (Integer secondCategoryIdx : secondCategoryIndices) {
                if (booleanSimilarities[firstCategoryIdx][secondCategoryIdx] == 1) {
                    sum++;
                }
            }
        }
        return sum;
    }
}
