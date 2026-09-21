package com.agi.repository;

import com.agi.model.InspectionTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends MongoRepository<InspectionTask, String> {

    Optional<InspectionTask> findByTaskCode(String taskCode);

    List<InspectionTask> findByDeviceCode(String deviceCode);

    List<InspectionTask> findByStatus(String status);
}
