package com.thesis.util;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class DiffUtil {

    public List<DiffResult> compareFiles(String filePath1, String filePath2) throws IOException {
        List<String> original = readFileAsLines(filePath1);
        List<String> revised = readFileAsLines(filePath2);

        Patch<String> patch = DiffUtils.diff(original, revised);
        List<DiffResult> results = new ArrayList<>();

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            DiffResult result = new DiffResult();
            result.setType(delta.getType().toString());
            result.setOriginalPosition(delta.getSource().getPosition());
            result.setOriginalLines(delta.getSource().getLines());
            result.setRevisedPosition(delta.getTarget().getPosition());
            result.setRevisedLines(delta.getTarget().getLines());
            results.add(result);
        }

        return results;
    }

    /**
     * 对比两个文件并返回完整结果（包含原始文档和修订文档的完整内容）
     * 支持图片和表格的富文本内容
     */
    public FullDiffResult compareFilesWithFullContent(String filePath1, String filePath2) throws IOException {
        List<ContentBlock> originalBlocks = readDocxAsContentBlocks(filePath1);
        List<ContentBlock> revisedBlocks = readDocxAsContentBlocks(filePath2);

        // 为了进行文本差异对比，提取纯文本行
        List<String> originalLines = extractTextLines(originalBlocks);
        List<String> revisedLines = extractTextLines(revisedBlocks);

        Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);
        List<DiffResult> diffResults = new ArrayList<>();

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            DiffResult result = new DiffResult();
            result.setType(delta.getType().toString());
            result.setOriginalPosition(delta.getSource().getPosition());
            result.setOriginalLines(delta.getSource().getLines());
            result.setRevisedPosition(delta.getTarget().getPosition());
            result.setRevisedLines(delta.getTarget().getLines());
            diffResults.add(result);
        }

        FullDiffResult fullResult = new FullDiffResult();
        fullResult.setDiffs(diffResults);
        fullResult.setOriginalLines(originalLines);
        fullResult.setRevisedLines(revisedLines);
        fullResult.setOriginalBlocks(originalBlocks);
        fullResult.setRevisedBlocks(revisedBlocks);

        return fullResult;
    }

    /**
     * 从内容块列表中提取纯文本行用于差异对比
     */
    private List<String> extractTextLines(List<ContentBlock> blocks) {
        List<String> lines = new ArrayList<>();
        for (ContentBlock block : blocks) {
            lines.add(block.getSignature());
        }
        return lines;
    }

    /**
     * 读取文件内容为行列表,支持 .txt 和 .docx 格式
     */
    private List<String> readFileAsLines(String filePath) throws IOException {
        if (filePath.toLowerCase().endsWith(".docx")) {
            return readDocxAsLines(filePath);
        } else {
            return Files.readAllLines(Paths.get(filePath));
        }
    }

    /**
     * 读取 DOCX 文件内容为行列表（仅文本，兼容旧版本）
     */
    private List<String> readDocxAsLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    lines.add(text);
                }
            }
        }
        return lines;
    }

    /**
     * 读取 DOCX 文件为富文本内容块列表（包含文本、表格、图片）
     */
    public List<ContentBlock> readDocxAsContentBlocks(String filePath) throws IOException {
        List<ContentBlock> blocks = new ArrayList<>();
        
        if (!filePath.toLowerCase().endsWith(".docx")) {
            // 非 docx 文件，按纯文本处理
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            int index = 0;
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    ContentBlock block = new ContentBlock("TEXT", line, index++);
                    block.setSignature(line);
                    blocks.add(block);
                }
            }
            return blocks;
        }

        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            int blockIndex = 0;
            
            // 按顺序遍历文档中的所有 body 元素
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;

                    // 检查段落中是否包含图片
                    for (XWPFRun run : paragraph.getRuns()) {
                        List<XWPFPicture> pictures = run.getEmbeddedPictures();
                        for (XWPFPicture picture : pictures) {
                            XWPFPictureData pictureData = picture.getPictureData();
                            if (pictureData != null) {
                                byte[] data = pictureData.getData();
                                String base64 = Base64.getEncoder().encodeToString(data);
                                String mimeType = pictureData.getPackagePart().getContentType();
                                String dataUrl = "data:" + mimeType + ";base64," + base64;
                                ContentBlock block = new ContentBlock("IMAGE", dataUrl, blockIndex++);
                                block.setSignature("[IMAGE_" + hash(base64) + "]");
                                blocks.add(block);
                            }
                        }
                    }

                    // 添加段落文本
                    String text = paragraph.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        ContentBlock block = new ContentBlock("TEXT", text, blockIndex++);
                        block.setSignature(text);
                        blocks.add(block);
                    }

                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    int rowCount = table.getNumberOfRows();
                    int colCount = rowCount > 0 ? table.getRow(0).getTableCells().size() : 0;
                    String tableHtml = convertTableToHtml(table);
                    ContentBlock block = new ContentBlock("TABLE", tableHtml, blockIndex++);
                    block.setSignature("[TABLE_" + rowCount + "x" + colCount + "_" + hash(tableHtml) + "]");
                    blocks.add(block);
                }
            }
        }

        return blocks;
    }

    /**
     * 简单的哈希函数，用于生成唯一的签名
     */
    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 将 XWPFTable 转换为 HTML 表格字符串
     */
    private String convertTableToHtml(XWPFTable table) {
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"docx-table\" style=\"border-collapse: collapse; width: 100%;\">");
        
        for (XWPFTableRow row : table.getRows()) {
            html.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                html.append("<td style=\"border: 1px solid #ddd; padding: 8px;\">");
                html.append(escapeHtml(cell.getText()));
                html.append("</td>");
            }
            html.append("</tr>");
        }
        
        html.append("</table>");
        return html.toString();
    }

    /**
     * 转义 HTML 特殊字符
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    // ==================== 数据类 ====================

    public static class ContentBlock {
        private String type;      // TEXT, TABLE, IMAGE
        private String content;   // 文本内容、表格HTML、或图片Base64 Data URL
        private String signature; // 用于差异对比的唯一签名
        private int index;        // 在文档中的顺序

        public ContentBlock() {}

        public ContentBlock(String type, String content, int index) {
            this.type = type;
            this.content = content;
            this.index = index;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
    }

    public static class DiffResult {
        private String type;
        private int originalPosition;
        private List<String> originalLines;
        private int revisedPosition;
        private List<String> revisedLines;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getOriginalPosition() { return originalPosition; }
        public void setOriginalPosition(int originalPosition) { this.originalPosition = originalPosition; }
        public List<String> getOriginalLines() { return originalLines; }
        public void setOriginalLines(List<String> originalLines) { this.originalLines = originalLines; }
        public int getRevisedPosition() { return revisedPosition; }
        public void setRevisedPosition(int revisedPosition) { this.revisedPosition = revisedPosition; }
        public List<String> getRevisedLines() { return revisedLines; }
        public void setRevisedLines(List<String> revisedLines) { this.revisedLines = revisedLines; }
    }

    /**
     * 完整对比结果，包含原始文档和修订文档的完整内容及富文本内容块
     */
    public static class FullDiffResult {
        private List<DiffResult> diffs;
        private List<String> originalLines;
        private List<String> revisedLines;
        private List<ContentBlock> originalBlocks;  // 新增：原始文档内容块
        private List<ContentBlock> revisedBlocks;   // 新增：修订文档内容块
        private String originalFileName;
        private String revisedFileName;

        public List<DiffResult> getDiffs() { return diffs; }
        public void setDiffs(List<DiffResult> diffs) { this.diffs = diffs; }
        public List<String> getOriginalLines() { return originalLines; }
        public void setOriginalLines(List<String> originalLines) { this.originalLines = originalLines; }
        public List<String> getRevisedLines() { return revisedLines; }
        public void setRevisedLines(List<String> revisedLines) { this.revisedLines = revisedLines; }
        public List<ContentBlock> getOriginalBlocks() { return originalBlocks; }
        public void setOriginalBlocks(List<ContentBlock> originalBlocks) { this.originalBlocks = originalBlocks; }
        public List<ContentBlock> getRevisedBlocks() { return revisedBlocks; }
        public void setRevisedBlocks(List<ContentBlock> revisedBlocks) { this.revisedBlocks = revisedBlocks; }
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        public String getRevisedFileName() { return revisedFileName; }
        public void setRevisedFileName(String revisedFileName) { this.revisedFileName = revisedFileName; }
    }
}
