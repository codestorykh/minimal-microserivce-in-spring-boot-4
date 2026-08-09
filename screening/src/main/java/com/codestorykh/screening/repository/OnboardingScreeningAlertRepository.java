package com.codestorykh.screening.repository;

import com.codestorykh.screening.domain.onboarding.OnboardingScreeningAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OnboardingScreeningAlertRepository extends JpaRepository<OnboardingScreeningAlert, Long> {

    List<OnboardingScreeningAlert> findByScreeningId(String screeningId);
}
