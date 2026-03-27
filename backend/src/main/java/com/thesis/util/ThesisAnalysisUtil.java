package com.thesis.util;

import com.thesis.dto.ThesisAnalysisResult.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 论文分析工具类
 * 通过 Apache POI 解析 docx，提取摘要/目录/参考文献，并检测引用规范性。
 * 不涉及任何 LLM 调用，所有分析均为纯代码实现。
 */
@Component
public class ThesisAnalysisUtil {

    // ==================== 正则模式 ====================

    /** 摘要起始标记 */
    private static final Pattern ABSTRACT_START = Pattern.compile(
            "^\\s*(摘\\s*要|中文摘要|ABSTRACT|Abstract)\\s*[:：]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** 摘要结束标记 */
    private static final Pattern ABSTRACT_END = Pattern.compile(
            "^\\s*(关\\s*键\\s*词|关键字|Keywords|Key\\s*Words|目\\s*录|CONTENTS)\\s*[:：]?",
            Pattern.CASE_INSENSITIVE);

    /** 一级标题模式（章节号） */
    private static final Pattern CHAPTER_L1 = Pattern.compile(
            "^\\s*(第[一二三四五六七八九十百]+章|第\\d+章|\\d+)\\s+(.+)$");

    /** 二级标题模式 */
    private static final Pattern CHAPTER_L2 = Pattern.compile(
            "^\\s*(\\d+\\.\\d+)\\s+(.+)$");

    /** 三级标题模式 */
    private static final Pattern CHAPTER_L3 = Pattern.compile(
            "^\\s*(\\d+\\.\\d+\\.\\d+)\\s+(.+)$");

    /** 参考文献起始标记 */
    private static final Pattern REF_SECTION_START = Pattern.compile(
            "^\\s*(参\\s*考\\s*文\\s*献|参考资料|References|Bibliography)\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** 参考文献条目模式 [n] */
    private static final Pattern REF_ENTRY = Pattern.compile(
            "^\\s*\\[(\\d+)]\\s*(.+)$");

    /** 图引用模式：图X-X 或 图X.X 或 图X */
    private static final Pattern FIGURE_PATTERN = Pattern.compile(
            "图\\s*(\\d+[-.]?\\d*)");

    /** 表引用模式：表X-X 或 表X.X 或 表X */
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "表\\s*(\\d+[-.]?\\d*)");

    /** 文献引用模式：[n] 或 [n,m] 或 [n-m] */
    private static final Pattern CITATION_PATTERN = Pattern.compile(
            "\\[(\\d+(?:[,，\\-~～]\\d+)*)\\]");

    /** 年份提取 */
    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "((?:19|20)\\d{2})");

    /** 作者提取（中文文献：第一个句号/点前为作者） */
    private static final Pattern CN_AUTHOR_PATTERN = Pattern.compile(
            "^(.+?)[.．。]");

    /** 标题提取（中文文献：第一个句号后到第二个句号或[J]前） */
    private static final Pattern CN_TITLE_PATTERN = Pattern.compile(
            "[.．。]\\s*(.+?)\\s*[.．。\\[]");

    // ==================== 关键字检测（摘要要素） ====================

    /** 研究内容/意义关键词 */
    private static final String[] RESEARCH_KEYWORDS = {
            "研究", "探讨", "分析", "意义", "针对", "面向", "目的", "背景",
            "旨在", "本文", "本论文", "论文", "课题"
    };

    /** 技术关键词 */
    private static final String[] TECH_KEYWORDS = {
            "技术", "方法", "算法", "框架", "架构", "模型", "设计", "开发",
            "采用", "使用", "基于", "利用", "运用", "引入", "实现",
            "Spring", "Vue", "React", "Java", "Python", "MySQL", "深度学习",
            "机器学习", "神经网络", "微服务", "前后端"
    };

    /** 问题关键词 */
    private static final String[] PROBLEM_KEYWORDS = {
            "问题", "挑战", "不足", "缺陷", "瓶颈", "困难", "需求",
            "解决", "改善", "改进", "优化", "克服", "应对"
    };

