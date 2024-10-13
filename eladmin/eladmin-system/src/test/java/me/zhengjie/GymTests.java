package me.zhengjie;

import me.zhengjie.task.SyncMemberUserTask;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GymTests {

    @Resource
    private SyncMemberUserTask syncMemberUserTask;

    @Test
    void sync() {
        syncMemberUserTask.sync();
    }
}
