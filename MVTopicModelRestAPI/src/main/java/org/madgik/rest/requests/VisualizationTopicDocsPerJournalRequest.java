package org.madgik.rest.requests;

public class VisualizationTopicDocsPerJournalRequest extends PageableRequest {

    private Integer topicId;
    private String experimentId;

    public VisualizationTopicDocsPerJournalRequest() {

    }

    public VisualizationTopicDocsPerJournalRequest(String filter, String sortOrder,
                                                   Integer pageNumber, Integer pageSize,
                                                   Integer topicId, String experimentId) {
        super(filter, sortOrder, pageNumber, pageSize);
        this.topicId = topicId;
        this.experimentId = experimentId;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }
}
