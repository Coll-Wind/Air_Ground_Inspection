package com.agi.repository;

import com.agi.model.InspectionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionRecordRepository extends MongoRepository<InspectionRecord, String> {

    List<InspectionRecord> findByDeviceCode(String deviceCode);

    List<InspectionRecord> findByDeviceType(String deviceType);
}
