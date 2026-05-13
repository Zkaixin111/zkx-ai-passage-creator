package com.zkx.aipassagecreator.service.image;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpRequest;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表情包配图策略（Bing 图片搜索）
 * <p>
 * 通过 Bing 图片搜索获取表情包图片直链（murl，自动追加 "meme" 关键词并过滤大图），
 * 返回远程图 URL（由调用方下载上传 COS）。抓取失败时降级 Twemoji 渲染，保证始终有图。
 */
@Slf4j
@Component
public class EmojiPackImageProvider implements ImageProvider {

    /** Bing 图片搜索地址（qft 过滤大图） */
    private static final String BING_SEARCH_URL =
            "https://www.bing.com/images/search?q={query}&qft=+filterui:imagesize-large&form=IRFLTR";

    /** 浏览器 UA（Bing 无 UA 会返回反爬页面） */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** 页面内 murl（真实图片地址）提取；非贪婪匹配到 &quot; 实体为止，&amp; 等实体由反转义还原 */
    private static final Pattern MURL_PATTERN = Pattern.compile("murl&quot;:&quot;(https?://.*?)&quot;");

    /** 关键词子串 → emoji 字符（降级 Twemoji 用） */
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
        String keywords = StrUtil.blankToDefault(requirement.getKeywords(), "funny");
        try {
            String imageUrl = searchBingMeme(keywords);
            if (StrUtil.isNotBlank(imageUrl)) {
                return new ImageFetchResult(imageUrl, null, ImageMethodEnum.EMOJI_PACK.getValue(), "jpg");
            }
            log.warn("Bing 表情包未搜索到结果: keywords={}", keywords);
        } catch (Exception e) {
            log.warn("Bing 表情包搜索失败: keywords={}, error={}", keywords, e.getMessage());
        }
        // 降级：Twemoji 渲染（关键词匹配 emoji 映射表）
        return twemojiFallback(keywords);
    }

    /**
     * Bing 图片搜索，解析页面内 murl 取第一张真实图片地址
     */
    private String searchBingMeme(String keywords) throws Exception {
        String query = URLEncoder.encode(keywords + " meme", StandardCharsets.UTF_8);
        String url = BING_SEARCH_URL.replace("{query}", query);
        String body = HttpRequest.get(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .setFollowRedirects(true)
                .timeout(10_000)
                .execute()
                .body();
        if (StrUtil.isBlank(body)) {
            return null;
        }
        Matcher matcher = MURL_PATTERN.matcher(body);
        if (matcher.find()) {
            // 反转义 HTML 实体（&amp; 等）与 JSON 转义（\u002f 等）
            String raw = matcher.group(1);
            String unescaped = HtmlUtil.unescape(raw).replace("\\u002f", "/");
            return StrUtil.isBlank(unescaped) ? null : unescaped;
        }
        return null;
    }

    /**
     * 降级：关键词匹配 emoji 映射表，经 Twemoji（iconify）渲染为 SVG
     */
    private ImageFetchResult twemojiFallback(String keywords) {
        String lower = keywords == null ? "" : keywords.toLowerCase(Locale.ROOT);
        String emoji = DEFAULT_EMOJI;
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                emoji = entry.getValue();
                break;
            }
        }
        // 复合 emoji（含变体选择符）需取全部码点并以 "-" 连接，如 ❤️ -> 2764-fe0f
        String codepoint = emoji.codePoints()
                .mapToObj(cp -> Integer.toHexString(cp))
                .reduce((a, b) -> a + "-" + b)
                .orElse("1f5bc");
        String url = ArticleConstant.TWEMOJI_URL.replace("{codepoint}", codepoint);
        return new ImageFetchResult(url, null, ImageMethodEnum.EMOJI_PACK.getValue(), "svg");
    }
}