    /** 效果关键词 */
    private static final String[] RESULT_KEYWORDS = {
            "结果", "效果", "表明", "证明", "验证", "实现了", "达到",
            "提高", "提升", "降低", "减少", "满足", "性能", "功能",
            "测试", "实验", "可行", "有效"
    };

    // ==================== 设计/实现章节关键词 ====================

    private static final String[] DESIGN_KEYWORDS = {
            "设计", "概要设计", "详细设计", "系统设计", "数据库设计",
            "界面设计", "架构设计", "功能设计", "模块设计"
    };

    private static final String[] IMPL_KEYWORDS = {
            "实现", "编码", "开发", "编程", "功能实现", "系统实现",
            "模块实现", "核心实现", "关键代码"
    };

    private static final String[] REQ_KEYWORDS = {
            "需求", "需求分析", "可行性", "游戏策划", "功能分析",
            "业务分析", "系统分析", "用户需求"
    };

    private static final String[] TEST_KEYWORDS = {
            "测试", "测试用例", "功能测试", "性能测试", "系统测试",
            "单元测试", "集成测试", "验收测试", "调试"
    };

    // ==================== 公共方法 ====================

    /**
     * 提取摘要文本
     * 策略：定位"摘要"标题段落，向下读取直到遇到"关键词"/"目录"
     */
    public String extractAbstract(String filePath) throws IOException {
        List<String> paragraphs = readAllParagraphTexts(filePath);
        StringBuilder sb = new StringBuilder();
        boolean inAbstract = false;

        for (String text : paragraphs) {
            if (!inAbstract) {
                if (ABSTRACT_START.matcher(text).matches()) {
                    inAbstract = true;
                    continue;
                }
                // 也检查摘要标题和正文在同一段落的情况
                Matcher m = Pattern.compile(
                        "^\\s*(摘\\s*要|中文摘要)\\s*[:：]\\s*(.+)$").matcher(text);
                if (m.matches()) {
                    sb.append(m.group(2));
                    inAbstract = true;
                    continue;
                }
            } else {
                if (ABSTRACT_END.matcher(text).find()) {
                    break;
                }
                if (sb.length() > 0) sb.append("\n");
                sb.append(text);
            }
        }
        return sb.toString().trim();
    }

    /**
     * 分析摘要要素
     */
    public AbstractAnalysis analyzeAbstract(String abstractText) {
        AbstractAnalysis analysis = new AbstractAnalysis();
        if (abstractText == null || abstractText.isEmpty()) {
            analysis.setSummary("未检测到摘要内容");
            return analysis;
        }

        analysis.setHasResearchContent(containsAny(abstractText, RESEARCH_KEYWORDS));
        analysis.setHasTechnology(containsAny(abstractText, TECH_KEYWORDS));
        analysis.setHasProblem(containsAny(abstractText, PROBLEM_KEYWORDS));
        analysis.setHasResult(containsAny(abstractText, RESULT_KEYWORDS));

        // 生成总结
        List<String> missing = new ArrayList<>();
        if (!analysis.isHasResearchContent()) missing.add("研究内容/意义");
        if (!analysis.isHasTechnology()) missing.add("采用的技术");
        if (!analysis.isHasProblem()) missing.add("解决的问题");
        if (!analysis.isHasResult()) missing.add("实际效果");

        if (missing.isEmpty()) {
            analysis.setSummary("摘要要素完整，包含研究内容、技术、问题、效果四个方面");
        } else {
            analysis.setSummary("摘要缺少以下要素：" + String.join("、", missing));
        }

        return analysis;
    }

