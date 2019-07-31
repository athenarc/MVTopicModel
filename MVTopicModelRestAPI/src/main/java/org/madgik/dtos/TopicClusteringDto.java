package org.madgik.dtos;

import java.io.Serializable;

public class TopicClusteringDto implements Serializable {

    private Integer clusterId;
    private Integer clusterMember;

    public TopicClusteringDto() {

    }

    public TopicClusteringDto(Integer clusterId, Integer clusterMember) {
        this.clusterId = clusterId;
        this.clusterMember = clusterMember;
    }

    public Integer getClusterId() {
        return clusterId;
    }

    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
    }

    public Integer getClusterMember() {
        return clusterMember;
    }

    public void setClusterMember(Integer clusterMember) {
        this.clusterMember = clusterMember;
    }

    @Override
    public String toString() {
        return "TopicClusteringDto{" +
                "clusterId=" + clusterId +
                ", clusterMember=" + clusterMember +
                '}';
    }
}
