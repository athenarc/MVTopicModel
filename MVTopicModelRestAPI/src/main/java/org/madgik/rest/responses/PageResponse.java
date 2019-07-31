package org.madgik.rest.responses;

import org.madgik.dtos.ParentDto;
import org.springframework.data.domain.Page;

public class PageResponse extends AbstractResponse {

    private Page<? extends ParentDto> body;

    public PageResponse() {

    }

    public PageResponse(String status, String message, Page<ParentDto> body) {
        super(status,message);
        this.body = body;
    }

    public Page<? extends ParentDto> getBody() {
        return body;
    }

    public void setBody(Page<? extends ParentDto> body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "PageResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", body=" + body +
                '}';
    }
}
