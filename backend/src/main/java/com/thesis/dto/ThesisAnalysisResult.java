package com.thesis.dto;

import lombok.Data;
import java.util.List;

/**
 * 论文分析结果 DTO
 * 包含摘要分析、目录分析、参考文献验证、引用检测四大模块
 */
@Data
public class ThesisAnalysisResult {

    /** 分析的论文版本ID */
    private Long versionId;

    // ==================== 摘要分析 ====================

    /** 提取的摘要原文 */
    private String abstractText;

    /** 摘要要素检测结果 */
    private AbstractAnalysis abstractAnalysis;

    /** 预留：LLM 摘要质量评估结果（当前为 null） */
    private String llmAnalysis;

    // ==================== 目录分析 ====================

    /** 章节列表（标题、级别、段落数） */
    private List<ChapterInfo> chapters;

    /** 目录占比分析 */
    private ChapterProportionAnalysis proportionAnalysis;

    // ==================== 参考文献分析 ====================

    /** 提取的参考文献列表 */
    private List<ReferenceInfo> references;

    // ==================== 引用检测 ====================

    /** 未被引用的图/表/文献列表 */
    private List<CitationIssue> citationIssues;

    // ==================== 内部数据类 ====================

    /**
     * 摘要要素检测：研究内容/意义、技术、问题、效果
     */
    @Data
    public static class AbstractAnalysis {
        private boolean hasResearchContent;   // 是否包含研究内容/意义
        private boolean hasTechnology;        // 是否提及采用的技术
        private boolean hasProblem;           // 是否描述解决的问题
        private boolean hasResult;            // 是否说明实际效果
        private String summary;               // 检测总结
    }

    /**
     * 章节信息
     */
    @Data
    public static class ChapterInfo {
        private String title;           // 章节标题
        private int level;              // 层级（1=一级标题, 2=二级, 3=三级）
        private int pageCount;          // 该章节包含的页数
        private double proportion;      // 占比百分比
    }

    /**
     * 目录占比分析
     */
    @Data
    public static class ChapterProportionAnalysis {
        private double designAndImplProportion;  // 设计与实现的合计占比
        private boolean meetsRequirement;        // 是否满足 50% 要求
        private boolean hasRequirements;         // 是否有需求分析/游戏策划
        private boolean hasDesign;               // 是否有设计章节
        private boolean hasImplementation;       // 是否有实现章节
        private boolean hasTesting;              // 是否有测试章节
        private String summary;                  // 分析总结
    }

    /**
     * 参考文献信息
     */
    @Data
    public static class ReferenceInfo {
        private int index;              // 文献编号 [1], [2]...
        private String rawText;         // 原始文本
        private String title;           // 提取的标题
        private String authors;         // 提取的作者
        private String year;            // 提取的年份
        private boolean verified;       // 是否通过验证
        private String verifyDetail;    // 验证详情（匹配/不匹配/未找到）
        private String searchUrl;       // 百度学术搜索链接
        private boolean citedInText;    // 是否在正文中被引用
    }

    /**
     * 引用问题项
     */
    @Data
    public static class CitationIssue {
        private String type;            // FIGURE / TABLE / REFERENCE
        private String identifier;      // "图1"、"表2"、"[3]"
        private String description;     // 问题描述
    }
}
