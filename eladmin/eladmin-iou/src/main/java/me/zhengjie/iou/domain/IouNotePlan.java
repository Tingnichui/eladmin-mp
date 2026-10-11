package me.zhengjie.iou.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.JSONWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Timestamp;

/** 约定还款计划，尚未提供用户端写入接口；剩余本金不是实际还款余额。 */
@Data
@TableName("iou_note_plan")
public class IouNotePlan {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteLongAsString)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteLongAsString)
    private Long noteId;
    private Integer periodNo;
    private LocalDate dueDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalAmount;
    private BigDecimal remainingPrincipal;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Timestamp createTime;
}
