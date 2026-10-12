package me.zhengjie.iou.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.sql.Timestamp;

/** Internal payment record. Controllers return an explicit public projection only. */
@Getter @Setter
@TableName("iou_fee_order")
public class IouFeeOrder {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderNo;
    private Long noteId;
    private Long borrowerUserId;
    private BigDecimal amount;
    private String status;
    private String feeSnapshot;
    private String payerOpenid;
    private String oauthStateHash;
    private Timestamp oauthExpiresAt;
    private Timestamp oauthConsumedAt;
    private Timestamp paidAt;
    private Timestamp createTime;
    @Override public String toString() { return "IouFeeOrder"; }
}
