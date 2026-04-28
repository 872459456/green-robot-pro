package com.greenrobot.monitor.config;

import org.springframework.context.annotation.Configuration;

/**
 * 叶片检测配置文件
 * 
 * 集中管理所有叶片检测相关的阈值参数
 * 所有参数都可通过配置文件或环境变量调整
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
@Configuration
public class LeafConfig {
    
    // ==================== HSV绿色范围 ====================
    // HSV色彩空间比RGB更适合区分绿色植物
    // H值范围: 0-180 (OpenCV中H值范围是0-180，而非0-360)
    
    /** 绿色HSV范围下限 - H值35开始为绿色 */
    public static final int LOWER_GREEN_H = 35;
    
    /** 绿色HSV范围上限 - H值85以内都是绿色 */
    public static final int UPPER_GREEN_H = 85;
    
    /** 绿色饱和度下限 - 饱和度低于30可能是背景或褪色 */
    public static final int LOWER_GREEN_S = 30;
    
    /** 绿色亮度下限 - 亮度低于30可能是阴影 */
    public static final int LOWER_GREEN_V = 30;
    
    // ==================== 叶片健康状态阈值 ====================
    // 基于大量实测数据得出的阈值
    
    /** 黄叶判定阈值 - H值超过65认为偏黄 */
    public static final int YELLOW_LEAF_H_THRESHOLD = 65;
    
    /** 枯萎发白判定 - 高亮度(V>200)且低饱和度(S<40)表示枯萎褪色 */
    public static final int WILT_BRIGHTNESS_THRESHOLD = 200;
    
    /** 枯萎发白判定 - 饱和度低于40表示颜色褪去 */
    public static final int WILT_SATURATION_THRESHOLD = 40;
    
    /** 中度枯萎预警 - V>220且S<50 */
    public static final int WILT_WARNING_BRIGHTNESS = 220;
    
    /** 中度枯萎预警 - S<50表示开始褪色 */
    public static final int WILT_WARNING_SATURATION = 50;
    
    // ==================== 叶片尺寸过滤 ====================
    // 过小或过大的区域不是有效叶片
    
    /** 最小叶片面积 - 面积小于200像素的忽略（可能是噪声） */
    public static final int MIN_LEAF_AREA = 200;
    
    /** 最大叶片面积 - 面积大于100000像素的忽略（可能是多叶片聚集） */
    public static final int MAX_LEAF_AREA = 100000;
    
    // ==================== 形态学操作核大小 ====================
    // 用于去噪和填补小的空洞
    
    /** 形态学核大小 - 用于开闭运算去噪 */
    public static final int MORPHOLOGY_KERNEL_SIZE = 3;
    
    // ==================== 模糊检测阈值 ====================
    // 拉普拉斯方差法检测图片模糊度
    
    /** 模糊判定阈值 - Laplacian方差小于50认为图片模糊 */
    public static final double BLUR_VARIANCE_THRESHOLD = 50.0;
    
    // ==================== 叶片追踪匹配权重 ====================
    // 多维特征匹配的权重分配
    
    /** 位置距离权重 - 35%，位置最近权重最高 */
    public static final double MATCH_POSITION_WEIGHT = 0.35;
    
    /** 颜色相似度权重 - 30%，颜色H值是核心特征 */
    public static final double MATCH_COLOR_WEIGHT = 0.30;
    
    /** 面积比例权重 - 15%，面积变化相对缓慢 */
    public static final double MATCH_AREA_WEIGHT = 0.15;
    
    /** 轮廓相似度权重 - 20%，Hu矩轮廓特征 */
    public static final double MATCH_CONTOUR_WEIGHT = 0.20;
    
    /** 匹配置信度阈值 - 综合得分低于0.3认为不匹配 */
    public static final double MATCH_CONFIDENT_THRESHOLD = 0.3;
    
    // ==================== 新叶片发现 ====================
    // 位置分桶算法参数
    
    /** 分桶大小 - 80像素一个桶，用于新叶片发现 */
    public static final int BUCKET_SIZE = 80;
    
    /** 连续出现次数 - 同一桶连续3帧出现才认为是新叶片 */
    public static final int NEW_LEAF_CONSECUTIVE_FRAMES = 3;
    
    // ==================== 预警冷却时间 ====================
    // 避免同一叶片重复预警
    
    /** 预警冷却时间（小时） - 同一叶片4小时内不重复预警 */
    public static final int ALERT_COOLDOWN_HOURS = 4;
}
