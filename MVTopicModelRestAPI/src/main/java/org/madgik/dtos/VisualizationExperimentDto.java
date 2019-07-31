package org.madgik.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.Objects;

public class VisualizationExperimentDto extends ParentDto {

    private String experimentId;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime ended;
    private String metadata;
    private String modality0;
    private String modality1;
    private String modality2;
    private String modality3;

    public VisualizationExperimentDto() {

    }

    public VisualizationExperimentDto(String experimentId, LocalDateTime ended, String metadata, String modality0,
                                      String modality1, String modality2, String modality3) {
        this.experimentId = experimentId;
        this.ended = ended;
        this.metadata = metadata;
        this.modality0 = modality0;
        this.modality1 = modality1;
        this.modality2 = modality2;
        this.modality3 = modality3;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public LocalDateTime getEnded() {
        return ended;
    }

    public void setEnded(LocalDateTime ended) {
        this.ended = ended;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getModality0() {
        return modality0;
    }

    public void setModality0(String modality0) {
        this.modality0 = modality0;
    }

    public String getModality1() {
        return modality1;
    }

    public void setModality1(String modality1) {
        this.modality1 = modality1;
    }

    public String getModality2() {
        return modality2;
    }

    public void setModality2(String modality2) {
        this.modality2 = modality2;
    }

    public String getModality3() {
        return modality3;
    }

    public void setModality3(String modality3) {
        this.modality3 = modality3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisualizationExperimentDto that = (VisualizationExperimentDto) o;
        return Objects.equals(experimentId, that.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experimentId);
    }

    @Override
    public String toString() {
        return "VisualizationExperimentDto{" +
                "experimentId='" + experimentId + '\'' +
                ", ended=" + ended +
                ", metadata='" + metadata + '\'' +
                ", modality0='" + modality0 + '\'' +
                ", modality1='" + modality1 + '\'' +
                ", modality2='" + modality2 + '\'' +
                ", modality3='" + modality3 + '\'' +
                '}';
    }
}
