package me.zhengjie;

import me.zhengjie.task.SyncMemberUserTask;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

@ExternalIntegrationTest
public class GymTests {

    @Resource
    private SyncMemberUserTask syncMemberUserTask;

    @Test
    void sync() {
        syncMemberUserTask.sync();
    }
}
