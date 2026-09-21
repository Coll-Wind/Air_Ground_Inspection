package com.agi.service;

import com.agi.hdfs.HdfsService;
import com.agi.model.InspectionRecord;
import com.agi.repository.InspectionRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InspectionRecordService {

    @Autowired
    private InspectionRecordRepository recordRepository;
    @Autowired
    private HdfsService hdfsService;

    /** 分页查询巡检记录(按上报时间倒序),支持设备编号/设备类型过滤 */
    public Map<String, Object> page(String deviceCode, String deviceType, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "reportTime"));
        Page<InspectionRecord> p;
        if (deviceCode != null) p = recordRepository.findByDeviceCode(deviceCode, pageable);
        else if (deviceType != null) p = recordRepository.findByDeviceType(deviceType, pageable);
        else p = recordRepository.findAll(pageable);
        return Map.of("list", p.getContent(), "total", p.getTotalElements());
    }

    /** 全量查询(轨迹等内部使用) */
    public List<InspectionRecord> listByDevice(String deviceCode) {
        return recordRepository.findByDeviceCode(deviceCode);
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
