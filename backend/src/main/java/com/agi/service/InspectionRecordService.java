package com.agi.service;

import com.agi.hdfs.HdfsService;
import com.agi.model.InspectionRecord;
import com.agi.repository.InspectionRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InspectionRecordService {

    @Autowired
    private InspectionRecordRepository recordRepository;
    @Autowired
    private HdfsService hdfsService;

    public List<InspectionRecord> listAll() {
        return recordRepository.findAll();
    }

    public List<InspectionRecord> listByDevice(String deviceCode) {
        return recordRepository.findByDeviceCode(deviceCode);
    }

    public List<InspectionRecord> listByType(String deviceType) {
        return recordRepository.findByDeviceType(deviceType);
    }

    public long count() {
        return recordRepository.count();
    }

    /**
     * 下载 HDFS 上的巡检图片
     */
    public byte[] downloadImage(String hdfsPath) throws Exception {
        return hdfsService.downloadFile(hdfsPath);
    }
}
