package com.agi.service;

import com.agi.model.Device;
import com.agi.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    public List<Device> listAll() {
        return deviceRepository.findAll();
    }

    public List<Device> listByType(String type) {
        return deviceRepository.findByDeviceType(type);
    }

    public List<Device> listByStatus(String status) {
        return deviceRepository.findByStatus(status);
    }

    public Optional<Device> getByCode(String code) {
        return deviceRepository.findByDeviceCode(code);
    }

    public Device save(Device device) {
        return deviceRepository.save(device);
    }

    public void deleteByCode(String code) {
        deviceRepository.findByDeviceCode(code).ifPresent(deviceRepository::delete);
    }

    public long count() {
        return deviceRepository.count();
    }

    public long countByStatus(String status) {
        return deviceRepository.findByStatus(status).size();
    }
}
