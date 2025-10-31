package com.shieldhub.backend.repository;

import com.shieldhub.backend.entity.Vulnerabilities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VulnerabilitiesRepository extends JpaRepository<Vulnerabilities, Integer> {
    // 이미 등록된 취약점인지 패턴과 타입으로 확인
    Optional<Vulnerabilities> findByVulnerabilityTypeAndDetectedPattern(String type, String pattern);
}