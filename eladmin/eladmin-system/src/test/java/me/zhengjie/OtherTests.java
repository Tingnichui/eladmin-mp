package me.zhengjie;

import me.zhengjie.other.task.YxtKunInfoTask;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OtherTests {

    @Resource
    private YxtKunInfoTask yxtInfoTask;

    @Test
    void name() throws InterruptedException {
        yxtInfoTask.syncAll("");
    }
}

