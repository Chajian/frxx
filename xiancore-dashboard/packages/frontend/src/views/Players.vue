<template>
  <div class="players-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>玩家列表</span>
          <el-input
            v-model="searchText"
            placeholder="搜索玩家名称"
            style="width: 200px"
            clearable
          />
        </div>
      </template>

      <el-table :data="filteredPlayers" v-loading="loading" stripe>
        <el-table-column prop="name" label="玩家名称" width="150" />
        <el-table-column prop="realm" label="境界" width="120" />
        <el-table-column prop="realmStage" label="境界层数" width="100" />
        <el-table-column prop="playerLevel" label="等级" width="100" />
        <el-table-column prop="spiritualRoot" label="灵根" width="100">
          <template #default="{ row }">
            {{ (row.spiritualRoot * 100).toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column prop="spiritualRootType" label="灵根类型" width="120" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { playerApi, type Player } from '@/api/player';
import { ElMessage } from 'element-plus';

const loading = ref(false);
const searchText = ref('');
const players = ref<Player[]>([]);

const filteredPlayers = computed(() => {
  if (!searchText.value) return players.value;
  return players.value.filter((p) =>
    p.name.toLowerCase().includes(searchText.value.toLowerCase())
  );
});

const fetchPlayers = async () => {
  loading.value = true;
  try {
    players.value = await playerApi.getAll();
  } catch (error) {
    ElMessage.error('获取玩家列表失败');
  } finally {
    loading.value = false;
  }
};

const viewDetail = (player: Player) => {
  ElMessage.info(`查看玩家 ${player.name} 的详情`);
  // TODO: 跳转到详情页或打开弹窗
};

onMounted(() => {
  fetchPlayers();
});
</script>

<style scoped>
.players-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
