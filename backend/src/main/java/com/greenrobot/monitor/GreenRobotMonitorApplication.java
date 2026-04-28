package com.greenrobot.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 绿萝监测系统 - Java后端
 * 
 * 办公室绿萝 Pro 核心系统
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@SpringBootApplication
@EnableScheduling  // 启用定时任务
public class GreenRobotMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenRobotMonitorApplication.class, args);
    }
}
