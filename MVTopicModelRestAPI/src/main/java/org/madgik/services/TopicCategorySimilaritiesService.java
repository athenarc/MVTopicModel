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

    public List<List<Integer>> calculateTopicCategorySimilarity(List<TopicSimilarityDto> topicSimilarityDtos, Integer[] assignments, Double threshold) {
        if (CollectionUtils.isEmpty(topicSimilarityDtos)) return null;
        if (threshold == null) threshold = 0.8;
        if (assignments == null) {
            assignments = new Integer[topicSimilarityDtos.size()];
            for(int i=0;i<topicSimilarityDtos.size();++i) assignments[i] = 0;
        }
        List<Integer> categories = new ArrayList<>();
        for(Integer cat: assignments) if(!categories.contains(cat)) categories.add(cat);
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

        logger.debug("Assignments: " + Arrays.toString(assignments));
        List<List<Integer>> categorySimilarities = new ArrayList<>();
        for (int i=0;i< num_categories; ++i) {
            categorySimilarities.add(new ArrayList<>());
            for (int j = 0; j < num_categories; ++j)
                categorySimilarities.get(j).add(0);
        }
        for (int i = 0; i < num_categories; i++) {
            List<Integer> indexOfCategory = getCategoryIndices(assignments, categories, numItems, i);
            for (int j = 0; j < num_categories; j++) {
                List<Integer> indexOfOtherCategory = getCategoryIndices(assignments, categories, numItems, j);
                categorySimilarities.get(i).set(j, getCategorySimilaritySum(indexOfCategory, indexOfOtherCategory, booleanSims));
            }
        }
        return categorySimilarities;
    }

    private List<Integer> getCategoryIndices(Integer[] assignments, List<Integer>categoriesArray, int numItems, int categoryIndex) {
        List<Integer> indexOfCategory = new ArrayList<>();
        for (int ass1 = 0; ass1 < numItems; ass1++) {
            if (assignments[ass1] == categoriesArray.get(categoryIndex)) {
                indexOfCategory.add(ass1);
            }
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
