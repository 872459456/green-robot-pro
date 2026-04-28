<template>
  <div id="app">
    <el-container>
      <el-header>
        <h1>🌿 办公室绿萝 Pro</h1>
        <el-menu
          mode="horizontal"
          :router="true"
          :default-active="$route.path"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/iterations">迭代日志</el-menu-item>
          <el-menu-item index="/monitor">监测</el-menu-item>
          <el-menu-item index="/leaves">叶片库</el-menu-item>
        </el-menu>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
</script>

<style>
#app {
  font-family: 'Helvetica Neue', Arial, sans-serif;
}
.el-header {
  display: flex;
  align-items: center;
  background-color: #1890ff;
  color: white;
}
.el-header h1 {
  margin-right: 40px;
  color: white;
}
.el-main {
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
</style>
