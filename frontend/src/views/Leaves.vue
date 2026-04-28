<template>
  <div class="leaves-container">
    <!-- 页面标题和操作栏 -->
    <el-card class="header-card">
      <div class="header-bar">
        <h2>🍃 叶片库</h2>
        <div class="header-actions">
          <!-- 筛选器 -->
          <el-select 
            v-model="filterStatus" 
            placeholder="状态筛选" 
            clearable
            @change="handleFilterChange"
          >
            <el-option label="全部" value="" />
            <el-option label="健康" value="HEALTHY" />
            <el-option label="黄叶" value="YELLOW_LEAF" />
            <el-option label="枯萎" value="WILT" />
            <el-option label="中度枯萎" value="WILT_WARNING" />
            <el-option label="黄化趋势" value="TREND_YELLOW" />
            <el-option label="枯萎趋势" value="TREND_WILT" />
          </el-select>

          <!-- 搜索框 -->
          <el-input
            v-model="searchKeyword"
            placeholder="搜索叶片ID"
            prefix-icon="Search"
            clearable
            @input="handleSearch"
          />

          <!-- 刷新按钮 -->
          <el-button icon="Refresh" @click="refreshLeaves" />
        </div>
      </div>
    </el-card>

    <!-- 统计概览 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4" v-for="(count, status) in statusCounts" :key="status">
        <div 
          class="status-stat" 
          :class="{ active: filterStatus === status }"
          @click="toggleStatusFilter(status)"
        >
          <span class="status-name">{{ getStatusText(status) }}</span>
          <span class="status-count">{{ count }}</span>
        </div>
      </el-col>
    </el-row>

    <!-- 叶片卡片列表 -->
    <div class="leaves-grid" v-if="filteredLeaves.length > 0">
      <el-card 
        v-for="leaf in paginatedLeaves" 
        :key="leaf.leafId"
        class="leaf-card"
        shadow="hover"
        @click="$router.push(`/leaves/${leaf.leafId}`)"
      >
        <!-- 卡片头部：叶片ID和状态 -->
        <div class="card-header">
          <span class="leaf-id">{{ leaf.leafId }}</span>
          <el-tag :type="getStatusType(leaf.healthStatus)" size="small">
            {{ getStatusText(leaf.healthStatus) }}
          </el-tag>
        </div>

        <!-- 叶片图片 -->
        <div class="leaf-image" v-if="leaf.annotatedImagePath || leaf.imagePath">
          <img 
            :src="getImageUrl(leaf.annotatedImagePath || leaf.imagePath)" 
            :alt="leaf.leafId"
          />
        </div>
        <div class="leaf-image placeholder" v-else>
          <el-icon :size="40"><Picture /></el-icon>
        </div>

        <!-- 叶片数据 -->
        <div class="leaf-data">
          <div class="data-row">
            <span class="label">面积</span>
            <span class="value">{{ leaf.area?.toFixed(0) || '-' }}</span>
          </div>
          <div class="data-row">
            <span class="label">H值</span>
            <span class="value">{{ leaf.colorH?.toFixed(1) || '-' }}</span>
          </div>
          <div class="data-row">
            <span class="label">S值</span>
            <span class="value">{{ leaf.colorS?.toFixed(1) || '-' }}</span>
          </div>
          <div class="data-row">
            <span class="label">V值</span>
            <span class="value">{{ leaf.colorV?.toFixed(1) || '-' }}</span>
          </div>
        </div>

        <!-- 成长信息 -->
        <div class="growth-info">
          <span class="growth-label">累计成长</span>
          <span class="growth-value" :class="{ positive: (leaf.growthPercentage || 0) > 0 }">
            {{ leaf.growthPercentage?.toFixed(1) || '0' }}%
          </span>
        </div>

        <!-- 底部信息 -->
        <div class="card-footer">
          <span class="confirm-date">
            确认于 {{ formatDate(leaf.confirmedAt) }}
          </span>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty 
      v-else 
      :description="searchKeyword || filterStatus ? '没有找到匹配的叶片' : '暂无叶片数据'"
    >
      <el-button type="primary" @click="$router.push('/monitor')" v-if="!searchKeyword && !filterStatus">
        去监控页面
      </el-button>
    </el-empty>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="filteredLeaves.length > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredLeaves.length"
        layout="prev, pager, next"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { getAllLeaves, deleteLeaf } from '../api/leafApi';
