<template>
  <div class="monitor-container">
    <!-- 页面标题 -->
    <el-card class="header-card">
      <div class="header-bar">
        <h2>📹 实时监控</h2>
        <div class="header-actions">
          <el-tag :type="status.online ? 'success' : 'danger'" size="large">
            {{ status.online ? '在线' : '离线' }}
          </el-tag>
          <el-button type="primary" @click="capture" :loading="capturing" :disabled="!status.online">
            <el-icon><Camera /></el-icon> 拍摄
          </el-button>
          <el-button @click="refreshStatus">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 主监控区域 -->
    <el-row :gutter="20">
      <!-- 视频/图像显示区 -->
      <el-col :span="16">
        <el-card class="video-card">
          <div class="video-container">
            <img 
              v-if="latestImage" 
              :src="latestImageUrl" 
              alt="监控画面"
              class="video-frame"
            />
            <div v-else class="video-placeholder">
              <el-icon :size="80"><VideoCamera /></el-icon>
              <p>暂无监控画面</p>
              <p class="hint">点击「拍摄」按钮获取第一帧</p>
            </div>
          </div>

          <!-- 状态信息栏 -->
          <div class="video-status" v-if="lastCapture">
            <span>最后拍摄: {{ formatTime(lastCapture.captureTime) }}</span>
            <span>叶片数: {{ lastCapture.leafCount || 0 }}</span>
            <span v-if="lastCapture.greenCoverage">覆盖率: {{ lastCapture.greenCoverage.toFixed(1) }}%</span>
            <span v-if="lastCapture.alertSent" class="alert-badge">已发送预警</span>
          </div>
        </el-card>

        <!-- 拍摄结果 -->
        <el-card class="result-card" v-if="captureResult">
          <template #header>
            <span>📊 拍摄结果</span>
          </template>
          <div class="result-grid">
            <div class="result-item">
              <span class="label">状态</span>
              <el-tag :type="captureResult.success ? 'success' : 'danger'">
                {{ captureResult.success ? '成功' : '失败' }}
              </el-tag>
            </div>
            <div class="result-item" v-if="captureResult.success">
              <span class="label">叶片数</span>
              <span class="value">{{ captureResult.leafCount }}</span>
            </div>
            <div class="result-item" v-if="captureResult.success">
              <span class="label">新叶片</span>
              <span class="value">{{ captureResult.newLeafCount }}</span>
            </div>
            <div class="result-item" v-if="captureResult.success">
              <span class="label">覆盖率</span>
              <span class="value">{{ captureResult.greenCoverage?.toFixed(1) || '-' }}%</span>
            </div>
            <div class="result-item" v-if="captureResult.success">
              <span class="label">预警</span>
              <el-tag :type="captureResult.alertSent ? 'warning' : 'success'" size="small">
                {{ captureResult.alertSent ? '已发送' : '无' }}
              </el-tag>
            </div>
            <div class="result-item" v-if="!captureResult.success">
              <span class="label">错误</span>
              <span class="error">{{ captureResult.errorMessage }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 侧边信息栏 -->
      <el-col :span="8">
        <!-- 监控状态 -->
        <el-card class="status-card">
          <template #header>
            <span>📍 监控状态</span>
          </template>
          <div class="status-list">
            <div class="status-item">
              <span class="label">状态</span>
              <el-tag :type="status.online ? 'success' : 'danger'">
                {{ status.online ? '在线' : '离线' }}
              </el-tag>
            </div>
            <div class="status-item">
              <span class="label">摄像头</span>
              <span class="value">Camera {{ status.cameraIndex || 0 }}</span>
            </div>
            <div class="status-item">
              <span class="label">今日拍摄</span>
              <span class="value">{{ status.captureCount || 0 }} 次</span>
            </div>
            <div class="status-item">
              <span class="label">最后拍摄</span>
              <span class="value">{{ status.lastCapture ? formatTime(status.lastCapture) : '-' }}</span>
            </div>
          </div>
        </el-card>

        <!-- 今日统计 -->
        <el-card class="stats-card">
          <template #header>
            <span>📊 今日统计</span>
          </template>
          <div class="stats-list" v-if="lastCapture">
            <div class="stat-item">
              <span class="icon healthy"><el-icon><CircleCheck /></el-icon></span>
              <span class="text">健康叶片</span>
              <span class="value">{{ lastCapture.healthyCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="icon attention"><el-icon><Warning /></el-icon></span>
              <span class="text">需关注</span>
              <span class="value">{{ lastCapture.attentionCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="icon coverage"><el-icon><Odometer /></el-icon></span>
              <span class="text">绿色覆盖率</span>
              <span class="value">{{ lastCapture.greenCoverage?.toFixed(1) || '-' }}%</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>

        <!-- 操作提示 -->
        <el-card class="tips-card">
          <template #header>
            <span>💡 操作提示</span>
          </template>
          <ul class="tips-list">
            <li>点击「拍摄」按钮手动触发一次拍摄</li>
            <li>拍摄后会自动进行叶片检测</li>
            <li>发现异常叶片会自动发送飞书预警</li>
            <li>可在「叶片库」查看所有叶片详情</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';

// API基础URL
const API_BASE = 'http://localhost:5501/api';

// 监控状态
const status = ref({
  online: false,
  cameraIndex: 0,
  lastCapture: null,
  captureCount: 0,
  status: 'OFFLINE',
  message: ''
});

// 拍摄状态
const capturing = ref(false);

// 最后拍摄结果
const lastCapture = ref(null);

// 最新图片
const latestImage = ref(null);

// 最新图片URL
const latestImageUrl = computed(() => {
  return `http://localhost:5501/api/monitor/latest?t=${Date.now()}`;
});

// 拍摄结果
const captureResult = ref(null);

// 定时刷新
let refreshTimer = null;

/**
 * 刷新监控状态
 */
async function refreshStatus() {
  try {
    const response = await axios.get(`${API_BASE}/monitor/status`);
    status.value = response.data;
    
    // 如果在线，尝试获取最新图片
    if (status.value.online) {
      loadLatestImage();
    }
  } catch (error) {
    console.error('获取状态失败:', error);
    status.value.online = false;
  }
}

/**
 * 加载最新图片
 */
async function loadLatestImage() {
  try {
    // 添加时间戳防止缓存
    const timestamp = new Date().getTime();
    latestImage.value = `http://localhost:5501/api/monitor/latest?t=${timestamp}`;
  } catch (error) {
    console.error('加载图片失败:', error);
    latestImage.value = null;
  }
}

/**
 * 触发拍摄
 */
async function capture() {
  if (capturing.value) return;
  
  capturing.value = true;
  captureResult.value = null;
  
  try {
    const response = await axios.post(`${API_BASE}/monitor/capture?force=false`);
    captureResult.value = response.data;
    
    if (response.data.success) {
      lastCapture.value = response.data;
      ElMessage.success('拍摄成功');
      
      // 刷新图片
      loadLatestImage();
      
      // 刷新状态
      refreshStatus();
    } else {
      ElMessage.warning(response.data.errorMessage || '拍摄失败');
    }
  } catch (error) {
    console.error('拍摄失败:', error);
    ElMessage.error('拍摄请求失败: ' + (error.message || '网络错误'));
    captureResult.value = {
      success: false,
      errorMessage: error.message
    };
  } finally {
    capturing.value = false;
  }
}

/**
 * 格式化时间
 */
function formatTime(timeStr) {
  if (!timeStr) return '-';
  const date = new Date(timeStr);
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`;
}

// 组件挂载
onMounted(() => {
  refreshStatus();
  // 每10秒刷新状态
  refreshTimer = setInterval(refreshStatus, 10000);
});

// 组件卸载
onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
  }
});
</script>

<style scoped>
.monitor-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 20px;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-bar h2 {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 视频卡片 */
.video-card {
  margin-bottom: 20px;
}

.video-container {
  width: 100%;
  min-height: 400px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-frame {
  width: 100%;
  height: auto;
  max-height: 500px;
  object-fit: contain;
}

.video-placeholder {
  color: #666;
  text-align: center;
}

.video-placeholder p {
  margin: 16px 0 0 0;
}

.video-placeholder .hint {
  font-size: 12px;
  color: #999;
}

.video-status {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
  color: #666;
}

.alert-badge {
  color: #ff4d4f;
  font-weight: bold;
}

/* 结果卡片 */
.result-card {
  margin-bottom: 20px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.result-item {
  text-align: center;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.result-item .label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.result-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.result-item .error {
  color: #ff4d4f;
  font-size: 14px;
}

/* 状态卡片 */
.status-card {
  margin-bottom: 20px;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-item .label {
  color: #909399;
  font-size: 14px;
}

.status-item .value {
  color: #303133;
  font-size: 14px;
}

/* 统计卡片 */
.stats-card {
  margin-bottom: 20px;
}

.stats-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-item .icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.stat-item .icon.healthy {
  background: #f6ffed;
  color: #52c41a;
}

.stat-item .icon.attention {
  background: #fff7e6;
  color: #faad14;
}

.stat-item .icon.coverage {
  background: #e6f7ff;
  color: #1890ff;
}

.stat-item .text {
  flex: 1;
  font-size: 14px;
  color: #666;
}

.stat-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

/* 提示卡片 */
.tips-card {
  background: #f0f9ff;
  border-color: #91d5ff;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #666;
}

.tips-list li {
  margin-bottom: 8px;
}

.tips-list li:last-child {
  margin-bottom: 0;
}
</style>
