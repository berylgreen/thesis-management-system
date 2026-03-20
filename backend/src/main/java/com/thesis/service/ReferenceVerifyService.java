package com.thesis.service;

import com.thesis.dto.ThesisAnalysisResult.ReferenceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参考文献验证服务
 * 通过知网 (CNKI) 搜索，核对参考文献的标题/作者/年份是否真实存在。
 *
 * 验证策略：
 * 1. 使用文献标题构造知网搜索请求
 * 2. 尝试解析返回 HTML，匹配标题/作者/年份
 * 3. 若知网反爬导致请求失败，仍提供搜索链接供教师手动核实
 */
@Service
public class ReferenceVerifyService {

    private static final Logger log = LoggerFactory.getLogger(ReferenceVerifyService.class);

    /** 知网搜索 URL 前缀 */
    private static final String CNKI_SEARCH_URL = "https://kns.cnki.net/kns8s/search?q=";

    /** 请求间隔（毫秒），避免触发反爬 */
    private static final long REQUEST_DELAY_MS = 2000;

    /** 标题匹配相似度阈值 */
    private static final double TITLE_SIMILARITY_THRESHOLD = 0.6;

    /**
     * 批量验证参考文献列表
     * 对每条文献逐一进行知网搜索验证
     */
    public void verifyReferences(List<ReferenceInfo> refs) {
        for (ReferenceInfo ref : refs) {
            try {
                verifyReference(ref);
                Thread.sleep(REQUEST_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("文献验证被中断");
                break;
            } catch (Exception e) {
                log.warn("验证文献[{}]失败: {}", ref.getIndex(), e.getMessage());
                ref.setVerified(false);
                ref.setVerifyDetail("验证失败: " + e.getMessage());
            }
        }
    }

    /**
     * 验证单条参考文献
     */
    private void verifyReference(ReferenceInfo ref) {
        if (ref.getTitle() == null || ref.getTitle().trim().isEmpty()) {
            ref.setVerified(false);
            ref.setVerifyDetail("未能提取文献标题，无法验证");
            return;
        }

        try {
            String searchTitle = ref.getTitle().trim();
            String encoded = URLEncoder.encode(searchTitle, StandardCharsets.UTF_8);
            String searchUrl = CNKI_SEARCH_URL + encoded;
            ref.setSearchUrl(searchUrl);

            String html = fetchUrl(searchUrl);
            if (html == null || html.isEmpty()) {
                // 知网反爬导致请求失败，标记为需人工核实
                ref.setVerified(false);
                ref.setVerifyDetail("知网自动验证失败（可能触发反爬），请点击搜索链接手动核实");
                return;
            }

            // 解析搜索结果
            analyzeSearchResult(ref, html, searchTitle);

        } catch (Exception e) {
            ref.setVerified(false);
            ref.setVerifyDetail("验证异常（请手动核实）: " + e.getMessage());
        }
    }

    /**
     * 分析知网搜索结果，核对标题/作者/年份
     */
    private void analyzeSearchResult(ReferenceInfo ref, String html, String searchTitle) {
        // 检查搜索结果是否为空
        if (html.contains("未找到") || html.contains("没有检索到") || html.contains("0 条结果")
                || html.contains("检索条件无结果")) {
            ref.setVerified(false);
            ref.setVerifyDetail("知网未找到该文献");
            return;
        }

        // 知网搜索结果标题通常在 <a class="fz14"> 或 <td class="name"> 内
        // 多种模式匹配以提高兼容性
        Pattern[] titlePatterns = {
                Pattern.compile("<a[^>]*class=\"fz14\"[^>]*>(.+?)</a>", Pattern.DOTALL),
                Pattern.compile("<td[^>]*class=\"name\"[^>]*>\\s*<a[^>]*>(.+?)</a>", Pattern.DOTALL),
                Pattern.compile("<a[^>]*target=\"_blank\"[^>]*>(.+?)</a>", Pattern.DOTALL)
        };

        boolean titleMatch = false;
        StringBuilder detail = new StringBuilder();
        String resultTitle = null;

        for (Pattern p : titlePatterns) {
            Matcher m = p.matcher(html);
            if (m.find()) {
                resultTitle = m.group(1)
                        .replaceAll("<[^>]+>", "")   // 去掉 HTML 标签
                        .replaceAll("\\s+", " ")      // 规范空白
                        .trim();
                if (!resultTitle.isEmpty() && resultTitle.length() > 2) {
                    break;
                }
            }
        }

        if (resultTitle != null && !resultTitle.isEmpty()) {
            double similarity = calculateSimilarity(searchTitle, resultTitle);
            titleMatch = similarity >= TITLE_SIMILARITY_THRESHOLD;
            detail.append(String.format("标题匹配度: %.0f%%", similarity * 100));

            if (titleMatch) {
                detail.append(" ✓");
            } else {
                detail.append(" ✗（搜索到: ").append(truncate(resultTitle, 40)).append("）");
            }
        } else {
            detail.append("未解析到搜索结果标题（请手动核实）");
        }

        // 检查作者匹配
        if (ref.getAuthors() != null && !ref.getAuthors().isEmpty()) {
            String firstAuthor = ref.getAuthors().split("[,，、;；]")[0].trim();
            if (html.contains(firstAuthor)) {
                detail.append(" | 作者匹配 ✓");
            } else {
                detail.append(" | 作者不匹配 ✗");
            }
        }

        // 检查年份匹配
        if (ref.getYear() != null && !ref.getYear().isEmpty()) {
            if (html.contains(ref.getYear())) {
                detail.append(" | 年份匹配 ✓");
            } else {
                detail.append(" | 年份不匹配 ✗");
            }
        }

        // 判定：标题匹配即认为文献真实存在
        ref.setVerified(titleMatch);
        ref.setVerifyDetail(detail.toString());
    }

    /**
     * HTTP GET 请求获取页面内容
     */
    private String fetchUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            // 模拟正常浏览器请求
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Referer", "https://www.cnki.net/");
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.warn("知网请求返回状态码: {}", responseCode);
                return null;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    content.append(line);
                }
            }

            return content.toString();
        } catch (Exception e) {
            log.warn("请求知网失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算两个字符串的相似度（Jaccard 字符级相似度）
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        s1 = s1.replaceAll("\\s+", "").toLowerCase();
        s2 = s2.replaceAll("\\s+", "").toLowerCase();

        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        Set<Character> set1 = new HashSet<>();
        Set<Character> set2 = new HashSet<>();
        for (char c : s1.toCharArray()) set1.add(c);
        for (char c : s2.toCharArray()) set2.add(c);

        Set<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<Character> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 截断字符串
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
