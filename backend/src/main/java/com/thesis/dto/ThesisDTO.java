package com.thesis.dto;

import com.thesis.entity.Thesis;
import lombok.Data;

@Data
public class ThesisDTO extends Thesis {
    private String studentName;  // 学生姓名
    private String studentUsername;  // 学号
    private String fileDate;  // 从文件名提取的日期（如 20260326）
}
