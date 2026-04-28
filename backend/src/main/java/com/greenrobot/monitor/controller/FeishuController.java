package com.greenrobot.monitor.controller;

import com.greenrobot.monitor.service.FeishuNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书通知REST API控制器
 * 
 * 提供飞书Webhook测试接口
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@RestController
@RequestMapping("/feishu")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeishuController {
    
    private final FeishuNotificationService feishuService;
    
    /**
     * 测试飞书Webhook连接
     * 
     * POST /api/feishu/test
     * 
     * @return 测试结果
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testWebhook() {
        try {
            boolean success = feishuService.testWebhook();
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "飞书Webhook测试成功！"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "飞书Webhook未配置或连接失败"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "测试失败: " + e.getMessage()
            ));
        }
    }
}
