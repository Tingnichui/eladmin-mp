package me.zhengjie;

import me.zhengjie.other.task.YxtKunInfoTask;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

@ExternalIntegrationTest
public class OtherTests {

    @Resource
    private YxtKunInfoTask yxtInfoTask;

    @Test
    void name() throws InterruptedException {
        yxtInfoTask.syncAll("");
    }

    @Test
    void updateInfo() {
        yxtInfoTask.updateInfo();
    }
}

