package me.zhengjie.iou.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "iou.wechat")
public class IouWechatOptions {
    private boolean enabled;
    private String appId = "";
    private String appSecret = "";
    private String publicOrigin = "";
    private String imageUrl = "";
    @Override public String toString() { return "IouWechatOptions"; }
}
