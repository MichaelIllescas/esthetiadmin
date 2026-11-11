package com.esthetiadmin.backend.catalog.repository;

import com.esthetiadmin.backend.catalog.entity.Offering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfferingRepository extends JpaRepository<Offering, Long> {

    List<Offering> findAllByOrderByNameAsc();

    boolean existsByCategoryId(Long categoryId);

    List<Offering> findAllByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndName(Long categoryId, String name);

    boolean existsByCategoryIdAndNameAndIdNot(Long categoryId, String name, Long id);

    /** Native on purpose: the treatment module does not exist yet and the catalog must not depend on it. */
    @Query(value = "SELECT COUNT(*) FROM treatment WHERE service_id = :serviceId", nativeQuery = true)
    long countTreatments(@Param("serviceId") Long serviceId);

    /** Native for the same reason: the catalog must not depend on the appointment module. */
    @Query(value = "SELECT COUNT(*) FROM appointment WHERE service_id = :serviceId", nativeQuery = true)
    long countAppointments(@Param("serviceId") Long serviceId);
}
