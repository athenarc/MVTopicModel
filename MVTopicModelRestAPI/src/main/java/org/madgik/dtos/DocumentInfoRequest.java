package org.madgik.dtos;

import java.util.ArrayList;
import java.util.List;

public class DocumentInfoRequest {

    private List<String> documentIds = new ArrayList<>();
    private Integer numChars;
    private Integer offset;
    private Integer limit;

    public DocumentInfoRequest() {

    }

    public DocumentInfoRequest(List<String> documentIds, Integer numChars, Integer offset, Integer limit) {
        this.documentIds = documentIds;
        this.numChars = numChars;
        this.offset = offset;
        this.limit = limit;
    }

    public List<String> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<String> documentIds) {
        this.documentIds = documentIds;
    }

    public Integer getNumChars() {
        return numChars;
    }

    public void setNumChars(Integer numChars) {
        this.numChars = numChars;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "DocumentInfoRequest{" +
                "documentIds=" + documentIds +
                ", numChars=" + numChars +
                ", offset=" + offset +
                ", limit=" + limit +
                '}';
    }
}
