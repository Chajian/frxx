<template>
  <div class="layout-container" :class="layoutClasses" :data-layout="preferencesStore.layout">
    <!-- 侧边栏导航布局 -->
    <template v-if="preferencesStore.layout === 'sidebar-nav'">
      <!-- 侧边栏 -->
      <el-aside :width="sidebarWidth" class="sidebar">
        <div class="logo">
          <h2 v-if="!preferencesStore.sidebarCollapsed">XianCore</h2>
          <h2 v-else>XC</h2>
        </div>
        <el-menu
          :key="'menu-' + menuBgColor + '-' + activeTextColor"
          :default-active="activeMenu"
          :collapse="preferencesStore.sidebarCollapsed"
          router
          :background-color="menuBgColor"
          :text-color="menuTextColor"
          :active-text-color="activeTextColor"
        >
          <el-menu-item index="/dashboard">
            <el-icon><House /></el-icon>
            <template #title>仪表盘</template>
          </el-menu-item>
          <el-menu-item index="/players">
            <el-icon><User /></el-icon>
            <template #title>玩家管理</template>
          </el-menu-item>
          <el-menu-item index="/sects">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>宗门管理</template>
          </el-menu-item>
          <el-menu-item index="/boss">
            <el-icon><Aim /></el-icon>
            <template #title>Boss管理</template>
          </el-menu-item>
          <el-menu-item index="/ranking">
            <el-icon><TrophyBase /></el-icon>
            <template #title>排行榜</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-container class="main-container">
        <!-- 顶部栏 -->
        <el-header class="header">
          <div class="header-left">
            <!-- 侧边栏折叠按钮 -->
            <el-button
              @click="preferencesStore.toggleSidebar()"
              :icon="preferencesStore.sidebarCollapsed ? Expand : Fold"
              circle
              text
            />
            <!-- 面包屑 -->
            <el-breadcrumb v-if="preferencesStore.showBreadcrumb" separator="/">
              <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <!-- 刷新按钮 -->
            <el-button @click="handleRefresh" :icon="Refresh" circle text />
            <!-- 偏好设置按钮 -->
            <el-button @click="showPreferences = true" :icon="Setting" circle text />
          </div>
        </el-header>

        <!-- 内容 -->
        <el-main class="content">
          <router-view />
        </el-main>
      </el-container>
    </template>

    <!-- 顶部导航布局 -->
    <template v-else-if="preferencesStore.layout === 'header-nav'">
      <el-container class="full-container">
        <!-- 顶部栏 -->
        <el-header class="header header-nav-header">
          <div class="header-left">
            <div class="logo-inline">
              <h2>XianCore</h2>
            </div>
            <!-- 顶部菜单 -->
            <el-menu
              :key="'header-menu-' + menuBgColor + '-' + activeTextColor"
              :default-active="activeMenu"
              mode="horizontal"
              router
              :background-color="headerBgColor"
              :text-color="headerMenuTextColor"
              :active-text-color="activeTextColor"
              class="header-menu"
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
              <el-menu-item index="/boss">
                <el-icon><Aim /></el-icon>
                <span>Boss管理</span>
              </el-menu-item>
              <el-menu-item index="/ranking">
                <el-icon><TrophyBase /></el-icon>
                <span>排行榜</span>
              </el-menu-item>
            </el-menu>
          </div>
          <div class="header-right">
            <!-- 刷新按钮 -->
            <el-button @click="handleRefresh" :icon="Refresh" circle text />
            <!-- 偏好设置按钮 -->
            <el-button @click="showPreferences = true" :icon="Setting" circle text />
          </div>
        </el-header>

        <!-- 内容 -->
        <el-main class="content">
          <router-view />
        </el-main>
      </el-container>
    </template>

    <!-- 混合导航布局 -->
    <template v-else-if="preferencesStore.layout === 'mixed-nav'">
      <el-container class="full-container">
        <!-- 顶部栏 -->
        <el-header class="header mixed-nav-header">
          <div class="header-left">
            <div class="logo-inline">
              <h2>XianCore</h2>
            </div>
          </div>
          <div class="header-right">
            <!-- 刷新按钮 -->
            <el-button @click="handleRefresh" :icon="Refresh" circle text />
            <!-- 偏好设置按钮 -->
            <el-button @click="showPreferences = true" :icon="Setting" circle text />
          </div>
        </el-header>

        <el-container>
          <!-- 侧边栏 -->
          <el-aside :width="sidebarWidth" class="sidebar mixed-sidebar">
            <el-menu
              :key="'mixed-menu-' + menuBgColor + '-' + activeTextColor"
              :default-active="activeMenu"
              :collapse="preferencesStore.sidebarCollapsed"
              router
              :background-color="menuBgColor"
              :text-color="menuTextColor"
              :active-text-color="activeTextColor"
            >
              <el-menu-item index="/dashboard">
                <el-icon><House /></el-icon>
                <template #title>仪表盘</template>
              </el-menu-item>
              <el-menu-item index="/players">
                <el-icon><User /></el-icon>
                <template #title>玩家管理</template>
              </el-menu-item>
              <el-menu-item index="/sects">
                <el-icon><OfficeBuilding /></el-icon>
                <template #title>宗门管理</template>
              </el-menu-item>
              <el-menu-item index="/boss">
                <el-icon><Aim /></el-icon>
                <template #title>Boss管理</template>
              </el-menu-item>
              <el-menu-item index="/ranking">
                <el-icon><TrophyBase /></el-icon>
                <template #title>排行榜</template>
              </el-menu-item>
            </el-menu>
          </el-aside>

          <!-- 主内容区 -->
          <el-container class="main-container">
            <!-- 面包屑栏 -->
            <div class="breadcrumb-bar" v-if="preferencesStore.showBreadcrumb">
              <el-button
                @click="preferencesStore.toggleSidebar()"
                :icon="preferencesStore.sidebarCollapsed ? Expand : Fold"
                circle
                text
                size="small"
              />
              <el-breadcrumb separator="/">
                <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
              </el-breadcrumb>
            </div>

            <!-- 内容 -->
            <el-main class="content">
              <router-view />
            </el-main>
          </el-container>
        </el-container>
      </el-container>
    </template>

    <!-- 全屏内容布局 -->
    <template v-else-if="preferencesStore.layout === 'full-content'">
      <el-container class="full-container">
        <!-- 简洁顶部栏 -->
        <el-header class="header full-content-header" height="50px">
          <div class="header-left">
            <div class="logo-inline logo-small">
              <h3>XianCore</h3>
            </div>
            <!-- 面包屑 -->
            <el-breadcrumb v-if="preferencesStore.showBreadcrumb" separator="/">
              <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <!-- 快速导航 -->
            <el-dropdown trigger="click">
              <el-button :icon="Menu" circle text />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/dashboard')">
                    <el-icon><House /></el-icon> 仪表盘
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/players')">
                    <el-icon><User /></el-icon> 玩家管理
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/sects')">
                    <el-icon><OfficeBuilding /></el-icon> 宗门管理
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/boss')">
                    <el-icon><Aim /></el-icon> Boss管理
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/ranking')">
                    <el-icon><TrophyBase /></el-icon> 排行榜
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <!-- 刷新按钮 -->
            <el-button @click="handleRefresh" :icon="Refresh" circle text />
            <!-- 偏好设置按钮 -->
            <el-button @click="showPreferences = true" :icon="Setting" circle text />
          </div>
        </el-header>

        <!-- 内容 -->
        <el-main class="content full-content">
          <router-view />
        </el-main>
      </el-container>
    </template>

    <!-- 侧边栏混合导航（默认回退） -->
    <template v-else>
      <!-- 侧边栏 -->
      <el-aside :width="sidebarWidth" class="sidebar">
        <div class="logo">
          <h2 v-if="!preferencesStore.sidebarCollapsed">XianCore</h2>
          <h2 v-else>XC</h2>
        </div>
        <el-menu
          :key="'menu-' + menuBgColor + '-' + activeTextColor"
          :default-active="activeMenu"
          :collapse="preferencesStore.sidebarCollapsed"
          router
          :background-color="menuBgColor"
          :text-color="menuTextColor"
          :active-text-color="activeTextColor"
        >
          <el-menu-item index="/dashboard">
            <el-icon><House /></el-icon>
            <template #title>仪表盘</template>
          </el-menu-item>
          <el-menu-item index="/players">
            <el-icon><User /></el-icon>
            <template #title>玩家管理</template>
          </el-menu-item>
          <el-menu-item index="/sects">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>宗门管理</template>
          </el-menu-item>
          <el-menu-item index="/boss">
            <el-icon><Aim /></el-icon>
            <template #title>Boss管理</template>
          </el-menu-item>
          <el-menu-item index="/ranking">
            <el-icon><TrophyBase /></el-icon>
            <template #title>排行榜</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-container class="main-container">
        <!-- 顶部栏 -->
        <el-header class="header">
          <div class="header-left">
            <!-- 侧边栏折叠按钮 -->
            <el-button
              @click="preferencesStore.toggleSidebar()"
              :icon="preferencesStore.sidebarCollapsed ? Expand : Fold"
              circle
              text
            />
            <!-- 面包屑 -->
            <el-breadcrumb v-if="preferencesStore.showBreadcrumb" separator="/">
              <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <!-- 刷新按钮 -->
            <el-button @click="handleRefresh" :icon="Refresh" circle text />
            <!-- 偏好设置按钮 -->
            <el-button @click="showPreferences = true" :icon="Setting" circle text />
          </div>
        </el-header>

        <!-- 内容 -->
        <el-main class="content">
          <router-view />
        </el-main>
      </el-container>
    </template>

    <!-- 偏好设置抽屉 -->
    <PreferencesDrawer v-model:visible="showPreferences" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import {
  House,
  User,
  OfficeBuilding,
  TrophyBase,
  Aim,
  Refresh,
  Setting,
  Fold,
  Expand,
  Menu,
} from '@element-plus/icons-vue';
import { usePreferencesStore } from '@/stores/preferences';
import { mixWithBlack } from '@/config/theme';
import PreferencesDrawer from '@/components/PreferencesDrawer.vue';

