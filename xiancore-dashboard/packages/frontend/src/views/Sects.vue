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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { sectApi, type Sect } from '@/api/sect';
import { ElMessage } from 'element-plus';

const loading = ref(false);
const sects = ref<Sect[]>([]);

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

const viewDetail = (sect: Sect) => {
  ElMessage.info(`查看宗门 ${sect.name} 的详情`);
  // TODO: 跳转到详情页或打开弹窗
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
</style>
