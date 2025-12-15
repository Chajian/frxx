<template>
  <div class="boss-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="刷新点总数" :value="stats.totalSpawnPoints" />
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="已启用" :value="stats.enabledSpawnPoints">
            <template #suffix>
              <span class="stat-suffix">/ {{ stats.totalSpawnPoints }}</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="MythicMobs 怪物" :value="mythicMobs.length" />
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="MythicMobs 物品" :value="items.length" />
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="掉落表" :value="dropTables.length" />
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="今日击杀" :value="stats.todayKills">
            <template #suffix>
              <span class="stat-suffix">/ {{ stats.totalKills }}</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- MythicMobs 状态提示 -->
    <el-alert
      v-if="!mythicMobsStatus.configured"
      title="MythicMobs 配置未设置"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
    >
      请在后端 .env 文件中设置 MYTHICMOBS_MOBS_PATH 环境变量，指向 MythicMobs 的 Mobs 目录。
    </el-alert>

    <!-- Tab 切换 -->
    <el-card>
      <el-tabs v-model="activeTab">
        <!-- 刷新点管理 Tab -->
        <el-tab-pane label="刷新点管理" name="spawn-points">
          <template #label>
            <span><el-icon><Location /></el-icon> 刷新点管理</span>
          </template>

          <div class="tab-header">
            <div class="filter-bar">
              <el-input
                v-model="searchText"
                placeholder="搜索 ID / 名称 / Boss类型"
                style="width: 250px"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-select v-model="filterTier" placeholder="等级筛选" clearable style="width: 120px">
                <el-option
                  v-for="tier in BOSS_TIERS"
                  :key="tier.value"
                  :label="tier.label"
                  :value="tier.value"
                />
              </el-select>
              <el-select v-model="filterEnabled" placeholder="状态筛选" clearable style="width: 120px">
                <el-option label="已启用" :value="true" />
                <el-option label="已禁用" :value="false" />
              </el-select>
            </div>
            <div class="header-actions">
              <el-button type="primary" @click="openCreateDialog">
                <el-icon><Plus /></el-icon>
                添加刷新点
              </el-button>
              <el-button @click="refreshData">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>

          <!-- 刷新点表格 -->
          <el-table :data="filteredSpawnPoints" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="150" sortable />
            <el-table-column prop="name" label="名称" width="150" />
            <el-table-column label="Boss 类型" width="200">
              <template #default="{ row }">
                <div class="mob-info" @click="showMobDetail(row.mythicMobId)" style="cursor: pointer;">
                  <span class="mob-id">{{ row.mythicMobId }}</span>
                  <span v-if="row.mythicMobInfo" class="mob-name">
                    {{ row.mythicMobInfo.displayName }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="等级" width="100" sortable sort-by="tier">
              <template #default="{ row }">
                <el-tag :color="getTierColor(row.tier)" effect="dark" size="small">
                  {{ getTierLabel(row.tier) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="位置" width="180">
              <template #default="{ row }">
                <span class="location-text">
                  {{ row.world }}: {{ row.x }}, {{ row.y }}, {{ row.z }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="冷却时间" width="120">
              <template #default="{ row }">
                {{ formatCooldown(Number(row.cooldownSeconds)) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-switch
                  v-model="row.enabled"
                  @change="handleToggleEnabled(row)"
                  :loading="row._toggling"
                />
              </template>
            </el-table-column>
            <el-table-column label="剩余冷却" width="120">
              <template #default="{ row }">
                <template v-if="row.remainingCooldown > 0">
                  <span class="cooldown-text">{{ formatCooldown(row.remainingCooldown) }}</span>
                </template>
                <template v-else>
                  <el-tag type="success" size="small">就绪</el-tag>
                </template>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- MythicMobs 怪物列表 Tab -->
        <el-tab-pane label="MythicMobs 怪物" name="mythicmobs">
          <template #label>
            <span><el-icon><Monster /></el-icon> MythicMobs 怪物</span>
          </template>

          <div class="tab-header">
            <div class="filter-bar">
              <el-input
                v-model="mobSearchText"
                placeholder="搜索怪物 ID / 名称"
                style="width: 250px"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-select v-model="mobFilterType" placeholder="类型筛选" clearable style="width: 150px">
                <el-option
                  v-for="(count, type) in mobTypeStats"
                  :key="type"
                  :label="`${type} (${count})`"
                  :value="type"
                />
              </el-select>
            </div>
            <div class="header-actions">
              <el-button @click="refreshMythicMobs" :loading="mobsLoading">
                <el-icon><Refresh /></el-icon>
                刷新缓存
              </el-button>
            </div>
          </div>

          <!-- 怪物列表 -->
          <el-table :data="filteredMythicMobs" v-loading="mobsLoading" stripe style="width: 100%">
            <el-table-column prop="id" label="怪物 ID" width="200" sortable />
            <el-table-column prop="displayName" label="显示名称" width="200" />
            <el-table-column prop="type" label="实体类型" width="150" sortable />
            <el-table-column label="血量" width="120" sortable sort-by="health">
              <template #default="{ row }">
                <span class="health-text">{{ row.health }}</span>
              </template>
            </el-table-column>
            <el-table-column label="伤害" width="100" sortable sort-by="damage">
              <template #default="{ row }">
                <span class="damage-text">{{ row.damage }}</span>
              </template>
            </el-table-column>
            <el-table-column label="护甲" width="100" sortable sort-by="armor">
              <template #default="{ row }">
                {{ row.armor }}
              </template>
            </el-table-column>
            <el-table-column label="技能数" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.skills?.length" type="info" size="small">
                  {{ row.skills.length }} 个
                </el-tag>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="fileName" label="配置文件" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="showMobDetail(row.id)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- MythicMobs 物品列表 Tab -->
        <el-tab-pane label="MythicMobs 物品" name="items">
          <template #label>
            <span><el-icon><Box /></el-icon> MythicMobs 物品</span>
          </template>

          <div class="tab-header">
            <div class="filter-bar">
              <el-input
                v-model="itemSearchText"
                placeholder="搜索物品 ID / 名称 / 材质"
                style="width: 250px"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-select v-model="itemFilterMaterial" placeholder="材质筛选" clearable style="width: 180px">
                <el-option
                  v-for="(count, material) in itemMaterialStats"
                  :key="material"
                  :label="`${material} (${count})`"
                  :value="material"
                />
              </el-select>
            </div>
            <div class="header-actions">
              <el-button @click="refreshItems" :loading="itemsLoading">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>

          <!-- 物品列表 -->
          <el-table :data="filteredItems" v-loading="itemsLoading" stripe style="width: 100%">
            <el-table-column prop="id" label="物品 ID" width="200" sortable />
            <el-table-column prop="displayName" label="显示名称" width="200" />
            <el-table-column prop="material" label="材质" width="180" sortable />
            <el-table-column label="自定义模型" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.customModelData" type="info" size="small">
                  {{ row.customModelData }}
                </el-tag>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column label="附魔" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.enchantments?.length" type="warning" size="small">
                  {{ row.enchantments.length }} 个
                </el-tag>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column label="属性" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.attributes?.length" type="success" size="small">
                  {{ row.attributes.length }} 个
                </el-tag>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column label="Lore" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.lore?.length" type="info" size="small">
                  {{ row.lore.length }} 行
                </el-tag>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="fileName" label="配置文件" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="showItemDetail(row.id)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 掉落表 Tab -->
        <el-tab-pane label="掉落表" name="droptables">
          <template #label>
            <span><el-icon><Coin /></el-icon> 掉落表</span>
          </template>

          <div class="tab-header">
            <div class="filter-bar">
              <el-input
                v-model="dropTableSearchText"
                placeholder="搜索掉落表 ID"
                style="width: 250px"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="header-actions">
              <el-button @click="fetchDropTables" :loading="dropTablesLoading">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>

          <!-- 掉落表列表 -->
          <el-table :data="filteredDropTables" v-loading="dropTablesLoading" stripe style="width: 100%">
            <el-table-column prop="id" label="掉落表 ID" width="250" sortable />
            <el-table-column label="掉落物数量" width="150">
              <template #default="{ row }">
                <el-tag type="primary" size="small">
                  {{ row.drops?.length || 0 }} 个
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="总权重" width="150">
              <template #default="{ row }">
                <span v-if="row.totalWeight">{{ row.totalWeight }}</span>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column label="条件数量" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.conditions?.length" type="warning" size="small">
                  {{ row.conditions.length }} 个
                </el-tag>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column label="掉落物预览">
              <template #default="{ row }">
                <div class="drops-preview">
                  <el-tag
                    v-for="(drop, idx) in row.drops?.slice(0, 3)"
                    :key="idx"
                    size="small"
                    style="margin-right: 4px; margin-bottom: 2px;"
                  >
                    {{ drop.item || drop.raw }}
                  </el-tag>
                  <span v-if="row.drops?.length > 3" class="more-drops">
                    +{{ row.drops.length - 3 }} 更多
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="showDropTableDetail(row)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 创建/编辑刷新点对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑刷新点' : '创建刷新点'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        v-loading="dialogLoading"
      >
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="ID" prop="id">
              <el-input
                v-model="formData.id"
                placeholder="唯一标识符"
                :disabled="isEditing"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formData.name" placeholder="显示名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="刷新点描述"
          />
        </el-form-item>

        <el-divider content-position="left">Boss 配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Boss 类型" prop="mythicMobId">
              <el-select
                v-model="formData.mythicMobId"
                placeholder="选择 MythicMobs 怪物"
                filterable
                :loading="mobsLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="mob in mythicMobs"
                  :key="mob.id"
                  :label="`${mob.displayName} (${mob.id})`"
                  :value="mob.id"
                >
                  <div class="mob-option">
                    <span class="mob-option-name">{{ mob.displayName }}</span>
                    <span class="mob-option-id">{{ mob.id }}</span>
                    <span class="mob-option-health">HP: {{ mob.health }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Boss 等级" prop="tier">
              <el-select v-model="formData.tier" style="width: 100%">
                <el-option
                  v-for="tier in BOSS_TIERS"
                  :key="tier.value"
                  :label="tier.label"
                  :value="tier.value"
                >
                  <span :style="{ color: tier.color, fontWeight: 'bold' }">{{ tier.label }}</span>
                  <span style="color: #999; margin-left: 8px; font-size: 12px">
                    {{ tier.description }}
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">位置配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="世界" prop="world">
              <el-input v-model="formData.world" placeholder="world" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生成模式">
              <el-select v-model="formData.spawnMode" style="width: 100%">
                <el-option
                  v-for="mode in SPAWN_MODES"
                  :key="mode.value"
                  :label="mode.label"
                  :value="mode.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="X" prop="x">
              <el-input-number v-model="formData.x" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Y" prop="y">
              <el-input-number v-model="formData.y" :min="0" :max="320" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Z" prop="z">
              <el-input-number v-model="formData.z" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="formData.spawnMode !== 'fixed'">
          <el-col :span="12">
            <el-form-item label="生成半径">
              <el-input-number v-model="formData.spawnRadius" :min="0" :max="500" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="随机偏移">
              <el-input-number v-model="formData.randomRadius" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">刷新规则</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="冷却时间">
              <el-input-number
                v-model="formData.cooldownSeconds"
                :min="60"
                :step="300"
                style="width: 100%"
              />
              <div class="form-item-hint">{{ formatCooldown(formData.cooldownSeconds || 0) }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大数量">
              <el-input-number v-model="formData.maxCount" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预警时间">
              <el-input-number v-model="formData.preSpawnWarning" :min="0" :max="300" style="width: 100%" />
              <div class="form-item-hint">生成前 {{ formData.preSpawnWarning || 0 }} 秒发送警告</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-switch v-model="formData.enabled" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEditing ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 怪物详情对话框 -->
    <el-dialog
      v-model="mobDetailVisible"
      :title="mobDetail?.displayName || '怪物详情'"
      width="900px"
    >
      <div v-if="mobDetailLoading" class="detail-loading">
        <el-skeleton :rows="10" animated />
      </div>
      <div v-else-if="mobDetail" class="mob-detail">
        <!-- 模板继承信息 -->
        <div v-if="mobDetail.templateInfo?.parent || mobDetail.templateInfo?.children?.length" class="detail-section template-section">
          <h4>模板继承</h4>
          <div class="template-chain">
            <template v-if="mobDetail.templateInfo?.parent">
              <el-tag type="warning" size="small">父模板: {{ mobDetail.templateInfo.parent }}</el-tag>
              <span class="chain-arrow">→</span>
            </template>
            <el-tag type="primary" size="small">{{ mobDetail.id }}</el-tag>
            <template v-if="mobDetail.templateInfo?.children?.length">
              <span class="chain-arrow">→</span>
              <el-tag v-for="child in mobDetail.templateInfo.children" :key="child" type="success" size="small" style="margin-left: 4px;">
                {{ child }}
              </el-tag>
            </template>
          </div>
          <div v-if="mobDetail.templateInfo?.depth" class="template-depth">
            继承深度: {{ mobDetail.templateInfo.depth }}
          </div>
        </div>

        <!-- 基础信息 -->
        <el-descriptions :column="3" border>
          <el-descriptions-item label="怪物 ID">{{ mobDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="显示名称">{{ mobDetail.displayName }}</el-descriptions-item>
          <el-descriptions-item label="实体类型">{{ mobDetail.type }}</el-descriptions-item>
          <el-descriptions-item label="血量">
            <span class="health-text">{{ mobDetail.health }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="伤害">
            <span class="damage-text">{{ mobDetail.damage }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="护甲">{{ mobDetail.armor }}</el-descriptions-item>
          <el-descriptions-item label="配置文件" :span="3">{{ mobDetail.fileName }}</el-descriptions-item>
        </el-descriptions>

        <!-- Boss 血条 -->
        <div v-if="mobDetail.bossBar" class="detail-section">
          <h4>Boss 血条</h4>
          <el-tag type="success">已启用</el-tag>
          <span v-if="mobDetail.bossBar.title" style="margin-left: 10px;">
            标题: {{ mobDetail.bossBar.title }}
          </span>
          <span v-if="mobDetail.bossBar.color" style="margin-left: 10px;">
            颜色: {{ mobDetail.bossBar.color }}
          </span>
        </div>

        <!-- 装备 -->
        <div v-if="mobDetail.equipment?.length" class="detail-section">
          <h4>装备配置</h4>
          <el-table :data="mobDetail.equipment" size="small" stripe>
            <el-table-column prop="slot" label="槽位" width="120" />
            <el-table-column prop="item" label="物品" />
          </el-table>
        </div>

        <!-- 技能 (增强版) -->
        <div v-if="mobDetail.parsedSkills?.length" class="detail-section">
          <h4>技能列表 ({{ mobDetail.parsedSkills.length }} 个)</h4>
          <el-table :data="mobDetail.parsedSkills" size="small" stripe max-height="300">
            <el-table-column label="技能机制" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ row.mechanic || '未解析' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="触发器" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.trigger" type="info" size="small">@{{ row.trigger }}</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="目标" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.targetSelector" type="warning" size="small">?{{ row.targetSelector }}</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="参数">
              <template #default="{ row }">
                <template v-if="row.params?.length">
                  <el-tag v-for="(param, idx) in row.params.slice(0, 3)" :key="idx" size="small" type="info" style="margin-right: 4px;">
                    {{ param.key }}={{ param.value }}
                  </el-tag>
                  <span v-if="row.params.length > 3" class="more-params">+{{ row.params.length - 3 }}</span>
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="条件" width="120">
              <template #default="{ row }">
                <template v-if="row.parsedConditions?.length">
                  <el-tag v-for="(cond, idx) in row.parsedConditions.slice(0, 2)" :key="idx" :type="cond.negated ? 'danger' : 'success'" size="small" style="margin-right: 4px;">
                    {{ cond.negated ? '!' : '' }}{{ cond.type }}
                  </el-tag>
                  <span v-if="row.parsedConditions.length > 2">+{{ row.parsedConditions.length - 2 }}</span>
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 技能组 -->
        <div v-if="mobDetail.skillGroups?.length" class="detail-section">
          <h4>引用的技能组 ({{ mobDetail.skillGroups.length }} 个)</h4>
          <el-collapse>
            <el-collapse-item v-for="group in mobDetail.skillGroups" :key="group.id" :title="group.id">
              <div v-if="group.cooldown" class="skill-group-info">
                <el-tag size="small">冷却: {{ group.cooldown }}s</el-tag>
              </div>
              <el-table :data="group.skills" size="small" stripe>
                <el-table-column label="机制" width="100">
                  <template #default="{ row }">
                    <el-tag size="small">{{ row.mechanic || '-' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="触发器" width="100">
                  <template #default="{ row }">
                    <span v-if="row.trigger">@{{ row.trigger }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="原始配置">
                  <template #default="{ row }">
                    <code class="skill-raw">{{ row.raw }}</code>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </div>

        <!-- 掉落表 (可编辑版) -->
        <div class="detail-section">
          <div class="section-header">
            <h4>掉落配置</h4>
            <el-button type="primary" size="small" @click="addMobDrop">
              <el-icon><Plus /></el-icon>
              添加掉落
            </el-button>
          </div>

          <!-- 引用的掉落表 -->
          <div v-if="mobDetail.dropsTable" class="drops-table-ref">
            <el-tag type="warning">引用掉落表: {{ mobDetail.dropsTable }}</el-tag>
            <div v-if="mobDetail.expandedDropsTable" class="expanded-drops">
              <p class="expanded-label">掉落表内容 (只读):</p>
              <el-table :data="mobDetail.expandedDropsTable.drops" size="small" stripe>
                <el-table-column label="物品" width="200">
                  <template #default="{ row }">
                    {{ row.item || row.raw }}
                  </template>
                </el-table-column>
                <el-table-column label="数量" width="100">
                  <template #default="{ row }">
                    {{ row.amount || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="概率" width="100">
                  <template #default="{ row }">
                    <span v-if="row.chance !== undefined">{{ (row.chance * 100).toFixed(1) }}%</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- 可编辑的掉落列表 -->
          <div v-if="editableDrops.length > 0" class="editable-drops">
            <p class="drops-label">直接掉落 (可编辑):</p>
            <el-table :data="editableDrops" size="small" stripe>
              <el-table-column label="类型" width="140">
                <template #default="{ row }">
                  <el-select v-model="row.type" size="small" style="width: 100%">
                    <el-option
                      v-for="dropType in DROP_TYPES"
                      :key="dropType.value"
                      :label="dropType.label"
                      :value="dropType.value"
                    >
                      <span style="display: flex; align-items: center;">
                        <el-icon style="margin-right: 6px;"><component :is="dropType.icon" /></el-icon>
                        {{ dropType.label }}
                      </span>
                    </el-option>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="物品/掉落表" width="280">
                <template #default="{ row }">
                  <!-- MythicMobs 物品 -->
                  <el-select
                    v-if="row.type === 'mythicitem'"
                    v-model="row.item"
                    filterable
                    allow-create
                    default-first-option
                    placeholder="选择或输入 MythicMobs 物品"
                    size="small"
                    style="width: 100%"
                  >
                    <template #prefix>
                      <el-icon><Box /></el-icon>
                    </template>
                    <el-option
                      v-for="item in items"
                      :key="item.id"
                      :label="`${item.displayName} (${item.id})`"
                      :value="item.id"
                    >
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>{{ item.displayName }}</span>
                        <span style="color: var(--el-text-color-secondary); font-size: 12px;">{{ item.id }}</span>
                      </div>
                    </el-option>
                  </el-select>

                  <!-- 掉落表 -->
                  <el-select
                    v-else-if="row.type === 'droptable'"
                    v-model="row.item"
                    filterable
                    allow-create
                    default-first-option
                    placeholder="选择或输入掉落表"
                    size="small"
                    style="width: 100%"
                  >
                    <template #prefix>
                      <el-icon><Coin /></el-icon>
                    </template>
                    <el-option
                      v-for="dt in dropTables"
                      :key="dt.id"
                      :label="dt.id"
                      :value="dt.id"
                    >
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>{{ dt.id }}</span>
                        <el-tag size="small" type="info">{{ dt.drops?.length || 0 }} 个掉落物</el-tag>
                      </div>
                    </el-option>
                  </el-select>

                  <!-- 原版物品 -->
                  <el-select
                    v-else
                    v-model="row.item"
                    filterable
                    allow-create
                    default-first-option
                    placeholder="选择或输入原版物品"
                    size="small"
                    style="width: 100%"
                  >
                    <template #prefix>
                      <el-icon><Present /></el-icon>
                    </template>
                    <el-option
                      v-for="vanillaItem in VANILLA_ITEMS"
                      :key="vanillaItem.value"
                      :label="vanillaItem.label"
                      :value="vanillaItem.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="数量" width="100">
                <template #default="{ row }">
                  <el-input v-model="row.amount" placeholder="1" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="概率" width="120">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.chance"
                    :min="0"
                    :max="1"
                    :step="0.1"
                    :precision="2"
                    size="small"
                    style="width: 100%"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button type="danger" link size="small" @click="removeMobDrop($index)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="drops-actions">
              <el-button type="success" size="small" @click="saveMobDrops" :loading="savingDrops">
                保存掉落配置
              </el-button>
            </div>
          </div>
          <el-empty v-else-if="!mobDetail.dropsTable" description="暂无掉落配置" :image-size="60" />
        </div>

        <!-- AI 行为 -->
        <div v-if="mobDetail.aiGoals?.length || mobDetail.aiTargets?.length" class="detail-section">
          <h4>AI 行为</h4>
          <el-row :gutter="20">
            <el-col :span="12" v-if="mobDetail.aiGoals?.length">
              <p><strong>目标选择器:</strong></p>
              <ul class="ai-list">
                <li v-for="(goal, index) in mobDetail.aiGoals" :key="index">{{ goal }}</li>
              </ul>
            </el-col>
            <el-col :span="12" v-if="mobDetail.aiTargets?.length">
              <p><strong>攻击目标:</strong></p>
              <ul class="ai-list">
                <li v-for="(target, index) in mobDetail.aiTargets" :key="index">{{ target }}</li>
              </ul>
            </el-col>
          </el-row>
        </div>

        <!-- 其他选项 -->
        <div v-if="hasOtherOptions" class="detail-section">
          <h4>其他选项</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item v-if="mobDetail.faction" label="派系">
              {{ mobDetail.faction }}
            </el-descriptions-item>
            <el-descriptions-item v-if="mobDetail.disguise" label="伪装">
              {{ mobDetail.disguise }}
            </el-descriptions-item>
            <el-descriptions-item v-if="mobDetail.followRange" label="跟随范围">
              {{ mobDetail.followRange }}
            </el-descriptions-item>
            <el-descriptions-item v-if="mobDetail.hearingRange" label="听力范围">
              {{ mobDetail.hearingRange }}
            </el-descriptions-item>
            <el-descriptions-item v-if="mobDetail.preventOtherDrops !== undefined" label="阻止其他掉落">
              {{ mobDetail.preventOtherDrops ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="mobDetail.preventRandomEquipment !== undefined" label="阻止随机装备">
              {{ mobDetail.preventRandomEquipment ? '是' : '否' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <template #footer>
        <el-button @click="openYamlEditor" type="warning">
          <el-icon><Edit /></el-icon>
          编辑配置
        </el-button>
        <el-button @click="mobDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- YAML 编辑对话框 -->
    <el-dialog
      v-model="yamlEditorVisible"
      title="编辑怪物配置"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 15px;"
      >
        直接编辑 YAML 配置。修改将保存到原始配置文件。
      </el-alert>
      <el-input
        v-model="yamlContent"
        type="textarea"
        :rows="20"
        placeholder="YAML 配置"
        class="yaml-editor"
      />
      <div v-if="yamlError" class="yaml-error">
        <el-alert type="error" :title="yamlError" :closable="false" />
      </div>
      <template #footer>
        <el-button @click="validateYamlContent">验证</el-button>
        <el-button @click="yamlEditorVisible = false">取消</el-button>
        <el-button type="primary" @click="saveYamlConfig" :loading="yamlSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 物品详情对话框 -->
    <el-dialog
      v-model="itemDetailVisible"
      :title="itemDetail?.displayName || '物品详情'"
      width="700px"
    >
      <div v-if="itemDetailLoading" class="detail-loading">
        <el-skeleton :rows="8" animated />
      </div>
      <div v-else-if="itemDetail" class="item-detail">
        <!-- 基础信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="物品 ID">{{ itemDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="显示名称">{{ itemDetail.displayName }}</el-descriptions-item>
          <el-descriptions-item label="材质">{{ itemDetail.material }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ itemDetail.amount || 1 }}</el-descriptions-item>
          <el-descriptions-item v-if="itemDetail.customModelData" label="自定义模型">
            {{ itemDetail.customModelData }}
          </el-descriptions-item>
          <el-descriptions-item v-if="itemDetail.unbreakable" label="不可破坏">
            <el-tag type="success" size="small">是</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="itemDetail.color" label="颜色">
            <span class="color-preview" :style="{ backgroundColor: itemDetail.color }"></span>
            {{ itemDetail.color }}
          </el-descriptions-item>
          <el-descriptions-item label="配置文件" :span="2">{{ itemDetail.fileName }}</el-descriptions-item>
        </el-descriptions>

        <!-- Lore -->
        <div v-if="itemDetail.lore?.length" class="detail-section">
          <h4>Lore (描述)</h4>
          <div class="lore-list">
            <div v-for="(line, index) in itemDetail.lore" :key="index" class="lore-line">
              {{ line }}
            </div>
          </div>
        </div>

        <!-- 附魔 -->
        <div v-if="itemDetail.enchantments?.length" class="detail-section">
          <h4>附魔 ({{ itemDetail.enchantments.length }} 个)</h4>
          <el-table :data="itemDetail.enchantments" size="small" stripe>
            <el-table-column prop="enchantment" label="附魔类型" />
            <el-table-column prop="level" label="等级" width="100">
              <template #default="{ row }">
                <el-tag type="warning" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 属性修饰符 -->
        <div v-if="itemDetail.attributes?.length" class="detail-section">
          <h4>属性修饰符 ({{ itemDetail.attributes.length }} 个)</h4>
          <el-table :data="itemDetail.attributes" size="small" stripe>
            <el-table-column prop="attribute" label="属性" />
            <el-table-column prop="amount" label="数值" width="100">
              <template #default="{ row }">
                <span :class="row.amount >= 0 ? 'positive-value' : 'negative-value'">
                  {{ row.amount >= 0 ? '+' : '' }}{{ row.amount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="operation" label="操作" width="100" />
            <el-table-column prop="slot" label="槽位" width="100" />
          </el-table>
        </div>

        <!-- 药水效果 -->
        <div v-if="itemDetail.potionEffects?.length" class="detail-section">
          <h4>药水效果</h4>
          <el-tag v-for="(effect, index) in itemDetail.potionEffects" :key="index" style="margin-right: 8px; margin-bottom: 4px;">
            {{ effect }}
          </el-tag>
        </div>

        <!-- 隐藏标志 -->
        <div v-if="itemDetail.hideFlags?.length" class="detail-section">
          <h4>隐藏标志</h4>
          <el-tag v-for="(flag, index) in itemDetail.hideFlags" :key="index" type="info" style="margin-right: 8px;">
            {{ flag }}
          </el-tag>
        </div>

        <!-- 头颅材质 -->
        <div v-if="itemDetail.skullTexture" class="detail-section">
          <h4>头颅材质</h4>
          <code class="skull-texture">{{ itemDetail.skullTexture }}</code>
        </div>

        <!-- NBT 数据 -->
        <div v-if="itemDetail.nbt && Object.keys(itemDetail.nbt).length" class="detail-section">
          <h4>NBT 数据</h4>
          <pre class="nbt-data">{{ JSON.stringify(itemDetail.nbt, null, 2) }}</pre>
        </div>
      </div>

      <template #footer>
        <el-button @click="openItemYamlEditor" type="warning">
          <el-icon><Edit /></el-icon>
          编辑配置
        </el-button>
        <el-button @click="itemDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 物品 YAML 编辑对话框 -->
    <el-dialog
      v-model="itemYamlEditorVisible"
      title="编辑物品配置"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 15px;"
      >
        直接编辑 YAML 配置。修改将保存到原始配置文件。
      </el-alert>
      <el-input
        v-model="itemYamlContent"
        type="textarea"
        :rows="20"
        placeholder="YAML 配置"
        class="yaml-editor"
      />
      <div v-if="itemYamlError" class="yaml-error">
        <el-alert type="error" :title="itemYamlError" :closable="false" />
      </div>
      <template #footer>
        <el-button @click="validateItemYamlContent">验证</el-button>
        <el-button @click="itemYamlEditorVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItemYamlConfig" :loading="itemYamlSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 掉落表详情对话框 -->
    <el-dialog
      v-model="dropTableDetailVisible"
      :title="`掉落表详情: ${dropTableDetail?.id || ''}`"
      width="800px"
    >
      <div v-if="dropTableDetail" class="droptable-detail">
        <!-- 基础信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="掉落表 ID">{{ dropTableDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="掉落物数量">
            <el-tag type="primary" size="small">{{ dropTableDetail.drops?.length || 0 }} 个</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="dropTableDetail.totalWeight" label="总权重">
            {{ dropTableDetail.totalWeight }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 条件列表 -->
        <div v-if="dropTableDetail.conditions?.length" class="detail-section">
          <h4>触发条件 ({{ dropTableDetail.conditions.length }} 个)</h4>
          <el-table :data="dropTableDetail.conditions" size="small" stripe>
            <el-table-column label="条件类型" width="150">
              <template #default="{ row }">
                <el-tag :type="row.negated ? 'danger' : 'success'" size="small">
                  {{ row.negated ? '!' : '' }}{{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="参数">
              <template #default="{ row }">
                <template v-if="row.params?.length">
                  <el-tag
                    v-for="(param, idx) in row.params"
                    :key="idx"
                    size="small"
                    type="info"
                    style="margin-right: 4px;"
                  >
                    {{ param.key }}={{ param.value }}
                  </el-tag>
                </template>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column label="原始配置">
              <template #default="{ row }">
                <code class="drop-raw">{{ row.raw }}</code>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 掉落物列表 -->
        <div v-if="dropTableDetail.drops?.length" class="detail-section">
          <h4>掉落物列表 ({{ dropTableDetail.drops.length }} 个)</h4>
          <el-table :data="dropTableDetail.drops" size="small" stripe max-height="400">
            <el-table-column label="物品" width="200">
              <template #default="{ row }">
                <span class="drop-item">{{ row.item || row.raw }}</span>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="100">
              <template #default="{ row }">
                <span v-if="row.amount">{{ row.amount }}</span>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column label="概率" width="120">
              <template #default="{ row }">
                <template v-if="row.chance !== undefined">
                  <el-progress
                    :percentage="Math.min(row.chance * 100, 100)"
                    :format="() => `${(row.chance * 100).toFixed(1)}%`"
                    :stroke-width="10"
                    style="width: 80px;"
                  />
                </template>
                <span v-else class="no-data">-</span>
              </template>
            </el-table-column>
            <el-table-column label="原始配置">
              <template #default="{ row }">
                <code class="drop-raw">{{ row.raw }}</code>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="dropTableDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Plus, Refresh, Search, Location, View, Edit, Box, Coin, Delete, Present } from '@element-plus/icons-vue';
import {
  bossApi,
  type BossSpawnPoint,
  type BossStats,
  type MythicMobInfo,
  type MythicMobDetailInfo,
  type MythicMobsStatus,
  type MythicItemInfo,
  type MythicItemDetailInfo,
  type MythicDropTableInfo,
  type CreateSpawnPointInput,
  BOSS_TIERS,
  SPAWN_MODES,
  formatCooldown,
  getTierColor,
  getTierLabel,
} from '@/api/boss';

// 自定义 Monster 图标组件
const Monster = {
  template: `<svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M832 512a32 32 0 1 1 64 0v320a32 32 0 0 1-32 32H160a32 32 0 0 1-32-32V512a32 32 0 0 1 64 0v288h640V512zM512 128a160 160 0 0 1 138.24 240H672a32 32 0 0 1 32 32v128a32 32 0 0 1-64 0V432H384v96a32 32 0 0 1-64 0V400a32 32 0 0 1 32-32h21.76A160 160 0 0 1 512 128zm0 64a96 96 0 1 0 0 192 96 96 0 0 0 0-192z"/></svg>`
};

// Tab 状态
const activeTab = ref('spawn-points');

// 加载状态
const loading = ref(false);
const dialogLoading = ref(false);
const submitting = ref(false);
const mobsLoading = ref(false);
const mobDetailLoading = ref(false);
const dialogVisible = ref(false);
const mobDetailVisible = ref(false);
const isEditing = ref(false);

// 数据
const spawnPoints = ref<BossSpawnPoint[]>([]);
const stats = ref<BossStats>({
  totalSpawnPoints: 0,
  enabledSpawnPoints: 0,
  disabledSpawnPoints: 0,
  activeBosses: 0,
  totalKills: 0,
  todayKills: 0,
  tierDistribution: {},
});
const mythicMobs = ref<MythicMobInfo[]>([]);
const mythicMobsStatus = ref<MythicMobsStatus>({
  configured: true,
  path: '',
  cacheSize: 0,
  cacheAge: -1,
});
const mobDetail = ref<MythicMobDetailInfo | null>(null);
const mobTypeStats = ref<Record<string, number>>({});

// 筛选 - 刷新点
const searchText = ref('');
const filterTier = ref<number | null>(null);
const filterEnabled = ref<boolean | null>(null);

// 筛选 - MythicMobs
const mobSearchText = ref('');
const mobFilterType = ref<string | null>(null);

// Items 相关状态
const itemsLoading = ref(false);
const items = ref<MythicItemInfo[]>([]);
const itemMaterialStats = ref<Record<string, number>>({});
const itemSearchText = ref('');
const itemFilterMaterial = ref<string | null>(null);
const itemDetailVisible = ref(false);
const itemDetailLoading = ref(false);
const itemDetail = ref<MythicItemDetailInfo | null>(null);

// DropTables 相关状态
const dropTablesLoading = ref(false);
const dropTables = ref<MythicDropTableInfo[]>([]);
const dropTableSearchText = ref('');
const dropTableDetailVisible = ref(false);
const dropTableDetail = ref<MythicDropTableInfo | null>(null);

// 掉落编辑相关状态
type DropType = 'mythicitem' | 'vanilla' | 'droptable';

interface EditableDrop {
  type: DropType;           // 掉落类型
  item: string;             // 物品ID/掉落表ID
  amount: string;           // 数量
  chance: number;           // 概率
}

const DROP_TYPES = [
  { value: 'mythicitem', label: 'MythicMobs 物品', icon: 'Box' },
  { value: 'vanilla', label: '原版物品', icon: 'Present' },
  { value: 'droptable', label: '掉落表', icon: 'Coin' },
] as const;

// 常用原版物品列表
const VANILLA_ITEMS = [
  { value: 'DIAMOND', label: '钻石 (DIAMOND)' },
  { value: 'EMERALD', label: '绿宝石 (EMERALD)' },
  { value: 'GOLD_INGOT', label: '金锭 (GOLD_INGOT)' },
  { value: 'IRON_INGOT', label: '铁锭 (IRON_INGOT)' },
  { value: 'NETHERITE_INGOT', label: '下界合金锭 (NETHERITE_INGOT)' },
  { value: 'DIAMOND_SWORD', label: '钻石剑 (DIAMOND_SWORD)' },
  { value: 'DIAMOND_PICKAXE', label: '钻石镐 (DIAMOND_PICKAXE)' },
  { value: 'DIAMOND_AXE', label: '钻石斧 (DIAMOND_AXE)' },
  { value: 'DIAMOND_SHOVEL', label: '钻石锹 (DIAMOND_SHOVEL)' },
  { value: 'DIAMOND_HOE', label: '钻石锄 (DIAMOND_HOE)' },
  { value: 'NETHERITE_SWORD', label: '下界合金剑 (NETHERITE_SWORD)' },
  { value: 'NETHERITE_PICKAXE', label: '下界合金镐 (NETHERITE_PICKAXE)' },
  { value: 'NETHERITE_AXE', label: '下界合金斧 (NETHERITE_AXE)' },
  { value: 'BOW', label: '弓 (BOW)' },
  { value: 'CROSSBOW', label: '弩 (CROSSBOW)' },
  { value: 'TRIDENT', label: '三叉戟 (TRIDENT)' },
  { value: 'DIAMOND_HELMET', label: '钻石头盔 (DIAMOND_HELMET)' },
  { value: 'DIAMOND_CHESTPLATE', label: '钻石胸甲 (DIAMOND_CHESTPLATE)' },
  { value: 'DIAMOND_LEGGINGS', label: '钻石护腿 (DIAMOND_LEGGINGS)' },
  { value: 'DIAMOND_BOOTS', label: '钻石靴子 (DIAMOND_BOOTS)' },
  { value: 'NETHERITE_HELMET', label: '下界合金头盔 (NETHERITE_HELMET)' },
  { value: 'NETHERITE_CHESTPLATE', label: '下界合金胸甲 (NETHERITE_CHESTPLATE)' },
  { value: 'NETHERITE_LEGGINGS', label: '下界合金护腿 (NETHERITE_LEGGINGS)' },
  { value: 'NETHERITE_BOOTS', label: '下界合金靴子 (NETHERITE_BOOTS)' },
  { value: 'ELYTRA', label: '鞘翅 (ELYTRA)' },
  { value: 'TOTEM_OF_UNDYING', label: '不死图腾 (TOTEM_OF_UNDYING)' },
  { value: 'ENCHANTED_GOLDEN_APPLE', label: '附魔金苹果 (ENCHANTED_GOLDEN_APPLE)' },
  { value: 'GOLDEN_APPLE', label: '金苹果 (GOLDEN_APPLE)' },
  { value: 'EXPERIENCE_BOTTLE', label: '附魔之瓶 (EXPERIENCE_BOTTLE)' },
  { value: 'NETHER_STAR', label: '下界之星 (NETHER_STAR)' },
  { value: 'DRAGON_EGG', label: '龙蛋 (DRAGON_EGG)' },
  { value: 'BEACON', label: '信标 (BEACON)' },
  { value: 'SHULKER_BOX', label: '潜影盒 (SHULKER_BOX)' },
  { value: 'ENDER_CHEST', label: '末影箱 (ENDER_CHEST)' },
  { value: 'ENDER_PEARL', label: '末影珍珠 (ENDER_PEARL)' },
  { value: 'BLAZE_ROD', label: '烈焰棒 (BLAZE_ROD)' },
  { value: 'GHAST_TEAR', label: '恶魂之泪 (GHAST_TEAR)' },
  { value: 'CHORUS_FRUIT', label: '紫颂果 (CHORUS_FRUIT)' },
];

const editableDrops = ref<EditableDrop[]>([]);
const savingDrops = ref(false);

// 表单
const formRef = ref<FormInstance>();
const formData = reactive<CreateSpawnPointInput & { enabled: boolean }>({
  id: '',
  name: '',
  description: '',
  world: 'world',
  x: 0,
  y: 64,
  z: 0,
  mythicMobId: '',
  tier: 1,
  cooldownSeconds: 7200,
  maxCount: 1,
  randomLocation: false,
  spawnRadius: 100,
  randomRadius: 0,
  spawnMode: 'fixed',
  enabled: true,
  preSpawnWarning: 30,
});

const formRules: FormRules = {
  id: [
    { required: true, message: '请输入 ID', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: 'ID 只能包含字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  world: [{ required: true, message: '请输入世界名称', trigger: 'blur' }],
  mythicMobId: [{ required: true, message: '请选择 Boss 类型', trigger: 'change' }],
  tier: [{ required: true, message: '请选择等级', trigger: 'change' }],
};

// 计算属性
const filteredSpawnPoints = computed(() => {
  let result = spawnPoints.value;

  if (searchText.value) {
    const keyword = searchText.value.toLowerCase();
    result = result.filter(
      sp =>
        sp.id.toLowerCase().includes(keyword) ||
        sp.name.toLowerCase().includes(keyword) ||
        sp.mythicMobId.toLowerCase().includes(keyword)
    );
  }

  if (filterTier.value !== null) {
    result = result.filter(sp => sp.tier === filterTier.value);
  }

  if (filterEnabled.value !== null) {
    result = result.filter(sp => sp.enabled === filterEnabled.value);
  }

  return result;
});

const filteredMythicMobs = computed(() => {
  let result = mythicMobs.value;

  if (mobSearchText.value) {
    const keyword = mobSearchText.value.toLowerCase();
    result = result.filter(
      m =>
        m.id.toLowerCase().includes(keyword) ||
        m.displayName.toLowerCase().includes(keyword)
    );
  }

  if (mobFilterType.value) {
    result = result.filter(m => m.type.toUpperCase() === mobFilterType.value);
  }

  return result;
});

const hasOtherOptions = computed(() => {
  if (!mobDetail.value) return false;
  return (
    mobDetail.value.faction ||
    mobDetail.value.disguise ||
    mobDetail.value.followRange ||
    mobDetail.value.hearingRange ||
    mobDetail.value.preventOtherDrops !== undefined ||
    mobDetail.value.preventRandomEquipment !== undefined
  );
});

// 计算属性 - Items 筛选
const filteredItems = computed(() => {
  let result = items.value;

  if (itemSearchText.value) {
    const keyword = itemSearchText.value.toLowerCase();
    result = result.filter(
      item =>
        item.id.toLowerCase().includes(keyword) ||
        item.displayName.toLowerCase().includes(keyword) ||
        item.material.toLowerCase().includes(keyword)
    );
  }

  if (itemFilterMaterial.value) {
    result = result.filter(item => item.material.toUpperCase() === itemFilterMaterial.value);
  }

  return result;
});

// 计算属性 - DropTables 筛选
const filteredDropTables = computed(() => {
  let result = dropTables.value;

  if (dropTableSearchText.value) {
    const keyword = dropTableSearchText.value.toLowerCase();
    result = result.filter(dt => dt.id.toLowerCase().includes(keyword));
  }

  return result;
});

// 方法
const fetchData = async () => {
  loading.value = true;
  try {
    const [pointsRes, statsRes, statusRes] = await Promise.all([
      bossApi.getSpawnPoints(),
      bossApi.getStats(),
      bossApi.getMythicMobsStatus(),
    ]);
    spawnPoints.value = pointsRes;
    stats.value = statsRes;
    mythicMobsStatus.value = statusRes;
  } catch (error) {
    ElMessage.error('获取数据失败');
  } finally {
    loading.value = false;
  }
};

const fetchMythicMobs = async () => {
  mobsLoading.value = true;
  try {
    const [mobs, typeStats] = await Promise.all([
      bossApi.getMythicMobs(),
      bossApi.getMythicMobTypeStats(),
    ]);
    mythicMobs.value = mobs;
    mobTypeStats.value = typeStats;
  } catch (error) {
    console.error('获取 MythicMobs 列表失败:', error);
  } finally {
    mobsLoading.value = false;
  }
};

const refreshData = () => {
  fetchData();
  fetchMythicMobs();
};

const refreshMythicMobs = async () => {
  mobsLoading.value = true;
  try {
    await bossApi.refreshMythicMobsCache();
    await fetchMythicMobs();
    ElMessage.success('缓存已刷新');
  } catch (error) {
    ElMessage.error('刷新失败');
  } finally {
    mobsLoading.value = false;
  }
};

const showMobDetail = async (mobId: string) => {
  mobDetailVisible.value = true;
  mobDetailLoading.value = true;
  mobDetail.value = null;
  editableDrops.value = [];

  try {
    // 使用增强版 API 获取详细信息
    mobDetail.value = await bossApi.getMythicMobDetail(mobId, true);

    // 初始化可编辑的掉落列表，智能识别类型
    if (mobDetail.value?.drops?.length) {
      editableDrops.value = mobDetail.value.drops.map(drop => {
        const itemId = drop.item || drop.raw || '';

        // 智能识别掉落类型
        let type: DropType = 'vanilla';
        if (itemId.toLowerCase().includes('droptable') || itemId.startsWith('table:')) {
          type = 'droptable';
        } else if (items.value.some(i => i.id === itemId)) {
          type = 'mythicitem';
        } else if (dropTables.value.some(dt => dt.id === itemId)) {
          type = 'droptable';
        }

        return {
          type,
          item: itemId,
          amount: drop.amount || '1',
          chance: drop.chance ?? 1,
        };
      });
    }
  } catch (error) {
    ElMessage.error('获取怪物详情失败');
  } finally {
    mobDetailLoading.value = false;
  }
};

// 掉落编辑方法
const addMobDrop = () => {
  editableDrops.value.push({
    type: 'mythicitem',
    item: '',
    amount: '1',
    chance: 1,
  });
};

const removeMobDrop = (index: number) => {
  editableDrops.value.splice(index, 1);
};

const saveMobDrops = async () => {
  if (!mobDetail.value) return;

  savingDrops.value = true;
  try {
    // 构建掉落配置，根据类型生成不同格式
    const dropsConfig = editableDrops.value
      .filter(drop => drop.item.trim())
      .map(drop => {
        let dropStr = '';

        // 根据掉落类型构建格式
        if (drop.type === 'droptable') {
          // 掉落表引用格式: droptable{table=掉落表名}
          dropStr = `droptable{table=${drop.item}}`;
        } else if (drop.type === 'mythicitem') {
          // MythicMobs 物品格式: mythicitem{item=物品ID}
          dropStr = `mythicitem{item=${drop.item}}`;
        } else {
          // 原版物品直接使用 ID
          dropStr = drop.item;
        }

        // 添加数量（如果不是1）
        if (drop.amount && drop.amount !== '1') {
          dropStr += ` ${drop.amount}`;
        }

        // 添加概率（如果小于1）
        if (drop.chance !== undefined && drop.chance < 1) {
          dropStr += ` ${drop.chance}`;
        }

        return dropStr;
      });

    // 获取当前配置
    const yamlResult = await bossApi.getMobRawYaml(mobDetail.value.id);
    const validation = await bossApi.validateYaml(yamlResult.yaml);

    if (!validation.valid || !validation.parsed) {
      ElMessage.error('获取当前配置失败');
      return;
    }

    // 更新掉落配置
    const config = validation.parsed[mobDetail.value.id];
    if (!config) {
      ElMessage.error('配置解析失败');
      return;
    }

    config.Drops = dropsConfig;

    // 保存配置
    await bossApi.saveMobConfig(mobDetail.value.id, config);
    ElMessage.success('掉落配置已保存');

    // 刷新详情
    await showMobDetail(mobDetail.value.id);
  } catch (error) {
    ElMessage.error('保存掉落配置失败');
  } finally {
    savingDrops.value = false;
  }
};

// Items 相关方法
const fetchItems = async () => {
  itemsLoading.value = true;
  try {
    const [itemsList, materialStats] = await Promise.all([
      bossApi.getItems(),
      bossApi.getItemMaterialStats(),
    ]);
    items.value = itemsList;
    itemMaterialStats.value = materialStats;
  } catch (error) {
    console.error('获取物品列表失败:', error);
  } finally {
    itemsLoading.value = false;
  }
};

const refreshItems = async () => {
  await fetchItems();
  ElMessage.success('物品列表已刷新');
};

const showItemDetail = async (itemId: string) => {
  itemDetailVisible.value = true;
  itemDetailLoading.value = true;
  itemDetail.value = null;

  try {
    itemDetail.value = await bossApi.getItemDetail(itemId);
  } catch (error) {
    ElMessage.error('获取物品详情失败');
  } finally {
    itemDetailLoading.value = false;
  }
};

// DropTables 相关方法
const fetchDropTables = async () => {
  dropTablesLoading.value = true;
  try {
    const tables = await bossApi.getDropTables();
    dropTables.value = tables;
  } catch (error) {
    console.error('获取掉落表列表失败:', error);
  } finally {
    dropTablesLoading.value = false;
  }
};

const showDropTableDetail = (table: MythicDropTableInfo) => {
  dropTableDetail.value = table;
  dropTableDetailVisible.value = true;
};

// YAML 编辑功能
const yamlEditorVisible = ref(false);
const yamlContent = ref('');
const yamlError = ref('');
const yamlSaving = ref(false);

const openYamlEditor = async () => {
  if (!mobDetail.value) return;

  try {
    const result = await bossApi.getMobRawYaml(mobDetail.value.id);
    yamlContent.value = result.yaml;
    yamlError.value = '';
    yamlEditorVisible.value = true;
  } catch (error) {
    ElMessage.error('获取配置失败');
  }
};

const validateYamlContent = async () => {
  try {
    const result = await bossApi.validateYaml(yamlContent.value);
    if (result.valid) {
      yamlError.value = '';
      ElMessage.success('YAML 格式正确');
    } else {
      yamlError.value = result.error || '格式错误';
    }
  } catch (error) {
    ElMessage.error('验证失败');
  }
};

const saveYamlConfig = async () => {
  if (!mobDetail.value) return;

  // 先验证
  const validation = await bossApi.validateYaml(yamlContent.value);
  if (!validation.valid) {
    yamlError.value = validation.error || '格式错误';
    ElMessage.error('YAML 格式错误，请修正后再保存');
    return;
  }

  yamlSaving.value = true;
  try {
    // 从 YAML 中提取配置对象 (去掉外层的 mobId 键)
    const config = validation.parsed[mobDetail.value.id];
    if (!config) {
      yamlError.value = `配置中必须包含 "${mobDetail.value.id}" 键`;
      return;
    }

    await bossApi.saveMobConfig(mobDetail.value.id, config);
    ElMessage.success('配置已保存');
    yamlEditorVisible.value = false;

    // 刷新详情
    showMobDetail(mobDetail.value.id);
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    yamlSaving.value = false;
  }
};

// 物品 YAML 编辑功能
const itemYamlEditorVisible = ref(false);
const itemYamlContent = ref('');
const itemYamlError = ref('');
const itemYamlSaving = ref(false);

const openItemYamlEditor = async () => {
  if (!itemDetail.value) return;

  try {
    const result = await bossApi.getItemRawYaml(itemDetail.value.id);
    itemYamlContent.value = result.yaml;
    itemYamlError.value = '';
    itemYamlEditorVisible.value = true;
  } catch (error) {
    ElMessage.error('获取物品配置失败');
  }
};

const validateItemYamlContent = async () => {
  try {
    const result = await bossApi.validateYaml(itemYamlContent.value);
    if (result.valid) {
      itemYamlError.value = '';
      ElMessage.success('YAML 格式正确');
    } else {
      itemYamlError.value = result.error || '格式错误';
    }
  } catch (error) {
    ElMessage.error('验证失败');
  }
};

const saveItemYamlConfig = async () => {
  if (!itemDetail.value) return;

  // 先验证
  const validation = await bossApi.validateYaml(itemYamlContent.value);
  if (!validation.valid) {
    itemYamlError.value = validation.error || '格式错误';
    ElMessage.error('YAML 格式错误，请修正后再保存');
    return;
  }

  itemYamlSaving.value = true;
  try {
    // 从 YAML 中提取配置对象 (去掉外层的 itemId 键)
    const config = validation.parsed[itemDetail.value.id];
    if (!config) {
      itemYamlError.value = `配置中必须包含 "${itemDetail.value.id}" 键`;
      return;
    }

    await bossApi.saveItemConfig(itemDetail.value.id, config);
    ElMessage.success('物品配置已保存');
    itemYamlEditorVisible.value = false;

    // 刷新详情
    showItemDetail(itemDetail.value.id);
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    itemYamlSaving.value = false;
  }
};

const openCreateDialog = () => {
  isEditing.value = false;
  Object.assign(formData, {
    id: '',
    name: '',
    description: '',
    world: 'world',
    x: 0,
    y: 64,
    z: 0,
    mythicMobId: '',
    tier: 1,
    cooldownSeconds: 7200,
    maxCount: 1,
    randomLocation: false,
    spawnRadius: 100,
    randomRadius: 0,
    spawnMode: 'fixed',
    enabled: true,
    preSpawnWarning: 30,
  });
  dialogVisible.value = true;
};

const openEditDialog = (row: BossSpawnPoint) => {
  isEditing.value = true;
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    description: row.description || '',
    world: row.world,
    x: row.x,
    y: row.y,
    z: row.z,
    mythicMobId: row.mythicMobId,
    tier: row.tier,
    cooldownSeconds: Number(row.cooldownSeconds),
    maxCount: row.maxCount,
    randomLocation: row.randomLocation,
    spawnRadius: row.spawnRadius,
    randomRadius: row.randomRadius,
    spawnMode: row.spawnMode,
    enabled: row.enabled,
    preSpawnWarning: row.preSpawnWarning,
  });
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (isEditing.value) {
      await bossApi.updateSpawnPoint(formData.id, formData);
      ElMessage.success('更新成功');
    } else {
      await bossApi.createSpawnPoint(formData);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    fetchData();
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败');
  } finally {
    submitting.value = false;
  }
};

const handleToggleEnabled = async (row: BossSpawnPoint) => {
  (row as any)._toggling = true;
  try {
    await bossApi.toggleSpawnPoint(row.id);
    ElMessage.success(row.enabled ? '已启用' : '已禁用');
    fetchData();
  } catch (error) {
    row.enabled = !row.enabled; // 回滚
    ElMessage.error('切换状态失败');
  } finally {
    (row as any)._toggling = false;
  }
};

const handleDelete = async (row: BossSpawnPoint) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除刷新点 "${row.name}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
  } catch {
    return;
  }

  try {
    await bossApi.deleteSpawnPoint(row.id);
    ElMessage.success('删除成功');
    fetchData();
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

// 生命周期
onMounted(() => {
  fetchData();
  fetchMythicMobs();
  fetchItems();
  fetchDropTables();
});
</script>

<style scoped>
.boss-container {
  width: 100%;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-suffix {
  font-size: 12px;
  color: #999;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-bar {
  display: flex;
  gap: 15px;
}

.mob-info {
  display: flex;
  flex-direction: column;
}

.mob-id {
  font-weight: 500;
}

.mob-name {
  font-size: 12px;
  color: #999;
}

.location-text {
  font-family: monospace;
  font-size: 12px;
}

.cooldown-text {
  color: #e6a23c;
}

.health-text {
  color: #67c23a;
  font-weight: 500;
}

.damage-text {
  color: #f56c6c;
  font-weight: 500;
}

.no-data {
  color: #c0c4cc;
}

.mob-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.mob-option-name {
  font-weight: 500;
}

.mob-option-id {
  color: #999;
  font-size: 12px;
}

.mob-option-health {
  color: #67c23a;
  font-size: 12px;
}

.form-item-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 怪物详情样式 */
.detail-loading {
  padding: 20px;
}

.mob-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
  color: var(--el-text-color-primary);
}

.skill-raw,
.drop-raw {
  font-size: 12px;
  background-color: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
  word-break: break-all;
}

.ai-list {
  margin: 0;
  padding-left: 20px;
}

.ai-list li {
  font-family: monospace;
  font-size: 12px;
  margin-bottom: 4px;
}

/* 模板继承样式 */
.template-section {
  background-color: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 15px;
}

.template-chain {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.chain-arrow {
  color: var(--el-text-color-secondary);
  font-weight: bold;
}

.template-depth {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 技能组样式 */
.skill-group-info {
  margin-bottom: 10px;
}

/* 更多参数提示 */
.more-params {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-left: 4px;
}

/* 掉落表引用样式 */
.drops-table-ref {
  margin-bottom: 15px;
}

.expanded-drops {
  margin-top: 10px;
  padding: 10px;
  background-color: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.expanded-label {
  margin: 0 0 10px 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* YAML 编辑器样式 */
.yaml-editor :deep(textarea) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.yaml-error {
  margin-top: 10px;
}

/* 物品详情样式 */
.item-detail {
  max-height: 600px;
  overflow-y: auto;
}

.lore-list {
  background-color: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 6px;
}

.lore-line {
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
  padding: 2px 0;
}

.color-preview {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  vertical-align: middle;
  margin-right: 6px;
  border: 1px solid var(--el-border-color-light);
}

.positive-value {
  color: #67c23a;
  font-weight: 500;
}

.negative-value {
  color: #f56c6c;
  font-weight: 500;
}

.skull-texture {
  display: block;
  font-size: 12px;
  background-color: var(--el-fill-color-light);
  padding: 8px 12px;
  border-radius: 6px;
  word-break: break-all;
  max-height: 100px;
  overflow-y: auto;
}

.nbt-data {
  font-size: 12px;
  background-color: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 6px;
  margin: 0;
  max-height: 200px;
  overflow: auto;
}

/* 掉落表样式 */
.droptable-detail {
  max-height: 600px;
  overflow-y: auto;
}

.drops-preview {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.more-drops {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-left: 4px;
}

.drop-item {
  font-weight: 500;
  color: var(--el-color-primary);
}

/* 掉落编辑样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h4 {
  margin: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.editable-drops {
  margin-top: 15px;
}

.drops-label {
  margin: 0 0 10px 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.drops-actions {
  margin-top: 15px;
  text-align: right;
}
</style>
