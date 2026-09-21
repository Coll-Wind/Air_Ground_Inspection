package com.agi.repository;

import com.agi.model.InspectionTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends MongoRepository<InspectionTask, String> {

    Optional<InspectionTask> findByTaskCode(String taskCode);

    List<InspectionTask> findByDeviceCode(String deviceCode);

    List<InspectionTask> findByStatus(String status);

    // 分页重载
    Page<InspectionTask> findByDeviceCode(String deviceCode, Pageable pageable);

    Page<InspectionTask> findByStatus(String status, Pageable pageable);
}
