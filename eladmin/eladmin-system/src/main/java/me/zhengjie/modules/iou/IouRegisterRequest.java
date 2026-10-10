package me.zhengjie.modules.iou;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.*;

/** 只接受注册资料，不接受部门、岗位、角色、状态或系统用户ID。 */
@Getter
@Setter
public class IouRegisterRequest {
    @NotBlank @Size(max = 64) @JSONField(serialize = false)
    private String realName;
    @NotBlank @Size(max = 18) @JSONField(serialize = false)
    private String idNo;
    @NotBlank @Size(max = 2048) @JSONField(serialize = false)
    private String password;
    @NotBlank @Size(max = 16)
    private String code;
    @NotBlank @Size(max = 100)
    private String uuid;
    @NotNull @AssertTrue(message = "请同意用户协议与隐私政策")
    private Boolean agreementAccepted;
    @NotNull @AssertTrue(message = "请单独同意实名核验授权")
    private Boolean identityConsentAccepted;
    @NotBlank @Size(max = 64)
    private String agreementVersion;
}
