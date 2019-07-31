package org.madgik.rest.responses;

import org.madgik.dtos.ParentDto;

public class SingleObjectResponse extends AbstractResponse {

    private ParentDto body;

    public SingleObjectResponse() {

    }

    public SingleObjectResponse(String status, String message, ParentDto body) {
        super(status,message);
        this.body = body;
    }

    public ParentDto getBody() {
        return body;
    }

    public void setBody(ParentDto body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "SingleObjectResponse{" +
                "body=" + body +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
