package com.sherlocky.back2oss.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sherlocky.back2oss.service.BackupService;

@Component
public class ScheduledTasks {
	@Autowired 
	private BackupService backupService;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Scheduled(cron = "0 30 3 * * ?") // prod 每天3点半执行
    // @Scheduled(cron="0 0/1 * * * ?") // dev时每分钟触发一次
    public void reportCurrentTime() {
        System.out.println("### 备份文件开始：当前时间：" + dateFormat.format(new Date()));
        backupService.backup();
        System.out.println("### 备份文件结束：" + dateFormat.format(new Date()));
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