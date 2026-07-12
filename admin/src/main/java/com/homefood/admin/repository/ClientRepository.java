package com.homefood.admin.repository;

import com.homefood.admin.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    java.util.List<Client> findAllByOrderByNameAsc();
}
