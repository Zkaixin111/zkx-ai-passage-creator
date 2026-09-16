package com.zkx.aipassagecreator.service.image.search;

/**
 * 图片检索抽象接口
 * <p>
 * 后续可扩展其他实现（Unsplash、AI 生图等），MVP 阶段仅 Pexels + Picsum 兜底。
 */
public interface ImageSearchService {

    /**
     * 根据关键词搜索图片，返回图片直链 URL
     *
     * @param keywords 搜索关键词（建议英文）
     * @return 图片 URL；失败或无结果返回 null
     */
    String search(String keywords);
}
