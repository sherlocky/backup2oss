package com.sherlocky.back2oss;

import com.qiniu.storage.model.FileInfo;
import com.sherlocky.common.test.BaseJunitTest;
import com.sherlocky.back2oss.service.BackupService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: zhangcx
 * @date: 2019/5/9 10:59
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class Back2ossApplicationTest extends BaseJunitTest {

    @Autowired
    private BackupService backupService;

    @Test
    public void testBackup() {
        backupService.backup();
    }

    @Test
    public void testListBackupFiles() {
        String dateStr = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));

        List<FileInfo> lastMonthFiles = backupService.listBackupFiles(dateStr);

        beautiful(lastMonthFiles);

        lastMonthFiles.sort((a, b) -> a.key.compareTo(b.key));
        String[] needDeleteKeyList = lastMonthFiles.stream().skip(1).map((f) -> {
            return f.key;
        }).collect(Collectors.toList()).toArray(new String[0]);

        beautiful(needDeleteKeyList);
    }

    @Test
    public void testDeleteLastMonthOldFiles() {
        beautiful(backupService.deleteLastMonthOldFiles());
    }
}
