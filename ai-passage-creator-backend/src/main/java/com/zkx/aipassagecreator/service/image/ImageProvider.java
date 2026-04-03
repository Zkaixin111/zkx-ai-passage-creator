package com.zkx.aipassagecreator.service.image;

import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;

/**
 * 配图方式策略接口
 * <p>
 * 每种配图方式（Pexels / AI 生图 / Mermaid / Iconify / 表情包 / SVG）实现一个策略，
 * 由 {@link ImageProviderRegistry} 统一注册管理，客户端（ImageFetchAgent）按需选择。
 */
public interface ImageProvider {

    /**
     * 本策略对应的配图方式
     */
    ImageMethodEnum method();

    /**
     * 生成单张配图
     *
     * @param requirement 配图需求（position/keywords/sectionTitle/description）
     * @param taskId      任务 ID（用于 COS 目录隔离）
     * @param index       第几张（从 0 开始，用于文件名）
     * @return 配图结果（远程 URL 或直接生成的字节）；生成失败返回 null，由调用方降级
     */
    ImageFetchResult generate(ImageItem requirement, String taskId, int index);
}
