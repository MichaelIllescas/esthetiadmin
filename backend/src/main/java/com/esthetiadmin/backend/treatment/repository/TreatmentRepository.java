package com.esthetiadmin.backend.treatment.repository;

import com.esthetiadmin.backend.treatment.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TreatmentRepository extends JpaRepository<Treatment, Long>, JpaSpecificationExecutor<Treatment> {
}
