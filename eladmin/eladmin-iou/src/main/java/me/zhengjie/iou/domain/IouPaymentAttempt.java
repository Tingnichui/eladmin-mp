package me.zhengjie.iou.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Getter @Setter
@TableName("iou_payment_attempt")
public class IouPaymentAttempt {
    @TableId(type = IdType.AUTO) private Long id;
    private Long feeOrderId;
    private String reqSeqId;
    private String reqDate;
    private String huifuId;
    private String hfSeqId;
    private String status;
    private String payInfo;
    private Timestamp expiresAt;
    private Timestamp checkedAt;
    private Timestamp createTime;
    @Override public String toString() { return "IouPaymentAttempt"; }
}
