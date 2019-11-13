package com.sherlocky.back2oss;

import com.sherlocky.back2oss.task.ScheduledTasks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Backup2ossApplication implements CommandLineRunner {
    @Autowired
    private ScheduledTasks scheduledTasks;
    @Value("${backup.run.on-startup}")
    private boolean runTaskOnStartup;

    public static void main(String[] args) {
        SpringApplication.run(Backup2ossApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (runTaskOnStartup) {
            // 启动时先跑一遍任务
            scheduledTasks.taskBackup();
            scheduledTasks.taskClearOlds();
        }
    }
}
