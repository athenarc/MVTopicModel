package org.madgik.rest.requests;

import java.util.ArrayList;
import java.util.List;

public class DocumentInfoRequest extends PageableRequest {

    private List<String> documentIds = new ArrayList<>();
    private Integer numChars;

    public DocumentInfoRequest() {

    }

    public DocumentInfoRequest(String filter, String sortOrder, Integer pageNumber, Integer pageSize, List<String> documentIds, Integer numChars) {
        super(filter, sortOrder, pageNumber, pageSize);
        this.documentIds = documentIds;
        this.numChars = numChars;
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

    @Override
    public String toString() {
        return "DocumentInfoRequest{" +
                "documentIds=" + documentIds +
                ", numChars=" + numChars +
                '}';
    }
}
