package com.agi.repository;

import com.agi.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends MongoRepository<Alert, String> {

    List<Alert> findByAlertType(String alertType);

    List<Alert> findByDeviceCode(String deviceCode);

    List<Alert> findByStatus(String status);

    List<Alert> findByLevel(String level);

    // 分页重载
    Page<Alert> findByAlertType(String alertType, Pageable pageable);

    Page<Alert> findByDeviceCode(String deviceCode, Pageable pageable);

    Page<Alert> findByStatus(String status, Pageable pageable);
}