    /**
     * 提取目录结构
     * 三重策略：优先读取 Heading 样式，回退到目录区域解析，最后正则匹配
     */
    public List<ChapterInfo> extractTableOfContents(String filePath) throws IOException {
        // 策略1：基于 Word Heading 样式
        List<ChapterInfo> chapters = extractByHeadingStyle(filePath);

        // 策略2：从文档的"目录"区域解析（适用于自定义样式的文档）
        // 当 Heading 提取无一级标题时触发
        boolean hasL1 = chapters.stream().anyMatch(c -> c.getLevel() == 1);
        if (!hasL1) {
            List<ChapterInfo> tocChapters = extractFromTocSection(filePath);
            if (!tocChapters.isEmpty()) {
                chapters = tocChapters;
            }
        }

        // 策略3：回退到正则匹配
        hasL1 = chapters.stream().anyMatch(c -> c.getLevel() == 1);
        if (!hasL1) {
            chapters = extractByRegex(filePath);
        }

        return chapters;
    }

    /**
     * 分析章节占比
     * 计算每个一级章节的段落数占比，检测设计与实现是否≥50%
     */
    public ChapterProportionAnalysis analyzeChapterProportions(
            List<ChapterInfo> chapters, String filePath) throws IOException {

        ChapterProportionAnalysis analysis = new ChapterProportionAnalysis();
        if (chapters.isEmpty()) {
            analysis.setSummary("未检测到有效的章节结构");
            return analysis;
        }

        // 统计页数
        assignPageCounts(chapters, filePath);

        int totalPages = chapters.stream()
                .filter(c -> c.getLevel() == 1)
                .mapToInt(ChapterInfo::getPageCount)
                .sum();

        if (totalPages == 0) totalPages = 1; // 防止除零

        // 计算占比
        for (ChapterInfo ch : chapters) {
            if (ch.getLevel() == 1) {
                ch.setProportion(Math.round(ch.getPageCount() * 10000.0 / totalPages) / 100.0);
            }
        }

        // 检测结构完整性和占比
        double designImplProp = 0;
        boolean hasReq = false, hasDesign = false, hasImpl = false, hasTest = false;

        for (ChapterInfo ch : chapters) {
            if (ch.getLevel() != 1) continue;
            String title = ch.getTitle();

            if (containsAny(title, REQ_KEYWORDS)) {
                hasReq = true;
            }
            if (containsAny(title, DESIGN_KEYWORDS)) {
                hasDesign = true;
                designImplProp += ch.getProportion();
            }
            if (containsAny(title, IMPL_KEYWORDS)) {
                hasImpl = true;
                designImplProp += ch.getProportion();
            }
            // 同时包含设计和实现的章节
            if (containsAny(title, DESIGN_KEYWORDS) && containsAny(title, IMPL_KEYWORDS)) {
                // 已经加了两次，减去一次
                designImplProp -= ch.getProportion();
            }
            if (containsAny(title, TEST_KEYWORDS)) {
                hasTest = true;
            }
        }

        analysis.setDesignAndImplProportion(designImplProp);
        analysis.setMeetsRequirement(designImplProp >= 50.0);
        analysis.setHasRequirements(hasReq);
        analysis.setHasDesign(hasDesign);
        analysis.setHasImplementation(hasImpl);
        analysis.setHasTesting(hasTest);

        // 生成总结
        StringBuilder summary = new StringBuilder();
        List<String> missingChapters = new ArrayList<>();
        if (!hasReq) missingChapters.add("需求分析/游戏策划");
        if (!hasDesign) missingChapters.add("设计");
        if (!hasImpl) missingChapters.add("实现");
        if (!hasTest) missingChapters.add("测试");

        if (!missingChapters.isEmpty()) {
            summary.append("缺少章节：").append(String.join("、", missingChapters)).append("。");
        }

        summary.append(String.format("设计与实现合计占比 %.1f%%", designImplProp));
        if (designImplProp >= 50.0) {
            summary.append("，满足≥50%的要求。");
        } else {
            summary.append(String.format("，未达到50%%的要求（差%.1f%%）。", 50.0 - designImplProp));
        }

        analysis.setSummary(summary.toString());
        return analysis;
    }

