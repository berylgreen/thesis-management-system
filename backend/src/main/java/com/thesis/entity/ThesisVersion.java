package com.thesis.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_thesis_version")
public class ThesisVersion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long thesisId;

    private String filePath;

    private String contentHash;

    private Long fileSize;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
