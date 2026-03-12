package com.zkx.aipassagecreator.config;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 配置
 * <p>
 * 未配置 SecretId/SecretKey 时不创建 COSClient Bean，CosService 通过 @Autowired(required=false) 注入 null，直接使用图片源 URL。
 */
@Slf4j
@Configuration
public class CosConfig {

    @Value("${article.cos.secret-id:}")
    private String secretId;

    @Value("${article.cos.secret-key:}")
    private String secretKey;

    @Value("${article.cos.region:ap-guangzhou}")
    private String region;

    @Value("${article.cos.bucket:}")
    private String bucket;

    @Value("${article.cos.prefix:article}")
    private String prefix;

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${article.cos.secret-id:}') "
            + "&& T(org.springframework.util.StringUtils).hasText('${article.cos.secret-key:}')")
    public COSClient cosClient() {
        log.info("COS 配置检测到 SecretId/SecretKey，创建 COSClient（region={}）", region);
        BasicCOSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(credentials, clientConfig);
    }

    public String getBucket() {
        return bucket;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isEnabled() {
        return StrUtil.isNotBlank(secretId) && StrUtil.isNotBlank(secretKey) && StrUtil.isNotBlank(bucket);
    }

    public String getRegion() {
        return region;
    }
}
