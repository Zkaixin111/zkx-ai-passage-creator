package com.zkx.aipassagecreator.service.image;

import cn.hutool.core.util.StrUtil;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 表情包配图策略
 * <p>
 * 根据配图关键词匹配预设 emoji 映射表，通过 Twemoji（iconify）渲染为 SVG 直链。
 * 关键词匹配不到时使用默认图片 emoji。
 */
@Component
public class EmojiPackImageProvider implements ImageProvider {

    /** 关键词子串 → emoji 字符（按顺序匹配，先命中先用） */
    private static final Map<String, String> EMOJI_MAP = new LinkedHashMap<>();

    private static final String DEFAULT_EMOJI = "🖼️";

    static {
        EMOJI_MAP.put("tech", "💻");
        EMOJI_MAP.put("code", "💻");
        EMOJI_MAP.put("computer", "💻");
        EMOJI_MAP.put("rocket", "🚀");
        EMOJI_MAP.put("startup", "🚀");
        EMOJI_MAP.put("launch", "🚀");
        EMOJI_MAP.put("ai", "🧠");
        EMOJI_MAP.put("brain", "🧠");
        EMOJI_MAP.put("intelligence", "🧠");
        EMOJI_MAP.put("robot", "🤖");
        EMOJI_MAP.put("heart", "❤️");
        EMOJI_MAP.put("love", "❤️");
        EMOJI_MAP.put("money", "💰");
        EMOJI_MAP.put("finance", "💰");
        EMOJI_MAP.put("economy", "💰");
        EMOJI_MAP.put("food", "🍽️");
        EMOJI_MAP.put("eat", "🍽️");
        EMOJI_MAP.put("health", "🏃");
        EMOJI_MAP.put("sport", "🏃");
        EMOJI_MAP.put("exercise", "🏃");
        EMOJI_MAP.put("book", "📚");
        EMOJI_MAP.put("learn", "📚");
        EMOJI_MAP.put("education", "📚");
        EMOJI_MAP.put("study", "📚");
        EMOJI_MAP.put("chart", "📊");
        EMOJI_MAP.put("data", "📊");
        EMOJI_MAP.put("statistics", "📊");
        EMOJI_MAP.put("environment", "🌱");
        EMOJI_MAP.put("green", "🌱");
        EMOJI_MAP.put("nature", "🌱");
        EMOJI_MAP.put("travel", "✈️");
        EMOJI_MAP.put("plane", "✈️");
        EMOJI_MAP.put("business", "💼");
        EMOJI_MAP.put("work", "💼");
        EMOJI_MAP.put("idea", "💡");
        EMOJI_MAP.put("light", "💡");
        EMOJI_MAP.put("success", "🏆");
        EMOJI_MAP.put("win", "🏆");
        EMOJI_MAP.put("team", "👥");
        EMOJI_MAP.put("people", "👥");
    }

    @Override
    public ImageMethodEnum method() {
        return ImageMethodEnum.EMOJI_PACK;
    }

    @Override
    public ImageFetchResult generate(ImageItem requirement, String taskId, int index) {
        String keywords = requirement.getKeywords() == null ? "" : requirement.getKeywords().toLowerCase(Locale.ROOT);
        String emoji = DEFAULT_EMOJI;
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            if (keywords.contains(entry.getKey())) {
                emoji = entry.getValue();
                break;
            }
        }
        String codepoint = Integer.toHexString(emoji.codePointAt(0));
        String url = ArticleConstant.TWEMOJI_URL.replace("{codepoint}", codepoint);
        return new ImageFetchResult(url, null, ImageMethodEnum.EMOJI_PACK.getValue(), "svg");
    }
}
