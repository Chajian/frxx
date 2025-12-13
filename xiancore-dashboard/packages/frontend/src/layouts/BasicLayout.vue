<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h2>XianCore</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/players">
          <el-icon><User /></el-icon>
          <span>玩家管理</span>
        </el-menu-item>
        <el-menu-item index="/sects">
          <el-icon><OfficeBuilding /></el-icon>
          <span>宗门管理</span>
        </el-menu-item>
        <el-menu-item index="/ranking">
          <el-icon><TrophyBase /></el-icon>
          <span>排行榜</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button @click="handleRefresh" :icon="Refresh" circle />
        </div>
      </el-header>

      <!-- 内容 -->
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { House, User, OfficeBuilding, TrophyBase, Refresh } from '@element-plus/icons-vue';

const route = useRoute();

const activeMenu = computed(() => route.path);
const currentRouteName = computed(() => route.meta.title || '仪表盘');

const handleRefresh = () => {
  location.reload();
};
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4b;
  color: #fff;
}

.logo h2 {
  margin: 0;
  font-size: 20px;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