    /**
     * 提取参考文献列表
     */
    public List<ReferenceInfo> extractReferences(String filePath) throws IOException {
        List<String> paragraphs = readAllParagraphTexts(filePath);
        List<ReferenceInfo> refs = new ArrayList<>();
        boolean inRefSection = false;

        for (String text : paragraphs) {
            if (!inRefSection) {
                if (REF_SECTION_START.matcher(text).matches()) {
                    inRefSection = true;
                }
                continue;
            }

            // 遇到附录/致谢等结束参考文献区域
            if (text.matches("^\\s*(附\\s*录|致\\s*谢|致谢辞|APPENDIX|Acknowledgment).*$")) {
                break;
            }

            Matcher m = REF_ENTRY.matcher(text);
            if (m.matches()) {
                ReferenceInfo ref = new ReferenceInfo();
                ref.setIndex(Integer.parseInt(m.group(1)));
                ref.setRawText(m.group(2).trim());
                parseReferenceDetails(ref);
                refs.add(ref);
            } else if (!refs.isEmpty() && !text.trim().isEmpty()) {
                // 可能是上一条文献的续行
                ReferenceInfo lastRef = refs.get(refs.size() - 1);
                lastRef.setRawText(lastRef.getRawText() + " " + text.trim());
                parseReferenceDetails(lastRef);
            }
        }

        return refs;
    }

