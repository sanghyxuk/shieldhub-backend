package com.shieldhub.backend.repository;

import com.shieldhub.backend.entity.AnalysisVulnerabilities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisVulnerabilitiesRepository extends JpaRepository<AnalysisVulnerabilities, AnalysisVulnerabilities.AnalysisVulnerabilitiesId> {
    List<AnalysisVulnerabilities> findByIdAnalysisId(Integer analysisId);
}