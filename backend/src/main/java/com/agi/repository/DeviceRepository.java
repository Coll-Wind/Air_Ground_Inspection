package com.agi.repository;

import com.agi.model.Device;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends MongoRepository<Device, String> {

    Optional<Device> findByDeviceCode(String deviceCode);

    List<Device> findByDeviceType(String deviceType);

    List<Device> findByStatus(String status);

    boolean existsByDeviceCode(String deviceCode);
}
