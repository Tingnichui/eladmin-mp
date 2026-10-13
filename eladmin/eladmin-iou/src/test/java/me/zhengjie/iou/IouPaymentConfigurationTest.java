package me.zhengjie.iou;

import me.zhengjie.iou.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import static org.junit.jupiter.api.Assertions.*;

class IouPaymentConfigurationTest {
    @Test void huifuBusinessServiceIsNotRegisteredWithoutExplicitLegacySwitch(){new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(IouFeePaymentService.class).run(context->{assertNull(context.getStartupFailure());assertFalse(context.containsBean("iouFeePaymentService"));});}
    @Test void defaultApplicationYamlBindsWithEncryptedPaymentCredentials() throws Exception {
        StandardEnvironment environment=new StandardEnvironment();
        for(org.springframework.core.env.PropertySource<?> source:new YamlPropertySourceLoader().load("application",
                new FileSystemResource("../eladmin-system/src/main/resources/config/application.yml")))environment.getPropertySources().addFirst(source);
        IouNoteOptions notes=Binder.get(environment).bind("iou.notes",Bindable.of(IouNoteOptions.class)).get();
        IouPaymentOptions payment=Binder.get(environment).bind("iou.payment",Bindable.of(IouPaymentOptions.class)).get();
        assertEquals(new java.math.BigDecimal("0.025"),notes.getServiceFeeRate());assertNull(notes.getServiceFeeMax());assertFalse(payment.ready());assertEquals("ENABLED",notes.getServiceFeePolicy());
        IouNativeWechatOptions nativeOptions=Binder.get(environment).bind("iou.wechat-payment",Bindable.of(IouNativeWechatOptions.class)).get();assertTrue(nativeOptions.isEnabled());assertFalse(nativeOptions.ready()); // Raw binding does not decrypt ENC values.
    }
}
