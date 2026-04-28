<template>
  <div class="leaf-detail-container">
    <!-- 返回按钮 -->
    <div class="back-bar">
      <el-button @click="$router.push('/leaves')">
        <el-icon><ArrowLeft /></el-icon> 返回叶片库
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 叶片详情 -->
    <div v-else-if="leaf" class="detail-content">
      <!-- 头部信息 -->
      <el-card class="header-card">
        <div class="header-info">
          <div class="title-row">
            <h1>{{ leaf.leafId }}</h1>
            <el-tag :type="getStatusType(leaf.healthStatus)" size="large">
              {{ getStatusText(leaf.healthStatus) }}
            </el-tag>
          </div>
          <div class="meta-row">
            <span>确认时间: {{ formatDateTime(leaf.confirmedAt) }}</span>
            <span>最后观测: {{ formatDateTime(leaf.lastObservationTime) }}</span>
          </div>
        </div>

        <div class="header-actions">
          <el-button type="danger" @click="handleDelete">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </el-card>

      <!-- 主内容区 -->
      <el-row :gutter="20">
        <!-- 左侧：图片展示 -->
        <el-col :span="14">
          <el-card class="image-card">
            <!-- 图片模式切换 -->
            <div class="image-controls">
              <el-radio-group v-model="imageMode" size="small">
                <el-radio-button label="annotated">标注图</el-radio-button>
                <el-radio-button label="original">原图</el-radio-button>
                <el-radio-button label="compare">对比</el-radio-button>
              </el-radio-group>
              <el-button 
                size="small" 
                @click="showEnlarge = true"
                v-if="imageMode !== 'compare'"
              >
                <el-icon><FullScreen /></el-icon> 放大
              </el-button>
            </div>

            <!-- 单图模式 -->
            <div class="image-display" v-if="imageMode !== 'compare'">
              <img 
                :src="currentImageUrl" 
                :alt="leaf.leafId"
                @click="showEnlarge = true"
              />
            </div>

            <!-- 对比模式 -->
            <div class="image-compare" v-else>
              <div class="compare-item">
                <div class="compare-label">标注图</div>
                <img :src="getImageUrl(leaf.annotatedImagePath)" />
              </div>
              <div class="compare-item">
                <div class="compare-label">原图</div>
                <img :src="getImageUrl(leaf.imagePath)" />
              </div>
            </div>

            <!-- 历史滑动器 -->
            <div class="history-slider" v-if="observations.length > 1">
              <span class="slider-label">{{ currentHistoryIndex + 1 }} / {{ observations.length }}</span>
              <el-slider 
                v-model="currentHistoryIndex"
                :min="0"
                :max="observations.length - 1"
                :show-tooltip="false"
              />
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：数据展示 -->
        <el-col :span="10">
          <!-- 核心数据卡片 -->
          <el-card class="data-card">
            <template #header>
              <span>📊 当前状态</span>
            </template>

            <div class="data-grid">
              <div class="data-item">
                <span class="label">面积</span>
                <span class="value">{{ currentObservation?.area?.toFixed(0) || '-' }}</span>
              </div>
              <div class="data-item">
                <span class="label">H值</span>
                <span class="value">{{ currentObservation?.colorH?.toFixed(1) || '-' }}</span>
              </div>
              <div class="data-item">
                <span class="label">S值</span>
                <span class="value">{{ currentObservation?.colorS?.toFixed(1) || '-' }}</span>
              </div>
              <div class="data-item">
                <span class="label">V值</span>
                <span class="value">{{ currentObservation?.colorV?.toFixed(1) || '-' }}</span>
              </div>
            </div>

            <!-- 成长信息 -->
            <div class="growth-section">
              <div class="growth-item">
                <span class="label">累计成长</span>
                <span class="value" :class="{ positive: (leaf.growthPercentage || 0) > 0 }">
                  {{ leaf.growthPercentage?.toFixed(1) || '0' }}%
                </span>
              </div>
              <div class="growth-item">
                <span class="label">历史最大面积</span>
                <span class="value">{{ maxArea || '-' }}</span>
              </div>
            </div>
          </el-card>

          <!-- 成长趋势图表 -->
          <el-card class="chart-card">
            <template #header>
              <span>📈 成长记录</span>
            </template>

            <div class="chart-container">
              <div id="areaChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 历史记录列表 -->
      <el-card class="history-card">
        <template #header>
          <span>📋 历史记录</span>
        </template>

        <el-table :data="observations" stripe>
          <el-table-column prop="observationTime" label="时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.observationTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="area" label="面积" width="100">
            <template #default="{ row }">
              {{ row.area?.toFixed(0) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="colorH" label="H值" width="80">
            <template #default="{ row }">
              {{ row.colorH?.toFixed(1) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="colorS" label="S值" width="80">
            <template #default="{ row }">
              {{ row.colorS?.toFixed(1) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="colorV" label="V值" width="80">
            <template #default="{ row }">
              {{ row.colorV?.toFixed(1) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="matchConfidence" label="置信度" width="100">
            <template #default="{ row }">
              {{ row.matchConfidence ? (row.matchConfidence * 100).toFixed(0) + '%' : '-' }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 放大图片Modal -->
      <el-dialog v-model="showEnlarge" title="图片查看" width="80%">
        <img :src="currentImageUrl" style="width: 100%;" />
      </el-dialog>
    </div>

    <!-- 叶片不存在 -->
    <el-empty v-else description="叶片不存在">
      <el-button type="primary" @click="$router.push('/leaves')">
        返回叶片库
      </el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getLeafById, getLeafObservations, deleteLeaf, getRecentObservations } from '../api/leafApi';
import { HEALTH_STATUS } from '../config';
import { ElMessage, ElMessageBox } from 'element-plus';

// 路由
const route = useRoute();
const router = useRouter();

// 叶片ID
const leafId = computed(() => route.params.leafId);

// 数据
const leaf = ref(null);
const observations = ref([]);
const maxArea = ref(null);
const loading = ref(true);

// UI状态
const imageMode = ref('annotated');
const currentHistoryIndex = ref(0);
const showEnlarge = ref(false);

// 当前观测记录
const currentObservation = computed(() => {
  if (observations.value.length === 0) return null;
  return observations.value[currentHistoryIndex.value] || observations.value[0];
});

// 当前显示的图片URL
const currentImageUrl = computed(() => {
  if (!leaf.value) return '';
  
  const path = imageMode.value === 'annotated' 
    ? leaf.value.annotatedImagePath 
    : leaf.value.imagePath;
  
  return getImageUrl(path);
});

/**
 * 加载叶片数据
 */
async function loadLeafData() {
  loading.value = true;
  try {
    // 并行请求叶片详情和观测记录
    const [leafData, observationsData, maxAreaValue] = await Promise.all([
      getLeafById(leafId.value),
      getLeafObservations(leafId.value),
      getRecentObservations(leafId.value, 1).then(obs => {
        // 获取历史最大面积
        return null; // TODO: 调用API获取
      })
    ]);
    
    leaf.value = leafData;
    observations.value = observationsData || [];
    maxArea.value = maxAreaValue;
    
    // 如果有观测记录，初始化历史滑动器
    if (observations.value.length > 0) {
      currentHistoryIndex.value = 0;
    }
    
    // 绘制图表
    drawCharts();
  } catch (error) {
    ElMessage.error('加载失败: ' + error.message);
  } finally {
    loading.value = false;
  }
}

/**
 * 绘制成长趋势图表
 */
async function drawCharts() {
  if (observations.value.length === 0) return;
  
  const container = document.getElementById('areaChart');
  if (!container) return;
  
  // 如果观测记录太少，显示提示
  if (observations.value.length < 2) {
    container.innerHTML = '<p style="text-align: center; color: #909399;">数据不足，需要至少2条记录才能显示图表</p>';
    return;
  }
  
  try {
    // 动态导入G2Plot
    const [ { Line } ] = await Promise.all([
      import('@antv/g2plot')
    ]);
    
    // 准备面积数据（按时间正序）
    const areaData = observations.value
      .slice()
      .reverse()
      .map((obs, index) => ({
        date: formatDateShort(obs.observationTime),
        area: obs.area,
        hValue: obs.colorH
      }));
    
    // 绘制面积趋势图
    const areaPlot = new Line(container, {
      data: areaData,
      xField: 'date',
      yField: 'area',
      smooth: true,
      color: '#52c41a',
      point: {
        size: 4,
        shape: 'circle',
        style: {
          fill: '#52c41a'
        }
      },
      label: {
        formatter: (v) => v.toFixed(0)
      },
      xAxis: {
        label: {
          formatter: (v) => v
        }
      },
      yAxis: {
        label: {
          formatter: (v) => v.toFixed(0)
        }
      },
      title: {
        text: '面积趋势',
        style: {
          fontSize: 14
        }
      },
      height: 200
    });
    
    areaPlot.render();
    
  } catch (error) {
    console.error('图表渲染失败:', error);
    container.innerHTML = '<p style="text-align: center; color: #ff4d4f;">图表加载失败</p>';
  }
}

/**
 * 格式化日期（短格式）
 */
function formatDateShort(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

/**
 * 删除叶片
 */
async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      '确定要删除叶片 ' + leafId.value + ' 吗？所有观测记录也会被删除。',
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await deleteLeaf(leafId.value);
    ElMessage.success('删除成功');
    router.push('/leaves');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message);
    }
  }
}

/**
 * 获取图片URL
 */
function getImageUrl(path) {
  if (!path) return '';
  if (path.startsWith('http')) return path;
  return `http://localhost:5501${path}`;
}

/**
 * 获取状态文本
 */
function getStatusText(status) {
  return HEALTH_STATUS[status]?.text || status || '未知';
}

/**
 * 获取状态类型
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
 * 格式化日期时间
 */
function formatDateTime(dateStr) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN');
}

// 监听叶片变化，重新加载数据
watch(leafId, () => {
  loadLeafData();
});

// 组件挂载时加载数据
onMounted(() => {
  loadLeafData();
});
</script>

<style scoped>
.leaf-detail-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.back-bar {
  margin-bottom: 20px;
}

.loading {
  padding: 40px;
}

/* 头部卡片 */
.header-card {
  margin-bottom: 20px;
}

.header-info {
  display: inline-block;
  vertical-align: top;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.title-row h1 {
  margin: 0;
  font-size: 28px;
}

.meta-row {
  display: flex;
  gap: 24px;
  font-size: 14px;
  color: #909399;
}

.header-actions {
  float: right;
}

/* 图片卡片 */
.image-card {
  margin-bottom: 20px;
}

.image-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.image-display {
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.image-display img {
  width: 100%;
  cursor: pointer;
}

.image-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.compare-item {
  text-align: center;
}

.compare-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.compare-item img {
  width: 100%;
  border-radius: 8px;
}

.history-slider {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.slider-label {
  font-size: 14px;
  color: #666;
  min-width: 60px;
}

/* 数据卡片 */
.data-card {
  margin-bottom: 20px;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.data-item {
  text-align: center;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
}

.data-item .label {
  display: block;
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.data-item .value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.growth-section {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.growth-item {
  text-align: center;
}

.growth-item .label {
  display: block;
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.growth-item .value {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: #909399;
}

.growth-item .value.positive {
  color: #52c41a;
}

/* 图表卡片 */
.chart-card {
  margin-bottom: 20px;
}

.chart-container {
  height: 200px;
}

.chart {
  width: 100%;
  height: 100%;
}

/* 历史记录卡片 */
.history-card {
  margin-bottom: 20px;
}
</style>
