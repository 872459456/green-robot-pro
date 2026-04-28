/**
 * 叶片API服务
 * 
 * 封装所有与叶片相关的HTTP请求
 * 使用axios进行网络请求
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */

import axios from 'axios';
import { API_BASE_URL, API_TIMEOUT } from '../config';

// 创建axios实例，配置默认参数
const leafApiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json'
  }
});

// ==================== 叶片档案API ====================

/**
 * 获取所有已确认的叶片列表
 * 
 * GET /api/leaves
 * 
 * @returns {Promise<Array>} 叶片列表
 */
export async function getAllLeaves() {
  const response = await leafApiClient.get('/leaves');
  return response.data;
}

/**
 * 根据ID获取叶片详情
 * 
 * GET /api/leaves/{leafId}
 * 
 * @param {string} leafId 叶片ID
 * @returns {Promise<Object>} 叶片详情
 */
export async function getLeafById(leafId) {
  const response = await leafApiClient.get(`/leaves/${leafId}`);
  return response.data;
}

/**
 * 获取需要关注的叶片
 * 
 * GET /api/leaves/attention
 * 
 * @returns {Promise<Array>} 需要关注的叶片列表
 */
export async function getLeavesNeedingAttention() {
  const response = await leafApiClient.get('/leaves/attention');
  return response.data;
}

/**
 * 确认新叶片
 * 
 * POST /api/leaves/confirm
 * 
 * @param {Object} observation 观测记录
 * @returns {Promise<Object>} 新创建的叶片档案
 */
export async function confirmLeaf(observation) {
  const response = await leafApiClient.post('/leaves/confirm', observation);
  return response.data;
}

/**
 * 更新叶片信息
 * 
 * PUT /api/leaves/{leafId}
 * 
 * @param {string} leafId 叶片ID
 * @param {Object} observation 最新观测记录
 * @returns {Promise<Object>} 更新后的叶片档案
 */
export async function updateLeaf(leafId, observation) {
  const response = await leafApiClient.put(`/leaves/${leafId}`, observation);
  return response.data;
}

/**
 * 删除叶片
 * 
 * DELETE /api/leaves/{leafId}
 * 
 * @param {string} leafId 叶片ID
 * @returns {Promise<void>}
 */
export async function deleteLeaf(leafId) {
  await leafApiClient.delete(`/leaves/${leafId}`);
}

// ==================== 观测记录API ====================

/**
 * 获取叶片的历史观测记录
 * 
 * GET /api/leaves/{leafId}/observations
 * 
 * @param {string} leafId 叶片ID
 * @returns {Promise<Array>} 观测记录列表
 */
export async function getLeafObservations(leafId) {
  const response = await leafApiClient.get(`/leaves/${leafId}/observations`);
  return response.data;
}

/**
 * 获取叶片最近的观测记录
 * 
 * GET /api/leaves/{leafId}/observations/recent?limit=10
 * 
 * @param {string} leafId 叶片ID
 * @param {number} limit 返回数量
 * @returns {Promise<Array>} 最近的N条记录
 */
export async function getRecentObservations(leafId, limit = 10) {
  const response = await leafApiClient.get(
    `/leaves/${leafId}/observations/recent?limit=${limit}`
  );
  return response.data;
}

// ==================== 统计API ====================

/**
 * 获取叶片统计信息
 * 
 * GET /api/leaves/statistics
 * 
 * @returns {Promise<Object>} 统计信息
 */
export async function getLeafStatistics() {
  const response = await leafApiClient.get('/leaves/statistics');
  return response.data;
}

// ==================== 导出API客户端 ====================

// 导出axios实例，外部可以直接使用
export { leafApiClient };
