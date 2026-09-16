package com.zkx.aipassagecreator.service.image;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.zkx.aipassagecreator.config.CosConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * 腾讯云 COS 上传服务
 * <p>
 * 未配置 COS 时（cosClient 为 null）返回 null，调用方回退使用图片源 URL。
 */
@Slf4j
@Service
public class CosService {

    @Autowired(required = false)
    private COSClient cosClient;

    @Resource
    private CosConfig cosConfig;

    /**
     * 上传字节数据到 COS
     *
     * @param bytes    图片字节
     * @param taskId   任务 ID（用于目录隔离）
     * @param fileName 文件名（含扩展名）
     * @return 可访问的 COS URL；未配置 COS 或上传失败返回 null
     */
    public String upload(byte[] bytes, String taskId, String fileName) {
        if (cosClient == null || !cosConfig.isEnabled()) {
            return null;
        }
        try {
            String key = cosConfig.getPrefix() + "/" + taskId + "/" + fileName;
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            cosClient.putObject(cosConfig.getBucket(), key, new ByteArrayInputStream(bytes), metadata);
            return "https://" + cosConfig.getBucket() + ".cos." + cosConfig.getRegion() + ".myqcloud.com/" + key;
        } catch (Exception e) {
            log.error("COS 上传失败: {}", e.getMessage());
            return null;
        }
    }
}
