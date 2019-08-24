package org.madgik.dtos;

import java.util.List;

public class CurationTopicsCategories {
    List<CurationTopicCategories> curationTopicCategories;


    public List<CurationTopicCategories> getCurationTopicCategories() {
        return curationTopicCategories;
    }

    public void setCurationTopicCategories(List<CurationTopicCategories> curationTopicCategories) {
        this.curationTopicCategories = curationTopicCategories;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public CurationTopicsCategories(List<CurationTopicCategories> curationTopicCategories, List<String> categories) {
        this.curationTopicCategories = curationTopicCategories;
        this.categories = categories;
    }

    List<String> categories;
}
