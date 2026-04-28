/**
 * 检测API服务
 * 
 * 封装YOLOv8模型检测相关的HTTP请求
 * 
 * @author 狼群团队
 * @version 4.0.0
 * @since 2026-04-28
 */

import axios from 'axios';
import { API_BASE_URL, API_TIMEOUT } from '../config';

// 创建axios实例
const detectApiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json'
  }
});

/**
 * 执行目标检测
 * 
 * POST /api/detect
 * 
 * @param {string} imagePath 图片路径
 * @returns {Promise<Object>} 检测结果
 */
export async function detect(imagePath) {
  const response = await detectApiClient.post('/detect', null, {
    params: { imagePath }
  });
  return response.data;
}

/**
 * 获取模型状态
 * 
 * GET /api/detect/status
 * 
 * @returns {Promise<Object>} 模型状态
 */
export async function getModelStatus() {
  const response = await detectApiClient.get('/detect/status');
  return response.data;
}

/**
 * 批量检测
 * 
 * POST /api/detect/batch
 * 
 * @param {string[]} imagePaths 图片路径数组
 * @returns {Promise<string>} 批量检测结果
 */
export async function detectBatch(imagePaths) {
  const response = await detectApiClient.post('/detect/batch', null, {
    params: { imagePaths: imagePaths.join(',') }
  });
  return response.data;
}

/**
 * 检测并获取带标注的图片
 * 
 * POST /api/detect/annotate
 * 
 * @param {string} imagePath 图片路径
 * @returns {Promise<Blob>} 带标注的图片blob
 */
export async function detectAndAnnotate(imagePath) {
  const response = await detectApiClient.post('/detect/annotate', null, {
    params: { imagePath },
    responseType: 'blob'
  });
  return response.data;
}

// 导出API客户端
export { detectApiClient };