package com.sherlocky.back2oss.task;

import com.alibaba.fastjson.JSON;
import com.sherlocky.back2oss.service.BackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
public class ScheduledTasks {
	@Autowired
	private BackupService backupService;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * 如果需要并行执行任务，使用 @Async 注解，并且在主方法上使用 @EnableAsync 注解。
     */	
	@Scheduled(cron = "0 30 3 * * ?") // prod 每天凌晨3点半执行
    // @Scheduled(cron="0 0/1 * * * ?") // dev时每分钟触发一次
    public void taskBackup() {
	    log.info("### 备份文件开始：当前时间：" + dateFormat.format(new Date()));
        backupService.backup();
        log.info("### 备份文件结束：" + dateFormat.format(new Date()));
    }

    // 每月1号凌晨3点40执行（删除任务晚于备份任务）
    @Scheduled(cron = "0 40 3 1 * ?")
    public void taskClearOlds() {
        log.info("### 清理上月过期备份文件开始：当前时间：" + dateFormat.format(new Date()));
        String[] needDeleteFiles = backupService.deleteLastMonthOldFiles();
        log.info(JSON.toJSONString(needDeleteFiles));
        log.info("### 清理上月过期备份文件结束：" + dateFormat.format(new Date()));
    }
    
/*	@Scheduled(fixedRate = 1000 * 3)
	public void reportCurrentTime() {
		System.out.println("Scheduling Tasks Examples: The time is now " + dateFormat().format(new Date()));
	}

	// 每1分钟执行一次
	@Scheduled(cron = "0 * /1 *  * * * ")
	public void reportCurrentByCron() {
		System.out.println("Scheduling Tasks Examples By Cron: The time is now " + dateFormat().format(new Date()));
	}

	private SimpleDateFormat dateFormat() {
		return new SimpleDateFormat("HH:mm:ss");
	}*/
}