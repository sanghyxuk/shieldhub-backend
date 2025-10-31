package com.shieldhub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "AnalysisResults")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResults {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Integer analysisId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "url_to_analyze", nullable = false)
    private String urlToAnalyze;

    @CreationTimestamp
    @Column(name = "analysis_date", nullable = false)
    private LocalDateTime analysisDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status")
    private AnalysisStatus status;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(name = "error_message")
    private String errorMessage;

    public enum AnalysisStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }
}