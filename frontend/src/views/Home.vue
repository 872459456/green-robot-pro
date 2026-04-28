<template>
  <div class="home">
    <!-- 统计卡片区域 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon leaf-icon">
              <el-icon><Leaf /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalLeaves || 0 }}</div>
              <div class="stat-label">总叶片数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon attention-icon">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.attentionCount || 0 }}</div>
              <div class="stat-label">需要关注</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon healthy-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ getHealthyCount() }}</div>
              <div class="stat-label">健康叶片</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon status-icon" :class="{ online: isOnline }">
              <el-icon><VideoCamera /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ isOnline ? '在线' : '离线' }}</div>
              <div class="stat-label">监控系统</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作入口 -->
    <el-row :gutter="20" class="action-row">
      <el-col :span="24">
        <el-card class="action-card">
          <div class="action-buttons">
            <el-button type="primary" size="large" @click="$router.push('/monitor')">
              <el-icon><VideoPlay /></el-icon> 进入监控
            </el-button>
            <el-button size="large" @click="$router.push('/leaves')">
              <el-icon><Grid /></el-icon> 叶片库
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 需要关注的叶片列表 -->
    <el-card class="attention-card" v-if="attentionLeaves.length > 0">
      <template #header>
        <div class="card-header">
          <span><el-icon><Warning /></el-icon> 需要关注的叶片</span>
          <el-button type="text" @click="$router.push('/leaves?filter=attention')">
            查看全部
          </el-button>
        </div>
      </template>

      <div class="attention-list">
        <el-card 
          v-for="leaf in attentionLeaves.slice(0, 6)" 
          :key="leaf.leafId"
          class="attention-item"
          shadow="hover"
          @click="$router.push(`/leaves/${leaf.leafId}`)"
        >
          <div class="leaf-header">
            <span class="leaf-id">{{ leaf.leafId }}</span>
            <el-tag :type="getStatusType(leaf.healthStatus)" size="small">
              {{ getStatusText(leaf.healthStatus) }}
            </el-tag>
          </div>
          <div class="leaf-detail">
            <span>H: {{ leaf.colorH?.toFixed(1) || '-' }}</span>
            <span>S: {{ leaf.colorS?.toFixed(1) || '-' }}</span>
            <span>V: {{ leaf.colorV?.toFixed(1) || '-' }}</span>
          </div>
          <div class="leaf-time">
            {{ formatTime(leaf.lastObservationTime) }}
          </div>
        </el-card>
      </div>
    </el-card>

    <!-- 最新叶片列表 -->
    <el-card class="leaves-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Leaf /></el-icon> 全部叶片</span>
          <el-button type="primary" size="small" @click="$router.push('/monitor')">
            <el-icon><VideoPlay /></el-icon> 监控
          </el-button>
        </div>
      </template>

      <div class="leaves-grid" v-if="leaves.length > 0">
        <el-card 
          v-for="leaf in leaves.slice(0, 8)" 
          :key="leaf.leafId"
          class="leaf-card"
          shadow="hover"
          @click="$router.push(`/leaves/${leaf.leafId}`)"
        >
          <div class="leaf-header">
            <span class="leaf-id">{{ leaf.leafId }}</span>
            <el-tag :type="getStatusType(leaf.healthStatus)" size="small">
              {{ getStatusText(leaf.healthStatus) }}
            </el-tag>
          </div>
          <div class="leaf-stats">
            <div class="stat-item">
              <span class="label">面积</span>
              <span class="value">{{ leaf.area?.toFixed(0) || '-' }}</span>
            </div>
            <div class="stat-item">
              <span class="label">H值</span>
              <span class="value">{{ leaf.colorH?.toFixed(1) || '-' }}</span>
            </div>
            <div class="stat-item">
              <span class="label">成长</span>
              <span class="value growth" :class="{ positive: (leaf.growthPercentage || 0) > 0 }">
                {{ leaf.growthPercentage?.toFixed(1) || '0' }}%
              </span>
            </div>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无叶片数据">
        <el-button type="primary" @click="$router.push('/monitor')">
          去监控页面拍摄
        </el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getAllLeaves, getLeavesNeedingAttention, getLeafStatistics } from '../api/leafApi';

