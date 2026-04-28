package com.greenrobot.monitor.entity;

/**
 * 叶片健康状态枚举
 * 
 * 定义叶片的所有可能健康状态
 * 用于记录叶片当前状态和历史变化
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
public enum HealthStatus {

    /** 健康状态 - H值30-50，S>60，V<150 */
    HEALTHY("健康", "叶片颜色正常，生长良好"),

    /** 黄叶状态 - H值>65，呈黄色 */
    YELLOW_LEAF("黄叶", "检测到叶片偏黄，需要关注"),

    /** 枯萎发白 - V>200且S<40，呈现褪色发白状态 */
    WILT("枯萎", "检测到叶片枯萎发白，需要紧急处理"),

    /** 中度枯萎预警 - V>220且S<50，处于枯萎边缘 */
    WILT_WARNING("中度枯萎", "检测到叶片有枯萎趋势，需要关注"),

    /** 黄化趋势 - H值相对历史增加超过10 */
    TREND_YELLOW("黄化趋势", "叶片颜色有变黄趋势"),

    /** 枯萎趋势 - V值增加超过30且S减少超过15 */
    TREND_WILT("枯萎趋势", "叶片有枯萎趋势"),

    /** 未知状态 - 数据不足或检测失败 */
    UNKNOWN("未知", "状态检测中");

    private final String displayName;
    private final String description;

    HealthStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据状态判断是否需要发送预警
     * 
     * @return true表示需要发送预警
     */
    public boolean needsAlert() {
        return this == YELLOW_LEAF 
            || this == WILT 
            || this == WILT_WARNING
            || this == TREND_YELLOW
            || this == TREND_WILT;
    }

    /**
     * 获取状态的严重程度
     * 
     * @return 0-4，数字越大越严重
     */
    public int getSeverity() {
        switch (this) {
            case HEALTHY:
                return 0;
            case TREND_YELLOW:
            case TREND_WILT:
                return 1;
            case YELLOW_LEAF:
            case WILT_WARNING:
                return 2;
            case WILT:
                return 3;
            default:
                return 0;
        }
    }
}
