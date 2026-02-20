<template>
  <a-layout style="min-height: 100vh">
    <AppHeader />
    
    <a-layout-content class="templates-page">
      <div class="page-header">
        <h1>模板市场</h1>
        <p>选择模板快速创建项目</p>
      </div>

      <div class="filter-section">
        <a-space>
          <a-radio-group v-model:value="selectedType" @change="loadTemplates" buttonStyle="solid">
            <a-radio-button value="">全部</a-radio-button>
            <a-radio-button value="official">官方模板</a-radio-button>
            <a-radio-button value="user">用户模板</a-radio-button>
          </a-radio-group>
          <a-divider type="vertical" />
          <a-select v-model:value="selectedCategory" style="width: 120px" @change="loadTemplates" allowClear placeholder="分类">
            <a-select-option value="基础">基础</a-select-option>
            <a-select-option value="工具">工具</a-select-option>
            <a-select-option value="游戏">游戏</a-select-option>
            <a-select-option value="展示">展示</a-select-option>
            <a-select-option value="学习">学习</a-select-option>
          </a-select>
        </a-space>
      </div>

      <a-spin :spinning="loading">
        <div class="templates-grid">
          <div
            v-for="template in templates"
            :key="template.id"
            class="template-card"
            @click="useTemplate(template)"
          >
            <div class="template-icon">
              <FileOutlined v-if="!template.thumbnail" />
              <img v-else :src="template.thumbnail" :alt="template.name" />
            </div>
            <div class="template-info">
              <div class="template-name">{{ template.name }}</div>
              <div class="template-desc">{{ template.description }}</div>
              <div class="template-meta">
                <a-tag :color="template.type === 'official' ? 'blue' : 'green'">
                  {{ template.type === 'official' ? '官方' : '用户' }}
                </a-tag>
                <span class="usage-count"><FileTextOutlined /> {{ template.usage_count || 0 }} 次使用</span>
              </div>
            </div>
          </div>
        </div>
      </a-spin>

      <a-empty v-if="!loading && templates.length === 0" description="暂无模板" />
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { FileOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getTemplates, useTemplate } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const templates = ref<any[]>([])
const loading = ref(false)
const selectedType = ref('')
const selectedCategory = ref<string | undefined>(undefined)

const loadTemplates = async () => {
  loading.value = true
  try {
    const res: any = await getTemplates({
      type: selectedType.value || undefined,
      category: selectedCategory.value,
      page: 1,
      pageSize: 20
    })
    
    if (res.code === 200) {
      templates.value = res.data?.list || []
    }
  } catch (error: any) {
    message.error('加载模板失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const useTemplateFn = async (template: any) => {
  try {
    const res: any = await useTemplate(template.id, {
      userId: userStore.currentUser?.id,
      projectName: template.name + ' - 副本'
    })
    
    if (res.code === 200) {
      message.success('项目创建成功')
      router.push(`/project/${res.data.projectId}`)
    } else {
      message.error(res.message || '创建项目失败')
    }
  } catch (error: any) {
    message.error('创建项目失败: ' + error.message)
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.templates-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
  font-size: 14px;
}

.filter-section {
  margin-bottom: 24px;
  text-align: center;
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.template-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  gap: 16px;
}

.template-card:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.template-icon {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #1890ff;
  flex-shrink: 0;
}

.template-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.template-info {
  flex: 1;
  min-width: 0;
}

.template-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.template-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.usage-count {
  font-size: 12px;
  color: #999;
}

.dark-theme .template-card {
  background: #1f1f1f;
  border-color: #303030;
}

.dark-theme .template-icon {
  background: #2a2a2a;
}

.dark-theme .template-desc {
  color: rgba(255, 255, 255, 0.65);
}
</style>
