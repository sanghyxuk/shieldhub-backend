package com.shieldhub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Vulnerabilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vulnerabilities {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vulnerability_id")
    private Integer vulnerabilityId;

    @Column(name = "vulnerability_type", nullable = false)
    private String vulnerabilityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "details")
    private String details;

    @Column(name = "detected_pattern")
    private String detectedPattern;

    public enum Severity {
        HIGH, MEDIUM, LOW
    }
}