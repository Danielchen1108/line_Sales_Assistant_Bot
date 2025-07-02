package com.example.AIrobot.Repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.AIrobot.Entity.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // 根據名稱+createdBy 查詢
    Optional<Customer> findByNameAndCreatedBy(String name, String createdBy);
    List<Customer> findAllByNameAndCreatedBy(String name, String createdBy);
    List<Customer> findAllByCreatedBy(String createdBy);
    List<Customer> findByCreatedByAndPotentialLevel(String createdBy, String potentialLevel);

}
