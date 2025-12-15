# 偏好设置功能说明

## 功能概述

XianCore Dashboard 参考 seasun-cams 项目实现了完整的偏好设置功能，允许用户自定义布局、主题和界面行为。

## 功能特性

### 1. 布局类型 📐

支持5种不同的布局模式，满足不同使用场景：

- **侧边栏导航** (sidebar-nav) - 经典的侧边栏布局，左侧菜单右侧内容（默认）
- **顶部导航** (header-nav) - 顶部菜单栏布局，适合页面较少的应用
- **混合导航** (mixed-nav) - 顶部+侧边栏混合布局
- **侧边栏混合** (sidebar-mixed-nav) - 侧边栏分组混合布局
- **全屏内容** (full-content) - 无边框全屏内容布局

每种布局都有可视化预览图标，方便选择。

### 2. 内置主题 🎨

提供14种精心设计的主题颜色预设：

| 主题名称 | 颜色值 | 说明 |
|---------|--------|------|
| 默认蓝 | #409EFF | 经典的Element Plus蓝色主题（默认） |
| 深蓝 | #1890ff | 深邃的蓝色，专业稳重 |
| 深绿 | #0e9f6e | 深绿色，自然宁静 |
| 优雅灰 | #71717a | 低调优雅的灰色 |
| 翠绿 | #67c23a | 清新的绿色 |
| 中性 | #737373 | 中性色调，百搭耐看 |
| 活力橙 | #f97316 | 充满活力的橙色 |
| 粉红 | #ec4899 | 温柔的粉红色 |
| 玫瑰 | #f43f5e | 浪漫的玫瑰色 |
| 天蓝 | #0ea5e9 | 清澈的天蓝色 |
| 石板 | #64748b | 沉稳的石板色 |
| 紫罗兰 | #8b5cf6 | 神秘的紫罗兰 |
| 明黄 | #eab308 | 明亮的黄色 |
| 锌灰 | #71717a | 现代感的锌灰色 |

**自定义颜色**: 如果预设主题不满足需求，可使用颜色选择器自定义任意颜色。

### 3. 主题模式 🌓
- **亮色模式** - 适合白天使用
- **暗色模式** - 适合夜间使用，减少眼睛疲劳
- 实时切换，无需刷新页面

### 4. 语言设置 🌏
- **简体中文** - 默认语言
- **English** - 英文界面（待完善）
- 持久化保存，下次访问自动应用

### 5. 布局设置 📋
- **显示面包屑** - 显示/隐藏顶部面包屑导航
- **启用动画效果** - 开启/关闭页面切换动画
- **侧边栏折叠** - 点击按钮折叠/展开侧边栏
  - 展开：200px 宽度，显示完整菜单文字
  - 折叠：64px 宽度，仅显示图标

### 6. 数据刷新 🔄
- **自动刷新间隔**
  - 设置范围：0-300 秒
  - 0 表示禁用自动刷新
  - 推荐设置：30-60 秒
- 自动应用到所有支持的页面（如仪表盘）

## 使用方法

### 打开偏好设置
1. 点击右上角的齿轮图标 ⚙️
2. 设置抽屉从右侧滑出

### 修改设置

#### 选择布局类型
1. 在"布局类型"区域选择你喜欢的布局
2. 每个布局都有预览图标和说明
3. 点击即可切换（需要刷新页面生效）

#### 选择主题颜色
1. 在"内置主题"区域浏览14种预设颜色
2. 点击颜色卡片即可应用
3. 如需自定义：勾选"自定义颜色"，使用颜色选择器
4. 主题颜色实时生效，无需刷新

#### 其他设置
1. 在抽屉中选择你想要的选项
2. 设置会自动保存到浏览器 localStorage
3. 点击"确定"关闭抽屉

### 重置设置
点击"重置为默认"按钮，所有设置恢复到默认值。

## 技术实现

### 状态管理
使用 Pinia store 管理偏好设置状态：