const route = useRoute();
const preferencesStore = usePreferencesStore();
const showPreferences = ref(false);

const activeMenu = computed(() => route.path);
const currentRouteName = computed(() => route.meta.title || '仪表盘');
const sidebarWidth = computed(() => (preferencesStore.sidebarCollapsed ? '64px' : '200px'));

// 布局相关的 class
const layoutClasses = computed(() => ({
  'dark-mode': preferencesStore.theme === 'dark',
  'layout-sidebar-nav': preferencesStore.layout === 'sidebar-nav',
  'layout-header-nav': preferencesStore.layout === 'header-nav',
  'layout-mixed-nav': preferencesStore.layout === 'mixed-nav',
  'layout-full-content': preferencesStore.layout === 'full-content',
}));

// 根据主题色和暗色模式计算侧边栏颜色
const menuBgColor = computed(() => {
  const color = preferencesStore.currentPrimaryColor;
  const isDark = preferencesStore.theme === 'dark';
  let result: string;

  if (isDark) {
    result = '#1f2937';
  } else {
    // 亮色模式：基于主题色生成深色变体
    result = mixWithBlack(color, 0.75);
  }

  console.log('[BasicLayout] menuBgColor computed:', {
    currentPrimaryColor: color,
    isDark,
    result
  });

  return result;
});
const menuTextColor = computed(() => {
  return preferencesStore.theme === 'dark' ? '#e5e7eb' : '#bfcbd9';
});
const activeTextColor = computed(() => preferencesStore.currentPrimaryColor);
const headerBgColor = computed(() => preferencesStore.theme === 'dark' ? '#1f2937' : '#ffffff');
const headerMenuTextColor = computed(() => preferencesStore.theme === 'dark' ? '#e5e7eb' : '#303133');

