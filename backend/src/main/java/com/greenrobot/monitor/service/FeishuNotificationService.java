package com.greenrobot.monitor.service;

import com.greenrobot.monitor.config.LeafConfig;
import com.greenrobot.monitor.entity.ConfirmedLeaf;
import com.greenrobot.monitor.entity.HealthStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 飞书通知服务
 * 
 * 负责向飞书群发送叶片预警通知
 * 使用飞书机器人Webhook接口
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
@Slf4j
@Service
public class FeishuNotificationService {

    /** HTTP客户端，用于发送Webhook请求 */
    private final HttpClient httpClient;
    
    /** 飞书Webhook地址 */
    private final String webhookUrl;
    
    /** 日期时间格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public FeishuNotificationService(
            @Value("${feishu.webhook.url:#{null}}") String webhookUrl) {
        // 初始化HTTP客户端，设置超时时间
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.webhookUrl = webhookUrl;
    }

    /**
     * 发送叶片预警通知
     * 
     * 检查冷却时间，避免同一叶片重复预警
     * 
     * @param leaf 叶片档案
     * @param observation 最新观测记录
     * @return true表示发送成功，false表示被冷却或配置缺失
     */
    public boolean sendLeafAlert(ConfirmedLeaf leaf, com.greenrobot.monitor.entity.LeafObservation observation) {
        // ========== 1. 检查Webhook配置 ==========
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("飞书Webhook未配置，跳过通知");
            return false;
        }
        
        // ========== 2. 检查冷却时间 ==========
        // 同一叶片在冷却期内不重复发送
        if (isInCooldown(leaf)) {
            log.info("叶片{}处于冷却期({}小时)，跳过通知", 
                    leaf.getLeafId(), LeafConfig.ALERT_COOLDOWN_HOURS);
            return false;
        }
        
        // ========== 3. 构建消息内容 ==========
        String alertContent = buildAlertContent(leaf, observation);
        
        // ========== 4. 发送飞书消息 ==========
        boolean success = sendFeishuMessage(alertContent);
        
        if (success) {
            log.info("成功发送叶片预警: {}", leaf.getLeafId());
        }
        
        return success;
    }

    /**
     * 检查叶片是否处于预警冷却期
     * 
     * 冷却机制：同一叶片上次预警后，4小时内不重复预警
     * 
     * @param leaf 叶片档案
     * @return true表示在冷却期内
     */
    private boolean isInCooldown(ConfirmedLeaf leaf) {
        if (leaf.getLastAlertTime() == null) {
            // 从未预警过，不在冷却期
            return false;
        }
        
        LocalDateTime cooldownEnd = leaf.getLastAlertTime()
                .plusHours(LeafConfig.ALERT_COOLDOWN_HOURS);
        
        return LocalDateTime.now().isBefore(cooldownEnd);
    }

    /**
     * 构建飞书预警消息内容
     * 
     * 使用飞书卡片消息格式
     * 
     * @param leaf 叶片档案
     * @param observation 观测记录
     * @return JSON格式的消息内容
     */
    private String buildAlertContent(ConfirmedLeaf leaf, 
                                     com.greenrobot.monitor.entity.LeafObservation observation) {
        // 根据健康状态选择emoji和标题
        String emoji = getEmojiForStatus(leaf.getHealthStatus());
        String title = emoji + " 叶片" + leaf.getLeafId() + "预警";
        
        // 获取状态描述
        HealthStatus status = HealthStatus.valueOf(leaf.getHealthStatus());
        String statusDesc = status.getDescription();
        
        // 格式化时间
        String timeStr = observation.getObservationTime().format(DATE_FORMATTER);
        
        // 构建卡片消息JSON
        return String.format("""
            {
                "msg_type": "interactive",
                "card": {
                    "header": {
                        "title": {
                            "tag": "plain_text",
                            "content": "%s"
                        },
                        "template": "%s"
                    },
                    "elements": [
                        {
                            "tag": "div",
                            "text": {
                                "tag": "lark_md",
                                "content": "**状态**: %s\\n**描述**: %s"
                            }
                        },
                        {
                            "tag": "div",
                            "text": {
                                "tag": "lark_md",
                                "content": "**指标数据**\\n• 面积: %.0f\\n• H值: %.1f\\n• S值: %.1f\\n• V值: %.1f"
                            }
                        },
                        {
                            "tag": "div",
                            "text": {
                                "tag": "lark_md",
                                "content": "**时间**: %s"
                            }
                        }
                    ]
                }
            }
            """,
            title,
            getTemplateColor(leaf.getHealthStatus()),
            status.name(), // 使用name而不是displayName
            statusDesc,
            observation.getArea(),
            observation.getColorH(),
            observation.getColorS(),
            observation.getColorV(),
            timeStr
        );
    }

    /**
     * 获取状态对应的emoji
     * 
     * @param status 健康状态
     * @return emoji符号
     */
    private String getEmojiForStatus(String status) {
        return switch (status) {
            case "WILT" -> "🥀"; // 枯萎
            case "WILT_WARNING" -> "🟠"; // 中度枯萎
            case "YELLOW_LEAF" -> "🍂"; // 黄叶
            case "TREND_YELLOW" -> "🟡"; // 黄化趋势
            case "TREND_WILT" -> "⚠️"; // 枯萎趋势
            default -> "❓";
        };
    }

    /**
     * 获取状态对应的卡片颜色
     * 
     * @param status 健康状态
     * @return 飞书模板颜色（red/orange/yellow/green/blue/purple/gray）
     */
    private String getTemplateColor(String status) {
        return switch (status) {
            case "WILT" -> "red";
            case "WILT_WARNING" -> "orange";
            case "YELLOW_LEAF" -> "yellow";
            case "TREND_YELLOW", "TREND_WILT" -> "orange";
            default -> "gray";
        };
    }

    /**
     * 发送飞书消息
     * 
     * 使用HTTP POST请求发送到Webhook地址
     * 
     * @param messageContent JSON格式的消息内容
     * @return true表示发送成功
     */
    private boolean sendFeishuMessage(String messageContent) {
        try {
            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(messageContent))
                    .build();
            
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            // 检查响应
            if (response.statusCode() == 200) {
                log.debug("飞书消息发送成功: {}", response.body());
                return true;
            } else {
                log.error("飞书消息发送失败: HTTP {}", response.statusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("发送飞书消息异常", e);
            return false;
        }
    }

    /**
     * 测试Webhook连接
     * 
     * @return true表示连接正常
     */
    public boolean testWebhook() {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return false;
        }
        
        String testMessage = """
            {
                "msg_type": "text",
                "content": {
                    "text": "🧪 办公室绿萝 Pro Webhook测试成功！"
                }
            }
            """;
        
        return sendFeishuMessage(testMessage);
    }
}