```typescript
// 使用偏好设置 store
import { usePreferencesStore } from '@/stores/preferences';

const preferencesStore = usePreferencesStore();

// 访问设置
console.log(preferencesStore.theme); // 'light' | 'dark'
console.log(preferencesStore.builtinTheme); // 'default' | 'deep-blue' | ...
console.log(preferencesStore.layout); // 'sidebar-nav' | 'header-nav' | ...
console.log(preferencesStore.primaryColor); // '#409EFF'
console.log(preferencesStore.locale); // 'zh-CN' | 'en-US'
console.log(preferencesStore.sidebarCollapsed); // boolean
console.log(preferencesStore.autoRefreshInterval); // number

// 修改设置
preferencesStore.theme = 'dark';
preferencesStore.builtinTheme = 'deep-blue';
preferencesStore.layout = 'header-nav';
preferencesStore.toggleSidebar();
preferencesStore.resetPreferences();
```

### 主题颜色配置
所有主题预设定义在 `@/config/theme.ts`：

```typescript
import { themePresets, applyThemeColor } from '@/config/theme';

// 获取主题配置
const config = themePresets['deep-blue'];
console.log(config.name); // '深蓝'
console.log(config.color); // '#1890ff'

// 手动应用主题颜色
applyThemeColor('#1890ff');
```

主题颜色会自动应用到 CSS 变量，影响所有使用 Element Plus 主色的组件。

### 持久化存储
所有设置自动保存到 `localStorage`，键名为 `xiancore-preferences`。

### 自动刷新
使用 `useAutoRefresh` Hook 实现：

```typescript
import { useAutoRefresh } from '@/hooks/useAutoRefresh';

// 在组件中使用
const fetchData = async () => {
  // 获取数据的逻辑
};

useAutoRefresh(fetchData);
```

## 默认配置

```typescript
{
  theme: 'light',                // 亮色主题
  builtinTheme: 'default',       // 默认蓝色主题
  primaryColor: '#409EFF',       // 主题色
  layout: 'sidebar-nav',         // 侧边栏导航布局
  locale: 'zh-CN',               // 简体中文
  sidebarCollapsed: false,       // 侧边栏展开
  autoRefreshInterval: 0,        // 禁用自动刷新
  showBreadcrumb: true,          // 显示面包屑
  enableAnimation: true,         // 启用动画
}
```

## 与 seasun-cams 的对比

| 功能 | seasun-cams | XianCore Dashboard |
|-----|-------------|-------------------|
| 布局类型 | ✅ 5种布局模式 | ✅ 5种布局模式 |
| 内置主题 | ✅ 15+颜色预设 | ✅ 14颜色预设 |
| 自定义颜色 | ✅ 颜色选择器 | ✅ 颜色选择器 |
| 主题切换 | ✅ 亮色/暗色 | ✅ 亮色/暗色 |
| 语言切换 | ✅ 多语言支持 | ✅ 中文/英文 |
| 布局配置 | ✅ 丰富的布局选项 | ✅ 面包屑/动画 |
| 侧边栏 | ✅ 折叠/展开 | ✅ 折叠/展开 |
| 动画效果 | ✅ 多种过渡效果 | ✅ 开启/关闭 |
| 自动刷新 | ❌ 不支持 | ✅ 自定义间隔 |
| 快捷键 | ✅ 支持 | ❌ 不支持 |
| 颜色模式 | ✅ 灰度/色弱模式 | ❌ 不支持 |

## 未来扩展

计划添加的功能：
- [ ] 完善其他4种布局模式的实现
- [ ] 全局快捷键支持
- [ ] 灰度和色弱模式
- [ ] 字体大小调整
- [ ] 表格密度设置
- [ ] 页面切换动画类型选择
- [ ] 数据导出格式偏好
- [ ] 更多主题颜色选择

## 故障排除

### 设置未生效
1. 清除浏览器缓存
2. 刷新页面（Ctrl+F5 强制刷新）
3. 检查浏览器控制台是否有错误

### 设置丢失
检查浏览器是否禁用了 localStorage。在控制台运行：
```javascript
localStorage.getItem('xiancore-preferences')
```

### 自动刷新不工作
1. 确认刷新间隔大于 0
2. 检查页面是否使用了 `useAutoRefresh` Hook
3. 查看控制台是否有 API 错误

### 主题颜色未应用
1. 检查浏览器控制台是否有CSS变量错误
2. 确认 `applyThemeColor` 函数被正确调用
3. 尝试切换到其他主题再切换回来

---

**创建日期**: 2025-12-13
**更新日期**: 2025-12-13
**版本**: 2.0.0
**维护者**: XianCore Team