    /**
     * 检测图/表/文献引用
     * 找出文档中定义的所有图/表编号及参考文献编号，检查正文是否引用了它们
     */
    public List<CitationIssue> detectCitationIssues(String filePath,
                                                     List<ReferenceInfo> refs) throws IOException {
        List<String> paragraphs = readAllParagraphTexts(filePath);
        String fullText = String.join("\n", paragraphs);
        List<CitationIssue> issues = new ArrayList<>();

        // --- 检测图引用 ---
        Set<String> definedFigures = new LinkedHashSet<>();
        Set<String> citedFigures = new HashSet<>();

        for (String para : paragraphs) {
            Matcher m = FIGURE_PATTERN.matcher(para);
            while (m.find()) {
                String figId = m.group(1).trim();
                if (!figId.isEmpty()) {
                    // 判断是定义还是引用：如果在"如图"/"见图"/"图X所示"等上下文中是引用
                    // 简化处理：出现过2次以上视为已引用；只出现1次可能只是定义
                    definedFigures.add(figId);
                }
            }
        }

        // 统计每个图号出现次数
        for (String figId : definedFigures) {
            Pattern p = Pattern.compile("图\\s*" + Pattern.quote(figId));
            Matcher m = p.matcher(fullText);
            int count = 0;
            while (m.find()) count++;
            if (count >= 2) citedFigures.add(figId);
        }

        for (String figId : definedFigures) {
            if (!citedFigures.contains(figId)) {
                CitationIssue issue = new CitationIssue();
                issue.setType("FIGURE");
                issue.setIdentifier("图" + figId);
                issue.setDescription("图" + figId + " 在正文中未被引用（仅出现1次，可能未在上下文中引用）");
                issues.add(issue);
            }
        }

        // --- 检测表引用 ---
        Set<String> definedTables = new LinkedHashSet<>();
        Set<String> citedTables = new HashSet<>();

        for (String para : paragraphs) {
            Matcher m = TABLE_PATTERN.matcher(para);
            while (m.find()) {
                String tblId = m.group(1).trim();
                if (!tblId.isEmpty()) {
                    definedTables.add(tblId);
                }
            }
        }

        for (String tblId : definedTables) {
            Pattern p = Pattern.compile("表\\s*" + Pattern.quote(tblId));
            Matcher m = p.matcher(fullText);
            int count = 0;
            while (m.find()) count++;
            if (count >= 2) citedTables.add(tblId);
        }

        for (String tblId : definedTables) {
            if (!citedTables.contains(tblId)) {
                CitationIssue issue = new CitationIssue();
                issue.setType("TABLE");
                issue.setIdentifier("表" + tblId);
                issue.setDescription("表" + tblId + " 在正文中未被引用（仅出现1次，可能未在上下文中引用）");
                issues.add(issue);
            }
        }

        // --- 检测文献引用 ---
        Set<Integer> citedRefs = new HashSet<>();
        Matcher cm = CITATION_PATTERN.matcher(fullText);
        while (cm.find()) {
            String group = cm.group(1);
            // 处理 [1,2,3] 和 [1-3] 格式
            for (String part : group.split("[,，]")) {
                part = part.trim();
                if (part.contains("-") || part.contains("~") || part.contains("～")) {
                    String[] range = part.split("[-~～]");
                    if (range.length == 2) {
                        try {
                            int start = Integer.parseInt(range[0].trim());
                            int end = Integer.parseInt(range[1].trim());
                            for (int i = start; i <= end; i++) citedRefs.add(i);
                        } catch (NumberFormatException ignored) {}
                    }
                } else {
                    try {
                        citedRefs.add(Integer.parseInt(part));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        for (ReferenceInfo ref : refs) {
            boolean cited = citedRefs.contains(ref.getIndex());
            ref.setCitedInText(cited);
            if (!cited) {
                CitationIssue issue = new CitationIssue();
                issue.setType("REFERENCE");
                issue.setIdentifier("[" + ref.getIndex() + "]");
                issue.setDescription("参考文献[" + ref.getIndex() + "] 在正文中未被引用");
                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * 构造知网搜索 URL
     */
    public String buildCnkiSearchUrl(String title) {
        if (title == null || title.trim().isEmpty()) return null;
        String encoded = URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
        return "https://kns.cnki.net/kns8s/search?q=" + encoded;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 读取 docx 中所有段落的纯文本
     */
    private List<String> readAllParagraphTexts(String filePath) throws IOException {
        List<String> texts = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.trim().isEmpty()) {
                    texts.add(text.trim());
                }
            }
        }
        return texts;
    }

    /**
     * 基于 Heading 样式提取目录
     */
    private List<ChapterInfo> extractByHeadingStyle(String filePath) throws IOException {
        List<ChapterInfo> chapters = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                String style = para.getStyle();
                String text = para.getText();
                if (style == null || text == null || text.trim().isEmpty()) continue;

                int level = -1;
                String normalizedStyle = style.toLowerCase();
                if (normalizedStyle.contains("heading1") || "1".equals(style) || normalizedStyle.equals("heading 1")) {
                    level = 1;
                } else if (normalizedStyle.contains("heading2") || "2".equals(style) || normalizedStyle.equals("heading 2")) {
                    level = 2;
                } else if (normalizedStyle.contains("heading3") || "3".equals(style) || normalizedStyle.equals("heading 3")) {
                    level = 3;
                }

                if (level > 0) {
                    ChapterInfo ch = new ChapterInfo();
                    ch.setTitle(text.trim());
                    ch.setLevel(level);
                    chapters.add(ch);
                }
            }
        }
        return chapters;
    }

    /**
     * 基于正则模式提取目录（回退策略）
     */
    private List<ChapterInfo> extractByRegex(String filePath) throws IOException {
        List<String> paragraphs = readAllParagraphTexts(filePath);
        List<ChapterInfo> chapters = new ArrayList<>();

        for (String text : paragraphs) {
            // 优先匹配三级标题（更具体）
            Matcher m3 = CHAPTER_L3.matcher(text);
            if (m3.matches()) {
                ChapterInfo ch = new ChapterInfo();
                ch.setTitle(text.trim());
                ch.setLevel(3);
                chapters.add(ch);
                continue;
            }

            Matcher m2 = CHAPTER_L2.matcher(text);
            if (m2.matches()) {
                ChapterInfo ch = new ChapterInfo();
                ch.setTitle(text.trim());
                ch.setLevel(2);
                chapters.add(ch);
                continue;
            }

            Matcher m1 = CHAPTER_L1.matcher(text);
            if (m1.matches()) {
                ChapterInfo ch = new ChapterInfo();
                ch.setTitle(text.trim());
                ch.setLevel(1);
                chapters.add(ch);
            }
        }

        return chapters;
    }

    /**
     * 从文档的"目录"区域提取章节结构（策略2）。
     * 适用于未使用标准 Heading 样式但有目录页的论文。
     * 解析目录条目的标题和级别，去掉末尾页码。
     */
    private List<ChapterInfo> extractFromTocSection(String filePath) throws IOException {
        List<ChapterInfo> chapters = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {

            boolean inToc = false;

            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text == null || text.trim().isEmpty()) continue;
                String trimmed = text.trim();

                if (!inToc) {
                    if (TOC_START.matcher(trimmed).matches()) {
                        inToc = true;
                    }
                    continue;
                }

                // 目录结束判定：遇到不含末尾页码且匹配章节标题正则的段落（正文标题）
                if (!TOC_PAGE_NUMBER.matcher(trimmed).find()) {
                    if (CHAPTER_L1.matcher(trimmed).matches()) {
                        break;
                    }
                    continue;
                }

                // 提取末尾页码并去掉
                Matcher pm = TOC_PAGE_NUMBER.matcher(trimmed);
                if (!pm.find()) continue;

                String titlePart = trimmed.substring(0, pm.start()).trim();
                titlePart = titlePart.replaceAll("[\\t.·…]+$", "").trim();
                if (titlePart.isEmpty()) continue;

                // 判断级别
                int level = -1;
                if (CHAPTER_L3.matcher(titlePart).matches()) {
                    level = 3;
                } else if (CHAPTER_L2.matcher(titlePart).matches()) {
                    level = 2;
                } else if (CHAPTER_L1.matcher(titlePart).matches()) {
                    level = 1;
                }

                if (level > 0) {
                    ChapterInfo ch = new ChapterInfo();
                    ch.setTitle(titlePart);
                    ch.setLevel(level);
                    chapters.add(ch);
                }
            }
        }
        return chapters;
    }

    /**
     * 为一级章节分配页数
     * 从文档的"目录"区域提取每个一级标题对应的页码，
     * 计算两个一级标题之间跨越的页数。
     */
    private void assignPageCounts(List<ChapterInfo> chapters, String filePath) throws IOException {
        List<ChapterInfo> l1Chapters = chapters.stream()
                .filter(c -> c.getLevel() == 1)
                .collect(Collectors.toList());

        if (l1Chapters.isEmpty()) return;

        // 从目录区域提取一级标题及其页码
        Map<String, Integer> tocPageNumbers = extractTocPageNumbers(filePath);

        if (!tocPageNumbers.isEmpty()) {
            // 匹配章节标题到目录页码
            List<Integer> pageNumbers = new ArrayList<>();
            for (ChapterInfo ch : l1Chapters) {
                Integer pageNum = findMatchingPageNumber(ch.getTitle(), tocPageNumbers);
                pageNumbers.add(pageNum != null ? pageNum : 0);
            }

            // 计算每章页数 = 下一章起始页 - 本章起始页
            for (int i = 0; i < l1Chapters.size(); i++) {
                int startPage = pageNumbers.get(i);
                if (startPage == 0) {
                    l1Chapters.get(i).setPageCount(1);
                    continue;
                }
                int endPage = 0;
                for (int j = i + 1; j < pageNumbers.size(); j++) {
                    if (pageNumbers.get(j) > 0) {
                        endPage = pageNumbers.get(j);
                        break;
                    }
                }
                if (endPage > startPage) {
                    l1Chapters.get(i).setPageCount(endPage - startPage);
                } else {
                    l1Chapters.get(i).setPageCount(1);
                }
            }
        } else {
            for (ChapterInfo ch : l1Chapters) {
                ch.setPageCount(1);
            }
        }
    }

    /** 目录区域起始标记 */
    private static final Pattern TOC_START = Pattern.compile(
            "^\\s*(目\\s*录|CONTENTS)\\s*$", Pattern.CASE_INSENSITIVE);

    /** 从目录段落末尾提取页码 */
    private static final Pattern TOC_PAGE_NUMBER = Pattern.compile("(\\d+)\\s*$");

    /**
     * 解析文档中的"目录"区域，提取一级条目的标题→页码映射。
     * 目录条目的 getText() 形如 "1 绪论\t\t1" 或 "第一章 绪论\t\t12"，
     * 末尾数字即为页码。
     */
    private Map<String, Integer> extractTocPageNumbers(String filePath) throws IOException {
        Map<String, Integer> result = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {

            boolean inToc = false;

            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text == null || text.trim().isEmpty()) continue;
                String trimmed = text.trim();

                if (!inToc) {
                    if (TOC_START.matcher(trimmed).matches()) {
                        inToc = true;
                    }
                    continue;
                }

                // 目录结束判定：遇到正文 Heading 样式的章节标题
                String style = para.getStyle();
                if (style != null) {
                    String ls = style.toLowerCase();
                    boolean isHeadingStyle = ls.contains("heading") || "1".equals(style) || "2".equals(style);
                    if (isHeadingStyle && CHAPTER_L1.matcher(trimmed).matches()) {
                        break;
                    }
                }

                // 提取末尾页码
                Matcher pm = TOC_PAGE_NUMBER.matcher(trimmed);
                if (!pm.find()) continue;

                int pageNum = Integer.parseInt(pm.group(1));

                // 去掉末尾页码、tab、leader dots，保留标题部分
                String titlePart = trimmed.substring(0, pm.start()).trim();
                titlePart = titlePart.replaceAll("[\\t.·…]+$", "").trim();

                if (!titlePart.isEmpty() && CHAPTER_L1.matcher(titlePart).matches()) {
                    result.put(titlePart, pageNum);
                }
            }
        }
        return result;
    }

    /**
     * 根据标题文本在目录页码映射中查找匹配项。
     */
    private Integer findMatchingPageNumber(String chapterTitle, Map<String, Integer> tocPageNumbers) {
        if (chapterTitle == null) return null;
        String clean = chapterTitle.trim();

        // 精确匹配
        if (tocPageNumbers.containsKey(clean)) {
            return tocPageNumbers.get(clean);
        }

        // 包含匹配
        for (Map.Entry<String, Integer> entry : tocPageNumbers.entrySet()) {
            if (entry.getKey().contains(clean) || clean.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 解析参考文献条目，提取标题、作者、年份
     */
    private void parseReferenceDetails(ReferenceInfo ref) {
        String raw = ref.getRawText();

        // 提取年份
        Matcher ym = YEAR_PATTERN.matcher(raw);
        if (ym.find()) {
            ref.setYear(ym.group(1));
        }

        // 提取作者（中文文献：第一个句号前）
        Matcher am = CN_AUTHOR_PATTERN.matcher(raw);
        if (am.find()) {
            ref.setAuthors(am.group(1).trim());
        }

        // 提取标题（中文文献：第一个句号后到下一个句号或 [J]/[M] 前）
        Matcher tm = CN_TITLE_PATTERN.matcher(raw);
        if (tm.find()) {
            ref.setTitle(tm.group(1).trim());
        } else if (ref.getAuthors() != null) {
            // 回退：去掉作者后的第一个非空段
            String remaining = raw.substring(ref.getAuthors().length()).trim();
            if (remaining.startsWith(".") || remaining.startsWith("．") || remaining.startsWith("。")) {
                remaining = remaining.substring(1).trim();
            }
            // 截取到下一个分隔符
            int endIdx = remaining.length();
            for (String sep : new String[]{"[J]", "[M]", "[D]", "[C]", "[R]", "[S]", "[P]", ".", "．"}) {
                int idx = remaining.indexOf(sep);
                if (idx > 0 && idx < endIdx) endIdx = idx;
            }
            ref.setTitle(remaining.substring(0, endIdx).trim());
        }

        // 生成搜索 URL
        if (ref.getTitle() != null && !ref.getTitle().isEmpty()) {
            ref.setSearchUrl(buildCnkiSearchUrl(ref.getTitle()));
        }
    }

    /**
     * 检查文本是否包含关键词数组中的任一词
     */
    private boolean containsAny(String text, String[] keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
