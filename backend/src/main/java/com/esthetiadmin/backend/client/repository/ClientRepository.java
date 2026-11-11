package com.esthetiadmin.backend.client.repository;

import com.esthetiadmin.backend.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    boolean existsByFirstNameAndLastName(String firstName, String lastName);

    boolean existsByFirstNameAndLastNameAndIdNot(String firstName, String lastName, Long id);

    List<Client> findAllByBirthDateIsNotNull();

    /** Native on purpose: the treatment module must not be a dependency of the client module. */
    @Query(value = "SELECT COUNT(*) FROM treatment WHERE client_id = :clientId", nativeQuery = true)
    long countTreatments(@Param("clientId") Long clientId);

    /** Native for the same reason: the appointment module must not be a dependency of the client module. */
    @Query(value = "SELECT COUNT(*) FROM appointment WHERE client_id = :clientId", nativeQuery = true)
    long countAppointments(@Param("clientId") Long clientId);
}