// 叶片列表数据
const leaves = ref([]);
const attentionLeaves = ref([]);
const statistics = ref({});
const isOnline = ref(false);

// 定时刷新
let refreshTimer = null;

/**
 * 刷新首页数据
 */
async function refreshData() {
  try {
    // 并行请求所有数据
    const [leavesData, attentionData, statsData] = await Promise.all([
      getAllLeaves(),
      getLeavesNeedingAttention(),
      getLeafStatistics()
    ]);
    
    leaves.value = leavesData || [];
    attentionLeaves.value = attentionData || [];
    statistics.value = statsData || {};
    isOnline.value = true;
  } catch (error) {
    console.error('刷新数据失败:', error);
    isOnline.value = false;
  }
}

/**
 * 获取健康叶片数量
 */
function getHealthyCount() {
  if (!statistics.value.statusCounts) return 0;
  return statistics.value.statusCounts['HEALTHY'] || 0;
}

/**
 * 获取状态文本
 */
function getStatusText(status) {
  const statusMap = {
    'HEALTHY': '健康',
    'YELLOW_LEAF': '黄叶',
    'WILT': '枯萎',
    'WILT_WARNING': '中度枯萎',
    'TREND_YELLOW': '黄化趋势',
    'TREND_WILT': '枯萎趋势'
  };
  return statusMap[status] || status || '未知';
}

/**
 * 获取状态对应的Element Plus标签类型
 */
function getStatusType(status) {
  const typeMap = {
    'HEALTHY': 'success',
    'YELLOW_LEAF': 'warning',
    'WILT': 'danger',
    'WILT_WARNING': 'warning',
    'TREND_YELLOW': 'warning',
    'TREND_WILT': 'warning'
  };
  return typeMap[status] || 'info';
}

/**
 * 格式化时间
 */
function formatTime(timeStr) {
  if (!timeStr) return '-';
  const date = new Date(timeStr);
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}

// 组件挂载时开始定时刷新
onMounted(() => {
  refreshData();
  // 每30秒刷新一次
  refreshTimer = setInterval(refreshData, 30000);
});

// 组件卸载时清除定时器
onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
  }
});
</script>

<style scoped>
.home {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 统计卡片样式 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card :deep(.el-card__body) {
  padding: 16px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.leaf-icon {
  background: linear-gradient(135deg, #52c41a, #73d13d);
  color: white;
}

.attention-icon {
  background: linear-gradient(135deg, #faad14, #ffc53d);
  color: white;
}

.healthy-icon {
  background: linear-gradient(135deg, #52c41a, #73d13d);
  color: white;
}

.status-icon {
  background: linear-gradient(135deg, #8c8c8c, #b0b0b0);
  color: white;
}

.status-icon.online {
  background: linear-gradient(135deg, #1890ff, #69c0ff);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 操作入口 */
.action-row {
  margin-bottom: 20px;
}

.action-card :deep(.el-card__body) {
  padding: 20px;
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.action-buttons .el-button {
  min-width: 140px;
}

/* 需要关注的卡片 */
.attention-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.attention-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.attention-item {
  cursor: pointer;
  transition: all 0.3s;
}

.attention-item:hover {
  transform: translateY(-2px);
}

.attention-item .leaf-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.leaf-id {
  font-weight: bold;
  color: #303133;
}

.attention-item .leaf-detail {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.attention-item .leaf-time {
  font-size: 11px;
  color: #999;
}

/* 叶片卡片网格 */
.leaves-card {
  margin-bottom: 20px;
}

.leaves-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.leaf-card {
  cursor: pointer;
  transition: all 0.3s;
}

.leaf-card:hover {
  transform: translateY(-4px);
}

.leaf-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.leaf-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.stat-item {
  text-align: center;
}

.stat-item .label {
  display: block;
  font-size: 12px;
  color: #909399;
}

.stat-item .value {
  display: block;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.stat-item .value.growth {
  color: #909399;
}

.stat-item .value.growth.positive {
  color: #52c41a;
}
</style>