const handleRefresh = () => {
  location.reload();
};
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  transition: all 0.3s ease;
  background-color: var(--el-bg-color-page);
}

.full-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.sidebar {
  background-color: var(--sidebar-bg-color, #304156);
  color: var(--sidebar-text-color, #bfcbd9);
  transition: width 0.3s ease, background-color 0.3s ease;
  overflow: hidden;
}

.mixed-sidebar {
  margin-top: 0;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--sidebar-logo-bg-color, #2b3a4b);
  color: #fff;
  transition: all 0.3s ease;
}

.logo h2 {
  margin: 0;
  font-size: 20px;
  transition: all 0.3s ease;
}

.logo-inline {
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.logo-inline h2 {
  margin: 0;
  font-size: 20px;
  color: var(--el-color-primary);
}

.logo-small h3 {
  margin: 0;
  font-size: 16px;
  color: var(--el-color-primary);
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  background-color: var(--el-bg-color);
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  transition: background-color 0.3s ease;
  border-bottom: 1px solid var(--el-border-color-light);
}

.header-nav-header {
  height: 60px;
}

.header-nav-header .header-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.header-menu {
  border-bottom: none !important;
  flex: 1;
}

.mixed-nav-header {
  height: 60px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.mixed-nav-header .logo-inline h2 {
  color: var(--el-color-primary);
}

.mixed-nav-header .header-right .el-button {
  color: var(--el-text-color-primary);
}

.full-content-header {
  border-bottom: 1px solid var(--el-border-color-light);
}

.header-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.content {
  background-color: var(--el-bg-color-page);
  padding: 20px;
  overflow-y: auto;
  transition: background-color 0.3s ease;
}

.full-content {
  padding: 16px;
}

/* 暗色模式样式 - 侧边栏颜色通过 CSS 变量自动切换 */

.dark-mode .header {
  background-color: #1f2937;
  border-bottom-color: #374151;
}

.dark-mode .mixed-nav-header {
  background-color: var(--el-color-primary);
}

.dark-mode .content {
  background-color: #111827;
}

.dark-mode .breadcrumb-bar {
  background-color: #1f2937;
  border-bottom-color: #374151;
}

.dark-mode :deep(.el-card) {
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border-color: var(--el-border-color);
}

.dark-mode :deep(.el-menu) {
  border-right: none;
}

/* 确保 Element Plus 组件在暗色模式下正确显示 */
.dark-mode :deep(.el-breadcrumb__inner) {
  color: var(--el-text-color-secondary);
}

.dark-mode :deep(.el-breadcrumb__separator) {
  color: var(--el-text-color-placeholder);
}
</style>
