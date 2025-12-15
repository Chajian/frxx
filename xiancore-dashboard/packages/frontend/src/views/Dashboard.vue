<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff">
              <el-icon :size="30"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">玩家总数</div>
              <div class="stat-value">{{ stats.totalPlayers }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67c23a">
              <el-icon :size="30"><OfficeBuilding /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">宗门总数</div>
              <div class="stat-value">{{ stats.totalSects }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #e6a23c">
              <el-icon :size="30"><Medal /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">在线玩家</div>
              <div class="stat-value">{{ stats.onlinePlayers }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #f56c6c">
              <el-icon :size="30"><TrophyBase /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">最高境界</div>
              <div class="stat-value">{{ stats.highestRealm }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 排行榜 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>玩家等级排行</span>
              <el-button type="primary" link>查看更多</el-button>
            </div>
          </template>
          <div v-loading="loading">
            <div v-if="playerRanking.length === 0" class="empty-text">暂无数据</div>
            <div v-else>
              <div v-for="(player, index) in playerRanking" :key="player.uuid" class="ranking-item">
                <div class="rank">{{ index + 1 }}</div>
                <div class="info">
                  <div class="name">{{ player.name }}</div>
                  <div class="desc">{{ player.realm }} {{ player.realmStage }}层</div>
                </div>
                <div class="value">Lv.{{ player.playerLevel }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>宗门等级排行</span>
              <el-button type="primary" link>查看更多</el-button>
            </div>
          </template>
          <div v-loading="loading">
            <div v-if="sectRanking.length === 0" class="empty-text">暂无数据</div>
            <div v-else>
              <div v-for="(sect, index) in sectRanking" :key="sect.id" class="ranking-item">
                <div class="rank">{{ index + 1 }}</div>
                <div class="info">
                  <div class="name">{{ sect.name }}</div>
                  <div class="desc">宗主: {{ sect.ownerName }}</div>
                </div>
                <div class="value">Lv.{{ sect.level }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { User, OfficeBuilding, Medal, TrophyBase } from '@element-plus/icons-vue';
import { playerApi } from '@/api/player';
import { sectApi } from '@/api/sect';
import type { Player } from '@/api/player';
import type { Sect } from '@/api/sect';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';

const loading = ref(false);
const stats = ref({
  totalPlayers: 0,
  totalSects: 0,
  onlinePlayers: 0,
  highestRealm: '-',
});

const playerRanking = ref<Player[]>([]);
const sectRanking = ref<Sect[]>([]);

const fetchData = async () => {
  loading.value = true;
  try {
    const [players, sects, playerRank, sectRank] = await Promise.all([
      playerApi.getAll(),
      sectApi.getAll(),
      playerApi.getRanking(10),
      sectApi.getRanking(10),
    ]);

    stats.value.totalPlayers = players.length;
    stats.value.totalSects = sects.length;
    stats.value.onlinePlayers = 0; // TODO: 计算在线玩家
    stats.value.highestRealm = playerRank[0]?.realm || '-';

    playerRanking.value = playerRank;
    sectRanking.value = sectRank;
  } catch (error) {
    console.error('Failed to fetch data:', error);
  } finally {
    loading.value = false;
  }
};

// 使用自动刷新
useAutoRefresh(fetchData);

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.dashboard-container {
  width: 100%;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank {
  width: 30px;
  height: 30px;
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: var(--el-text-color-regular);
  margin-right: 12px;
}

.ranking-item:nth-child(1) .rank {
  background-color: #ffd700;
  color: #fff;
}

.ranking-item:nth-child(2) .rank {
  background-color: #c0c0c0;
  color: #fff;
}

.ranking-item:nth-child(3) .rank {
  background-color: #cd7f32;
  color: #fff;
}

.info {
  flex: 1;
}

.name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.value {
  font-size: 16px;
  font-weight: bold;
  color: var(--el-color-primary);
}

.empty-text {
  text-align: center;
  padding: 40px 0;
  color: var(--el-text-color-secondary);
}
</style>
