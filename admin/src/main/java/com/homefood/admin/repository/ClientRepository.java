package com.homefood.admin.repository;

import com.homefood.admin.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClientRepository extends JpaRepository<Client, Long> {
    java.util.List<Client> findAllByOrderByNameAsc();

    java.util.List<Client> findAllByArchivedFalseOrderByNameAsc();

    java.util.List<Client> findAllByArchivedTrueOrderByNameAsc();

    long countByArchivedTrue();

    @Query("SELECT DISTINCT c.address FROM Client c WHERE c.address IS NOT NULL AND c.address <> ''")
    java.util.List<String> findDistinctAddresses();
}
