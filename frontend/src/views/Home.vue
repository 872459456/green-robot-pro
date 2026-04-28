<template>
  <div class="home">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>🌿 办公室绿萝 Pro</template>
          <el-result
            icon="info"
            title="系统开发中"
            sub-title="Java + Vue 重构版，当前处于基础设施搭建阶段"
          >
            <template #extra>
              <el-button type="primary" @click="$router.push('/iterations')">
                查看迭代日志
              </el-button>
            </template>
          </el-result>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <el-card>
          <template #header>📊 当前版本</template>
          <el-statistic title="Python生产版" :value="2.2" :precision="1" />
          <p class="version-note">作为生产参考</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>🚀 开发版本</template>
          <el-statistic title="Java + Vue + AI" value="Pro" />
          <p class="version-note">重构进行中</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>📅 启动日期</template>
          <el-statistic title="v3.0开始" :value="20260428" />
          <p class="version-note">2026-04-28</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>📋 迭代路线图</template>
          <el-steps :active="0" align-center>
            <el-step title="v3.0 基础设施" description="迭代日志 + Java骨架" />
            <el-step title="v3.1 前后端分离" description="Vue + REST API" />
            <el-step title="v4.0 本地OSS" description="图片存储升级" />
            <el-step title="v4.1 多摄像头" description="硬件扩展" />
          </el-steps>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
}
.version-note {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
</style>
