package com.agi.controller;

import com.agi.model.InspectionRecord;
import com.agi.service.InspectionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/records")
@CrossOrigin(origins = "*")
public class InspectionRecordController {

    @Autowired
    private InspectionRecordService recordService;

    /** 巡检记录分页列表(按上报时间倒序,返回 {list, total}) */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String deviceType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(recordService.page(deviceCode, deviceType, page, size));
    }

    /** 下载 HDFS 巡检图片 */
    @GetMapping("/image")
    public ResponseEntity<byte[]> downloadImage(@RequestParam String path) throws Exception {
        byte[] data = recordService.downloadImage(path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
