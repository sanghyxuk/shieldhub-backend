package com.shieldhub.backend.repository;

import com.shieldhub.backend.entity.AnalysisVulnerabilities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisVulnerabilitiesRepository extends JpaRepository<AnalysisVulnerabilities, AnalysisVulnerabilities.AnalysisVulnerabilitiesId> {
}