import { HEALTH_STATUS } from '../config';
import { ElMessage, ElMessageBox } from 'element-plus';

// 叶片列表数据
const allLeaves = ref([]);
const filterStatus = ref('');
const searchKeyword = ref('');
const currentPage = ref(1);
const pageSize = ref(12);

// 筛选后的叶片列表
const filteredLeaves = computed(() => {
  let result = allLeaves.value;
  
  // 按状态筛选
  if (filterStatus.value) {
    result = result.filter(leaf => leaf.healthStatus === filterStatus.value);
  }
  
  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(leaf => 
      leaf.leafId?.toLowerCase().includes(keyword)
    );
  }
  
  return result;
});

// 分页后的叶片列表
const paginatedLeaves = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredLeaves.value.slice(start, end);
});

// 各状态数量统计
const statusCounts = computed(() => {
  const counts = {};
  // 初始化所有状态
  Object.keys(HEALTH_STATUS).forEach(status => {
    counts[status] = 0;
  });
  // 统计
  allLeaves.value.forEach(leaf => {
    if (leaf.healthStatus && counts[leaf.healthStatus] !== undefined) {
      counts[leaf.healthStatus]++;
    }
  });
  return counts;
});

/**
 * 刷新叶片列表
 */
async function refreshLeaves() {
  try {
    allLeaves.value = await getAllLeaves();
    ElMessage.success('刷新成功');
  } catch (error) {
    ElMessage.error('刷新失败: ' + error.message);
  }
}

/**
 * 筛选条件变化时重置分页
 */
function handleFilterChange() {
  currentPage.value = 1;
}

/**
 * 搜索处理
 */
function handleSearch() {
  currentPage.value = 1;
}

/**
 * 切换状态筛选
 */
function toggleStatusFilter(status) {
  if (filterStatus.value === status) {
    filterStatus.value = '';
  } else {
    filterStatus.value = status;
  }
}

/**
 * 获取状态文本
 */
function getStatusText(status) {
  return HEALTH_STATUS[status]?.text || status || '未知';
}

/**
 * 获取状态对应的标签类型
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
 * 获取图片URL
 */
function getImageUrl(path) {
  if (!path) return '';
  // 如果是完整URL直接返回
  if (path.startsWith('http')) return path;
  // 否则拼接基础URL
  return `http://localhost:5501${path}`;
}

/**
 * 格式化日期
 */
function formatDate(dateStr) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

// 组件挂载时加载数据
onMounted(() => {
  refreshLeaves();
});
</script>

<style scoped>
.leaves-container {
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

/* 状态统计栏 */
.stats-row {
  margin-bottom: 20px;
}

.status-stat {
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
}

.status-stat:hover {
  border-color: #1890ff;
}

.status-stat.active {
  border-color: #1890ff;
  background: #e6f7ff;
}

.status-name {
  font-size: 14px;
  color: #666;
}

.status-count {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

/* 叶片网格 */
.leaves-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.leaf-card {
  cursor: pointer;
  transition: all 0.3s;
}

.leaf-card:hover {
  transform: translateY(-4px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.leaf-id {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.leaf-image {
  height: 140px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.leaf-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.leaf-image.placeholder {
  color: #d9d9d9;
}

.leaf-data {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.data-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.data-row .label {
  color: #909399;
}

.data-row .value {
  font-weight: 500;
  color: #303133;
}

.growth-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-top: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.growth-label {
  font-size: 14px;
  color: #909399;
}

.growth-value {
  font-size: 16px;
  font-weight: bold;
  color: #909399;
}

.growth-value.positive {
  color: #52c41a;
}

.card-footer {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
