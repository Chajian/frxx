<template>
  <el-drawer
    v-model="visible"
    title="偏好设置"
    direction="rtl"
    size="360px"
    :modal="true"
  >
    <div class="preferences-content">
      <!-- 布局类型 -->
      <div class="preference-section">
        <div class="section-title">
          <el-icon><Grid /></el-icon>
          <span>布局类型</span>
        </div>
        <div class="layout-options">
          <div
            v-for="option in layoutOptions"
            :key="option.type"
            class="layout-option"
            :class="{ active: preferencesStore.layout === option.type }"
            @click="preferencesStore.setLayout(option.type as any)"
          >
            <div class="layout-icon">
              <LayoutIcon :type="option.type" />
            </div>
            <div class="layout-name">{{ option.name }}</div>
            <div class="layout-desc">{{ option.description }}</div>
          </div>
        </div>
      </div>

      <!-- 内置主题 -->
      <div class="preference-section">
        <div class="section-title">
          <el-icon><Sunny /></el-icon>
          <span>内置主题</span>
        </div>
        <div class="theme-presets">
          <div
            v-for="(config, key) in themePresets"
            :key="key"
            v-show="key !== 'custom'"
            class="theme-preset"
            :class="{ active: preferencesStore.builtinTheme === key }"
            @click="preferencesStore.setBuiltinTheme(key as any)"
          >
            <div class="theme-color" :style="{ backgroundColor: config.color }"></div>
            <div class="theme-name">{{ config.name }}</div>
          </div>
        </div>

        <!-- 自定义颜色 -->
        <div class="custom-color-section">
          <el-checkbox
            :model-value="preferencesStore.builtinTheme === 'custom'"
            @change="preferencesStore.setBuiltinTheme('custom')"
          >
            自定义颜色
          </el-checkbox>
          <el-color-picker
            v-if="preferencesStore.builtinTheme === 'custom'"
            :model-value="preferencesStore.primaryColor"
            @update:model-value="preferencesStore.setPrimaryColor($event)"
            show-alpha
            style="margin-top: 8px"
          />
        </div>
      </div>

      <!-- 主题模式 -->
      <div class="preference-section">
        <div class="section-title">
          <el-icon><Sunny /></el-icon>
          <span>主题模式</span>
        </div>
        <div class="section-content">
          <el-segmented v-model="preferencesStore.theme" :options="themeOptions" block />
        </div>
      </div>

      <!-- 语言设置 -->
      <div class="preference-section">
        <div class="section-title">
          <el-icon><Flag /></el-icon>
          <span>语言</span>
        </div>
        <div class="section-content">
          <el-segmented v-model="preferencesStore.locale" :options="localeOptions" block />
        </div>
      </div>

      <!-- 布局设置 -->
      <div class="preference-section">
        <div class="section-title">
          <el-icon><Grid /></el-icon>
          <span>布局设置</span>
        </div>
        <div class="section-content">
          <div class="preference-item">
            <span>显示面包屑</span>
            <el-switch v-model="preferencesStore.showBreadcrumb" />
          </div>
          <div class="preference-item">
            <span>启用动画效果</span>
            <el-switch v-model="preferencesStore.enableAnimation" />
          </div>
        </div>
      </div>

      <!-- 数据刷新 -->
      <div class="preference-section">
        <div class="section-title">
          <el-icon><Refresh /></el-icon>
          <span>数据刷新</span>
        </div>
        <div class="section-content">
          <div class="preference-item">
            <span>自动刷新间隔（秒）</span>
          </div>
          <el-input-number
            v-model="preferencesStore.autoRefreshInterval"
            :min="0"
            :max="300"
            :step="10"
            style="width: 100%"
          />
          <div class="preference-tip">0 表示禁用自动刷新</div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="preference-actions">
        <el-button type="danger" plain @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置为默认
        </el-button>
        <el-button type="primary" @click="handleClose">
          <el-icon><Check /></el-icon>
          确定
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Sunny, Flag, Grid, Refresh, RefreshLeft, Check } from '@element-plus/icons-vue';
import { usePreferencesStore } from '@/stores/preferences';
import { themePresets, layoutOptions } from '@/config/theme';
import { ElMessage } from 'element-plus';
import LayoutIcon from '@/components/LayoutIcon.vue';

const visible = defineModel<boolean>('visible', { default: false });

const preferencesStore = usePreferencesStore();

const themeOptions = [
  { label: '亮色', value: 'light' },
  { label: '暗色', value: 'dark' },
];

const localeOptions = [
  { label: '简体中文', value: 'zh-CN' },
  { label: 'English', value: 'en-US' },
];

const handleReset = () => {
  preferencesStore.resetPreferences();
  ElMessage.success('已重置为默认设置');
};

const handleClose = () => {
  visible.value = false;
};
</script>

<style scoped>
.preferences-content {
  padding: 0 4px;
}

.preference-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.preference-section:last-of-type {
  border-bottom: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
}

.section-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preference-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.preference-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: -4px;
}

/* 布局选项样式 */
.layout-options {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.layout-option {
  padding: 12px;
  border: 2px solid var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.layout-option:hover {
  border-color: var(--el-color-primary);
  background-color: var(--el-fill-color-light);
}

.layout-option.active {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.layout-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
}

.layout-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
  text-align: center;
}

.layout-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

/* 主题预设样式 */
.theme-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.theme-preset {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.theme-preset:hover {
  background-color: var(--el-fill-color-light);
}

.theme-preset.active {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.theme-color {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.theme-name {
  font-size: 12px;
  color: var(--el-text-color-regular);
  text-align: center;
}

.custom-color-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.preference-actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.preference-actions .el-button {
  flex: 1;
}
</style>
