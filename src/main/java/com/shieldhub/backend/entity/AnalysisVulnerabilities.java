package com.shieldhub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "AnalysisVulnerabilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisVulnerabilities {

    @EmbeddedId
    private AnalysisVulnerabilitiesId id;

    @Column(name = "detection_context")
    private String detectionContext;

    // --- Embedded Primary Key Class ---
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisVulnerabilitiesId implements Serializable {
        @Column(name = "analysis_id")
        private Integer analysisId;

        @Column(name = "vulnerability_id")
        private Integer vulnerabilityId;
    }
}