<template>
  <div class="sects-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>宗门列表</span>
        </div>
      </template>

      <el-table :data="sects" v-loading="loading" stripe>
        <el-table-column prop="name" label="宗门名称" width="180" />
        <el-table-column prop="ownerName" label="宗主" width="150" />
        <el-table-column prop="level" label="等级" width="100" />
        <el-table-column label="成员数量" width="120">
          <template #default="{ row }">
            {{ row._count?.members || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="sectFunds" label="宗门资金" width="150" />
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
      :title="editMode ? `编辑 - ${currentSect?.name}` : `宗门详情 - ${currentSect?.name}`"
      width="500px"
    >
      <div v-if="currentSect" class="detail-form">
        <!-- 查看模式 -->
        <div v-if="!editMode" class="view-mode">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="宗门ID">{{ currentSect.id }}</el-descriptions-item>
            <el-descriptions-item label="宗门名称">{{ currentSect.name }}</el-descriptions-item>
            <el-descriptions-item label="宗主">{{ currentSect.ownerName }}</el-descriptions-item>
            <el-descriptions-item label="等级">{{ currentSect.level }}</el-descriptions-item>
            <el-descriptions-item label="经验值">{{ currentSect.experience }}</el-descriptions-item>
            <el-descriptions-item label="宗门资金">{{ currentSect.sectFunds }}</el-descriptions-item>
            <el-descriptions-item label="成员数量">{{
              currentSect._count?.members || 0
            }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 编辑模式 -->
        <el-form v-else :model="editFormData" label-width="100px">
          <el-form-item label="宗门名称">
            <el-input v-model="editFormData.name" />
          </el-form-item>
          <el-form-item label="宗门等级">
            <el-input-number v-model="editFormData.level" :min="0" />
          </el-form-item>
          <el-form-item label="宗门资金">
            <el-input-number v-model="editFormData.sectFunds" :min="0" />
          </el-form-item>
          <el-form-item label="经验值">
            <el-input-number v-model="editFormData.experience" :min="0" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button v-if="!editMode" @click="enterEditMode">编辑</el-button>
          <el-button v-if="editMode" @click="cancelEdit">取消</el-button>
          <el-button v-if="editMode" type="primary" @click="saveSect" :loading="saving">
            保存
          </el-button>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { sectApi, type Sect } from '@/api/sect';
import { ElMessage } from 'element-plus';

const loading = ref(false);
const saving = ref(false);
const sects = ref<Sect[]>([]);
const detailDialogVisible = ref(false);
const editMode = ref(false);
const currentSect = ref<Sect | null>(null);
const editFormData = ref<Partial<Sect>>({});

const fetchSects = async () => {
  loading.value = true;
  try {
    sects.value = await sectApi.getAll();
  } catch (error) {
    ElMessage.error('获取宗门列表失败');
  } finally {
    loading.value = false;
  }
};

const viewDetail = async (sect: Sect) => {
  try {
    // 获取详细信息
    currentSect.value = await sectApi.getById(sect.id);
    editMode.value = false;
    detailDialogVisible.value = true;
  } catch (error) {
    ElMessage.error('获取宗门详情失败');
  }
};

const enterEditMode = () => {
  if (currentSect.value) {
    // 初始化编辑表单数据
    editFormData.value = {
      name: currentSect.value.name,
      level: currentSect.value.level,
      sectFunds: currentSect.value.sectFunds,
      experience: currentSect.value.experience,
    };
    editMode.value = true;
  }
};

const cancelEdit = () => {
  editMode.value = false;
  editFormData.value = {};
};

const saveSect = async () => {
  if (!currentSect.value) return;

  saving.value = true;
  try {
    const updatedSect = await sectApi.update(currentSect.value.id, editFormData.value);
    // 更新当前显示的宗门数据
    currentSect.value = updatedSect;
    // 更新列表数据
    const index = sects.value.findIndex((s) => s.id === currentSect.value!.id);
    if (index !== -1) {
      sects.value[index] = updatedSect;
    }
    editMode.value = false;
    ElMessage.success('宗门信息已更新');
  } catch (error) {
    ElMessage.error('保存宗门信息失败');
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  fetchSects();
});
</script>

<style scoped>
.sects-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-form {
  padding: 10px 0;
}

.view-mode {
  min-height: 200px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
