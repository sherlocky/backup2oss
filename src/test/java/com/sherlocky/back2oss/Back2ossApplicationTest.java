package com.sherlocky.back2oss;

import com.sherlocky.back2oss.service.BackupService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: zhangcx
 * @date: 2019/5/9 10:59
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class Back2ossApplicationTest {

    @Autowired
    private BackupService backupService;

    @Test
    public void testBackup() {
        backupService.backup();
    }
}
