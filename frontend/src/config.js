/**
 * API配置文件
 * 
 * 集中管理所有与后端API相关的配置
 * 包括API地址、端口、超时设置等
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */

// API基础地址
// 开发环境: localhost:5501
// 生产环境: 需要配置为实际部署地址
const API_BASE_URL = process.env.VITE_API_BASE_URL || 'http://localhost:5501/api';

// API超时时间（毫秒）
// 设置为30秒，避免大数据请求超时
const API_TIMEOUT = 30000;

// 轮询间隔（毫秒）
// 用于实时监控画面的刷新
const POLLING_INTERVAL = 5000;

// 图像相关配置
const IMAGE_CONFIG = {
  // 标注图路径前缀
  annotatedPrefix: '/static/captures/annotated/',
  // 原图路径前缀
  originalPrefix: '/static/captures/',
  // 支持的图片格式
  allowedFormats: ['jpg', 'jpeg', 'png'],
  // 最大图片大小（MB）
  maxSize: 10
};

// 叶片健康状态配置
const HEALTH_STATUS = {
  HEALTHY: { text: '健康', color: '#52c41a', icon: 'CheckCircle' },
  YELLOW_LEAF: { text: '黄叶', color: '#faad14', icon: 'Warning' },
  WILT: { text: '枯萎', color: '#ff4d4f', icon: 'CloseCircle' },
  WILT_WARNING: { text: '中度枯萎', color: '#ff7a45', icon: 'ExclamationCircle' },
  TREND_YELLOW: { text: '黄化趋势', color: '#fa8c16', icon: 'ArrowUp' },
  TREND_WILT: { text: '枯萎趋势', color: '#fa8c16', icon: 'ArrowUp' },
  UNKNOWN: { text: '未知', color: '#8c8c8c', icon: 'QuestionCircle' }
};

// 导出配置
export {
  API_BASE_URL,
  API_TIMEOUT,
  POLLING_INTERVAL,
  IMAGE_CONFIG,
  HEALTH_STATUS
};

// 导出为Vue插件
export default {
  install(app) {
    // 将配置注入到Vue实例
    app.config.globalProperties.$apiConfig = {
      API_BASE_URL,
      API_TIMEOUT,
      POLLING_INTERVAL,
      IMAGE_CONFIG,
      HEALTH_STATUS
    };
  }
};
