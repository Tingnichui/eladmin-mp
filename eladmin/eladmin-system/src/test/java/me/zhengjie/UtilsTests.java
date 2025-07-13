package me.zhengjie;

import me.zhengjie.utils.DingdingUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UtilsTests {

    @Resource
    private DingdingUtil dingdingUtil;

    @Test
    void sendMsg() {
        dingdingUtil.sendMsg("test");
    }

}
