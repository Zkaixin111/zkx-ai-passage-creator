package com.zkx.aipassagecreator.service.image;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配图策略产出结果
 * <p>
 * 两类图片来源：
 * <ul>
 *   <li>{@link #url}：远程图片直链（Pexels / Iconify / 表情包 / Picsum），由调用方下载并上传 COS</li>
 *   <li>{@link #data}：本服务直接生成的图片字节（Mermaid / SVG / AI 生图），必须上传 COS 才能展示</li>
 * </ul>
 * 二者至少一个非空。
 */
@Data
@NoArgsConstructor
public class ImageFetchResult {

    /** 远程图片 URL */
    private String url;

    /** 直接生成的图片字节（优先级高于 url） */
    private byte[] data;

    /** 实际使用的配图方式（用于 ImageItem.method，如 PEXELS / PICSUM） */
    private String method;

    /** 建议文件名扩展名（jpg/svg/png），COS 上传用 */
    private String ext;

    public ImageFetchResult(String url, byte[] data, String method, String ext) {
        this.url = url;
        this.data = data;
        this.method = method;
        this.ext = ext;
    }
}
