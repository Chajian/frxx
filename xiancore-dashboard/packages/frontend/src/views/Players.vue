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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="editMode ? `编辑玩家 - ${currentPlayer?.name}` : `玩家详情 - ${currentPlayer?.name}`"
      width="650px"
    >
      <div v-if="currentPlayer" v-loading="detailLoading">
        <!-- 查看模式 -->
        <el-tabs v-if="!editMode" v-model="activeTab">
          <!-- 基础信息 -->
          <el-tab-pane label="基础信息" name="basic">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="UUID" :span="2">{{
                currentPlayer.uuid
              }}</el-descriptions-item>
              <el-descriptions-item label="玩家名称">{{
                currentPlayer.name
              }}</el-descriptions-item>
              <el-descriptions-item label="等级">{{
                currentPlayer.playerLevel
              }}</el-descriptions-item>
              <el-descriptions-item label="境界">{{
                currentPlayer.realm
              }}</el-descriptions-item>
              <el-descriptions-item label="境界层数">{{
                currentPlayer.realmStage
              }}</el-descriptions-item>
              <el-descriptions-item label="灵根">{{
                ((currentPlayer.spiritualRoot || 0) * 100).toFixed(1)
              }}%</el-descriptions-item>
              <el-descriptions-item label="灵根类型">{{
                currentPlayer.spiritualRootType || '未知'
              }}</el-descriptions-item>
              <el-descriptions-item label="真气">{{
                currentPlayer.qi
              }}</el-descriptions-item>
              <el-descriptions-item label="灵石">{{
                currentPlayer.spiritStones || 0
              }}</el-descriptions-item>
              <el-descriptions-item label="宗门贡献">{{
                currentPlayer.contributionPoints || 0
              }}</el-descriptions-item>
              <el-descriptions-item label="活跃灵气">{{
                currentPlayer.activeQi || 0
              }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 功法技能 -->
          <el-tab-pane label="功法技能" name="skills">
            <!-- 统计卡片 -->
            <el-row :gutter="20" style="margin-bottom: 20px">
              <el-col :span="8">
                <el-statistic title="已学功法" :value="currentPlayer.skills?.length || 0" />
              </el-col>
              <el-col :span="8">
                <el-statistic title="平均等级" :value="avgSkillLevel" :precision="1" />
              </el-col>
              <el-col :span="8">
                <el-statistic title="技能点" :value="currentPlayer.skillPoints || 0" />
              </el-col>
            </el-row>

            <!-- 增强的功法表格 -->
            <el-table :data="enrichedSkills" stripe max-height="400" v-if="currentPlayer.skills?.length">
              <el-table-column prop="skillId" label="功法ID" width="150" />
              <el-table-column label="功法名称" width="150">
                <template #default="{ row }">
                  {{ row.skillInfo?.name || '未知' }}
                </template>
              </el-table-column>
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  <el-tag v-if="row.skillInfo">{{ getSkillTypeLabel(row.skillInfo.type) }}</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="元素" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.skillInfo" :type="getElementTagType(row.skillInfo.element)">
                    {{ getSkillElementLabel(row.skillInfo.element) }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="等级进度" width="200">
                <template #default="{ row }">
                  <el-progress
                    v-if="row.skillInfo"
                    :percentage="(row.level / (row.skillInfo.maxLevel || 10)) * 100"
                    :format="() => `${row.level}/${row.skillInfo.maxLevel || 10}`"
                  />
                  <span v-else>Lv.{{ row.level }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="proficiency" label="熟练度" width="100" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="viewSkillDetail(row.skillId)">
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无功法技能" />
          </el-tab-pane>

          <!-- 装备 -->
          <el-tab-pane label="装备" name="equipment">
            <el-table :data="currentPlayer.equipment || []" stripe max-height="300">
              <el-table-column prop="slot" label="槽位" width="120" />
              <el-table-column prop="itemId" label="物品ID" />
              <el-table-column prop="itemData" label="物品数据" show-overflow-tooltip />
            </el-table>
            <el-empty v-if="!currentPlayer.equipment?.length" description="暂无装备" />
          </el-tab-pane>
        </el-tabs>

        <!-- 编辑模式 -->
        <el-form v-else :model="editFormData" label-width="100px">
          <el-divider content-position="left">修炼数据</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="境界">
                <el-select v-model="editFormData.realm" placeholder="选择境界">
                  <el-option label="炼气期" value="炼气期" />
                  <el-option label="筑基期" value="筑基期" />
                  <el-option label="金丹期" value="金丹期" />
                  <el-option label="元婴期" value="元婴期" />
                  <el-option label="化神期" value="化神期" />
                  <el-option label="炼虚期" value="炼虚期" />
                  <el-option label="合体期" value="合体期" />
                  <el-option label="大乘期" value="大乘期" />
                  <el-option label="渡劫期" value="渡劫期" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="玩家等级">
                <el-input-number v-model="editFormData.playerLevel" :min="1" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="真气(修为)">
                <el-input-number v-model="editFormData.qi" :min="0" :step="1000" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="灵根值">
                <el-slider v-model="editFormData.spiritualRoot" :min="0" :max="1" :step="0.01" show-input />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="灵根类型">
                <el-select v-model="editFormData.spiritualRootType" placeholder="选择灵根类型" filterable>
                  <el-option-group label="天灵根 (0.5%)">
                    <el-option label="纯金灵根" value="HEAVENLY_METAL" />
                    <el-option label="纯木灵根" value="HEAVENLY_WOOD" />
                    <el-option label="纯水灵根" value="HEAVENLY_WATER" />
                    <el-option label="纯火灵根" value="HEAVENLY_FIRE" />
                    <el-option label="纯土灵根" value="HEAVENLY_EARTH" />
                  </el-option-group>
                  <el-option-group label="异灵根 (2.5%)">
                    <el-option label="金木双灵根" value="VARIANT_METAL_WOOD" />
                    <el-option label="金水双灵根" value="VARIANT_METAL_WATER" />
                    <el-option label="金火双灵根" value="VARIANT_METAL_FIRE" />
                    <el-option label="金土双灵根" value="VARIANT_METAL_EARTH" />
                    <el-option label="木水双灵根" value="VARIANT_WOOD_WATER" />
                    <el-option label="木火双灵根" value="VARIANT_WOOD_FIRE" />
                    <el-option label="木土双灵根" value="VARIANT_WOOD_EARTH" />
                    <el-option label="水火双灵根" value="VARIANT_WATER_FIRE" />
                    <el-option label="水土双灵根" value="VARIANT_WATER_EARTH" />
                    <el-option label="火土双灵根" value="VARIANT_FIRE_EARTH" />
                  </el-option-group>
                  <el-option-group label="杂灵根">
                    <el-option label="杂灵根" value="MIXED" />
                  </el-option-group>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">资源数据</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="灵石">
                <el-input-number v-model="editFormData.spiritStones" :min="0" :step="100" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="宗门贡献">
                <el-input-number v-model="editFormData.contributionPoints" :min="0" :step="10" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="活跃灵气">
                <el-input-number v-model="editFormData.activeQi" :min="0" :max="100" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button v-if="!editMode" @click="enterEditMode">编辑</el-button>
          <el-button v-if="editMode" @click="cancelEdit">取消</el-button>
          <el-button v-if="editMode" type="primary" @click="savePlayer" :loading="saving">
            保存
          </el-button>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { playerApi, type Player } from '@/api/player';
import {
  skillApi,
  getSkillTypeLabel,
  getSkillElementLabel,
  getElementTagType,
  type SkillInfo,
} from '@/api/skill';
import { ElMessage } from 'element-plus';

const router = useRouter();

const loading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const searchText = ref('');
const players = ref<Player[]>([]);
const detailDialogVisible = ref(false);
const currentPlayer = ref<any>(null);
const activeTab = ref('basic');
const editMode = ref(false);
const editFormData = ref<Partial<Player>>({});
const allSkills = ref<SkillInfo[]>([]);

const filteredPlayers = computed(() => {
  if (!searchText.value) return players.value;
  return players.value.filter((p) =>
    p.name.toLowerCase().includes(searchText.value.toLowerCase())
  );
});

// 丰富化的功法数据（关联功法详细信息）
const enrichedSkills = computed(() => {
  if (!currentPlayer.value?.skills) return [];

  return currentPlayer.value.skills.map((ps: any) => ({
    ...ps,
    skillInfo: allSkills.value.find((s) => s.id === ps.skillId) || null,
  }));
});

// 平均功法等级
const avgSkillLevel = computed(() => {
  const skills = currentPlayer.value?.skills || [];
  if (skills.length === 0) return 0;
  const sum = skills.reduce((acc: number, s: any) => acc + (s.level || 0), 0);
  return sum / skills.length;
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

// 加载所有功法（用于丰富化玩家功法数据）
const fetchAllSkills = async () => {
  try {
    allSkills.value = await skillApi.getAllSkills();
  } catch (error) {
    console.error('获取功法列表失败:', error);
  }
};

const viewDetail = async (player: Player) => {
  detailDialogVisible.value = true;
  detailLoading.value = true;
  activeTab.value = 'basic';
  editMode.value = false;

  try {
    const [playerDetail] = await Promise.all([
      playerApi.getByUuid(player.uuid),
      allSkills.value.length === 0 ? fetchAllSkills() : Promise.resolve(),
    ]);
    currentPlayer.value = playerDetail;
  } catch (error) {
    ElMessage.error('获取玩家详情失败');
  } finally {
    detailLoading.value = false;
  }
};

// 查看功法详情（跳转到功法管理页面）
const viewSkillDetail = (skillId: string) => {
  detailDialogVisible.value = false;
  router.push(`/skills?highlight=${skillId}`);
};

const enterEditMode = () => {
  if (currentPlayer.value) {
    editFormData.value = {
      realm: currentPlayer.value.realm,
      playerLevel: currentPlayer.value.playerLevel,
      qi: Number(currentPlayer.value.qi) || 0,
      spiritualRoot: currentPlayer.value.spiritualRoot,
      spiritualRootType: currentPlayer.value.spiritualRootType,
      spiritStones: Number(currentPlayer.value.spiritStones) || 0,
      contributionPoints: currentPlayer.value.contributionPoints || 0,
      activeQi: Number(currentPlayer.value.activeQi) || 0,
    };
    editMode.value = true;
  }
};

const cancelEdit = () => {
  editMode.value = false;
  editFormData.value = {};
};

const savePlayer = async () => {
  if (!currentPlayer.value) return;

  saving.value = true;
  try {
    const updatedPlayer = await playerApi.update(currentPlayer.value.uuid, editFormData.value);
    currentPlayer.value = updatedPlayer;
    // 更新列表数据
    const index = players.value.findIndex((p) => p.uuid === currentPlayer.value.uuid);
    if (index !== -1) {
      players.value[index] = { ...players.value[index], ...updatedPlayer };
    }
    editMode.value = false;
    ElMessage.success('玩家信息已更新');
  } catch (error) {
    ElMessage.error('保存玩家信息失败');
  } finally {
    saving.value = false;
  }
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
