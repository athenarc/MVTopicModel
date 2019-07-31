package org.madgik.rest.requests;

public class PageableRequest implements Request {

    protected String filter;
    protected String sortOrder;
    protected Integer pageNumber;
    protected Integer pageSize;

    public PageableRequest() {

    }

    public PageableRequest(String filter, String sortOrder, Integer pageNumber, Integer pageSize) {
        this.filter = filter;
        this.sortOrder = sortOrder;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public String toString() {
        return "PageableRequest{" +
                "filter='" + filter + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                ", pageNumber=" + pageNumber +
                ", pageSize='" + pageSize + '\'' +
                '}';
    }
}
