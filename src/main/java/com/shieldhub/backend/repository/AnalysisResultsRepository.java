package com.shieldhub.backend.repository;

import com.shieldhub.backend.entity.AnalysisResults;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisResultsRepository extends JpaRepository<AnalysisResults, Integer> {
}