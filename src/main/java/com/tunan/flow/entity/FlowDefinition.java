package com.tunan.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "flow_definition" , autoResultMap = true)
public class FlowDefinition {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String name;
    private String description;
    private Integer version;
    private String status; // draft, published, archived

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> definitionJson; // 流程定义JSON

    private LocalDateTime publishedAt;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
