package com.zkx.aipassagecreator.service.image;

import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配图策略注册表（策略模式核心）
 * <p>
 * Spring 自动收集所有 {@link ImageProvider} 实现，按 {@link ImageMethodEnum#getValue()} 建立索引，
 * 客户端无需关心具体策略，只需按方式值取用。
 */
@Component
public class ImageProviderRegistry {

    private final Map<String, ImageProvider> providers = new HashMap<>();

    public ImageProviderRegistry(List<ImageProvider> providerList) {
        for (ImageProvider provider : providerList) {
            providers.put(provider.method().getValue(), provider);
        }
    }

    /**
     * 按配图方式取值获取策略；不存在返回 null
     */
    public ImageProvider get(String methodValue) {
        return providers.get(methodValue);
    }

    /**
     * 默认策略（Pexels，含 Picsum 兜底）
     */
    public ImageProvider getDefault() {
        return providers.get(ImageMethodEnum.PEXELS.getValue());
    }
}
