package org.madgik.persistence.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "visualization_experiment")
public class VisualizationExperiment {

    @Id
    @GenericGenerator(name = "native_generator", strategy = "native")
    @GeneratedValue(generator = "native_generator")
    @Column(name = "experimentid")
    private String experimentId;
    @Column(name = "ended")
    private LocalDateTime ended;
    @Column(name = "metadata")
    private String metadata;
    @Column(name = "modality0")
    private String modality0;
    @Column(name = "modality1")
    private String modality1;
    @Column(name = "modality2")
    private String modality2;
    @Column(name = "modality3")
    private String modality3;

    public VisualizationExperiment() {

    }

    public VisualizationExperiment(LocalDateTime ended, String metadata, String modality0, String modality1,
                                   String modality2, String modality3) {
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
        VisualizationExperiment that = (VisualizationExperiment) o;
        return Objects.equals(experimentId, that.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experimentId);
    }

    @Override
    public String toString() {
        return "VisualizationExperiment{" +
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
