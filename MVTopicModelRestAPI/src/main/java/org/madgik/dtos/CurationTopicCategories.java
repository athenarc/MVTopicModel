package org.madgik.dtos;

import java.util.List;

public class CurationTopicCategories {
   public Integer getTopicid() {
      return topicid;
   }

   public void setTopicid(Integer topicid) {
      this.topicid = topicid;
   }

   public List<Double> getCategoryAssignments() {
      return categoryAssignments;
   }

   public void setCategoryAssignments(List<Double> categoryAssignments) {
      this.categoryAssignments = categoryAssignments;
   }

   public CurationTopicCategories(Integer topicid, List<Double> categoryAssignments) {
      this.topicid = topicid;
      this.categoryAssignments = categoryAssignments;
   }

   Integer topicid;
   List<Double> categoryAssignments;

}
