package com.agi.hdfs;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * HDFS 文件存储服务 - 巡检图片上传/下载
 */
@Slf4j
@Service
public class HdfsService {

    @Autowired
    private FileSystem fileSystem;

    @Value("${hdfs.uri}")
    private String hdfsUri;

    /**
     * 上传文件到 HDFS
     * @param inputStream 文件输入流
     * @param deviceCode  设备编号
     * @param fileName    文件名
     * @return HDFS 完整路径
     */
    public String uploadFile(InputStream inputStream, String deviceCode, String fileName) throws Exception {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dirPath = "/inspection/images/" + datePath + "/" + deviceCode;
        Path dir = new Path(dirPath);
        if (!fileSystem.exists(dir)) {
            fileSystem.mkdirs(dir);
        }

        String fullPath = dirPath + "/" + System.currentTimeMillis() + "_" + fileName;
        Path dstPath = new Path(fullPath);

        try (FSDataOutputStream out = fileSystem.create(dstPath, true)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.hsync();
        }

        log.info("文件上传 HDFS 成功: {}", fullPath);
        return hdfsUri + fullPath;
    }

    /**
     * 从 HDFS 下载文件
     * @param hdfsPath HDFS 路径(完整 URI 或 /path)
     * @return 文件字节数组
     */
    public byte[] downloadFile(String hdfsPath) throws Exception {
        String path = hdfsPath.startsWith(hdfsUri) ? hdfsPath.substring(hdfsUri.length()) : hdfsPath;
        Path srcPath = new Path(path);

        if (!fileSystem.exists(srcPath)) {
            throw new RuntimeException("HDFS 文件不存在: " + hdfsPath);
        }

        try (FSDataInputStream in = fileSystem.open(srcPath);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    /**
     * 获取 HDFS 上的文件列表
     */
    public boolean exists(String hdfsPath) throws Exception {
        String path = hdfsPath.startsWith(hdfsUri) ? hdfsPath.substring(hdfsUri.length()) : hdfsPath;
        return fileSystem.exists(new Path(path));
    }
}
