<template>
  <div class="home">
    <!-- 统计卡片区域 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <!-- 总叶片数 -->
          <div class="stat-card">
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
        <el-card shadow="hover">
          <!-- 需要关注的叶片 -->
          <div class="stat-card">
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
        <el-card shadow="hover">
          <!-- 健康叶片 -->
          <div class="stat-card">
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
        <el-card shadow="hover">
          <!-- 监控状态 -->
          <div class="stat-card">
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
        <div 
          v-for="leaf in attentionLeaves.slice(0, 6)" 
          :key="leaf.leafId"
          class="attention-item"
          @click="$router.push(`/leaves/${leaf.leafId}`)"
        >
          <div class="attention-info">
            <span class="leaf-id">{{ leaf.leafId }}</span>
            <el-tag :type="getStatusType(leaf.healthStatus)" size="small">
              {{ getStatusText(leaf.healthStatus) }}
            </el-tag>
          </div>
          <div class="attention-detail">
            <span>H: {{ leaf.colorH?.toFixed(1) || '-' }}</span>
            <span>S: {{ leaf.colorS?.toFixed(1) || '-' }}</span>
            <span>V: {{ leaf.colorV?.toFixed(1) || '-' }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 最新叶片列表 -->
    <el-card class="leaves-card">
      <template #header>
        <div class="card-header">
          <span>🌿 全部叶片</span>
          <div>
            <el-button type="primary" size="small" @click="$router.push('/monitor')">
              <el-icon><VideoPlay /></el-icon> 监控
            </el-button>
            <el-button size="small" @click="$router.push('/leaves')">
              查看全部
            </el-button>
          </div>
        </div>
      </template>

      <div class="leaves-grid" v-if="leaves.length > 0">
        <div 
          v-for="leaf in leaves.slice(0, 8)" 
          :key="leaf.leafId"
          class="leaf-card"
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
              <span class="value">{{ leaf.growthPercentage?.toFixed(1) || '0' }}%</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无叶片数据">
        <el-button type="primary" @click="$router.push('/monitor')">
          去监控页面
        </el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getAllLeaves, getLeavesNeedingAttention, getLeafStatistics } from '../api/leafApi';
import { HEALTH_STATUS } from '../config';

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
  return HEALTH_STATUS[status]?.text || status || '未知';
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

.stat-card {
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
  padding: 12px;
  border-radius: 8px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  cursor: pointer;
  transition: all 0.3s;
}

.attention-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.attention-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.leaf-id {
  font-weight: bold;
  color: #303133;
}

.attention-detail {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #666;
}

/* 叶片卡片网格 */
.leaves-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.leaf-card {
  padding: 16px;
  border-radius: 12px;
  background: white;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s;
}

.leaf-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #1890ff;
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
</style>
