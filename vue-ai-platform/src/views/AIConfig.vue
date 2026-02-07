<template>
  <div class="ai-config-page">
    <div class="page-header">
      <h2>AI配置管理</h2>
      <a-button type="primary" @click="showAddModal = true">
        <template #icon><PlusOutlined /></template>
        添加配置
      </a-button>
    </div>

    <div class="config-list">
      <a-spin :spinning="loading">
        <div v-if="sortedConfigs.length === 0" class="empty-state">
          <a-empty description="还没有AI配置">
            <a-button type="primary" @click="showAddModal = true">添加第一个配置</a-button>
          </a-empty>
        </div>
        
        <div v-else class="config-cards">
          <div v-for="config in sortedConfigs" :key="config.id" class="config-card" :class="{ active: config.isActive }">
            <div class="card-header">
              <div class="provider-info">
                <span class="provider-icon">{{ getProviderIcon(config.providerId) }}</span>
                <div>
                  <div class="provider-name">{{ getProviderName(config.providerId) }}</div>
                  <div class="model-name">{{ getModelName(config.providerId, config.modelId) }}</div>
                </div>
              </div>
              <div class="card-actions">
                <a-tag v-if="config.isActive" color="success">当前使用</a-tag>
                <a-dropdown>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="editConfig(config)">编辑</a-menu-item>
                      <a-menu-item @click="testConfig(config)" :loading="testingConfig === config.id">测试</a-menu-item>
                      <a-menu-item @click="setAsActive(config.id)" v-if="!config.isActive">设为当前</a-menu-item>
                      <a-menu-divider />
                      <a-menu-item danger @click="deleteConfig(config.id)">删除</a-menu-item>
                    </a-menu>
                  </template>
                  <a-button type="text" size="small">
                    <MoreOutlined />
                  </a-button>
                </a-dropdown>
              </div>
            </div>
            
            <div class="card-content">
              <div class="config-summary">
                <a-descriptions size="small" :column="1">
                  <a-descriptions-item v-for="field in getConfigSummaryFields(config.providerId, config.config)" :key="field.key" :label="field.label">
                    {{ field.displayValue }}
                  </a-descriptions-item>
                </a-descriptions>
              </div>
              <div class="config-meta">
                <span class="update-time">更新于 {{ formatTime(config.updatedAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </a-spin>
    </div>

    <!-- 添加/编辑配置对话框 -->
    <a-modal
      v-model:open="showAddModal"
      :title="editingConfig ? '编辑AI配置' : '添加AI配置'"
      width="800px"
      @ok="handleSaveConfig"
      @cancel="resetForm"
      :confirmLoading="saving"
    >
      <AIConfigForm
        ref="configFormRef"
        :config="editingConfig"
        @submit="handleConfigSubmit"
      />
    </a-modal>

    <!-- 测试配置对话框 -->
    <a-modal
      v-model:open="showTestModal"
      title="测试AI配置"
      width="600px"
      :footer="null"
    >
      <div v-if="testResult">
        <a-result
          :status="testResult.success ? 'success' : 'error'"
          :title="testResult.success ? '配置测试成功' : '配置测试失败'"
        >
          <template #extra v-if="testResult.data">
            <div class="test-result-details">
              <p><strong>模型:</strong> {{ testResult.data.model }}</p>
              <p><strong>响应时间:</strong> {{ testResult.data.responseTime }}</p>
              <p><strong>测试结果:</strong> {{ testResult.data.testResult }}</p>
            </div>
          </template>
          <template #extra v-else-if="testResult.error">
            <a-alert type="error" :message="testResult.error" />
          </template>
        </a-result>
      </div>
      <div v-else class="test-loading">
        <a-spin tip="正在测试配置..." />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, MoreOutlined } from '@ant-design/icons-vue'
import { useAIStore } from '@/stores/ai'
import { useUserStore } from '@/stores/user'
import { AI_PROVIDERS } from '@/types/ai'
import AIConfigForm from '@/components/AIConfigForm.vue'
import type { UserAIConfig } from '@/types/ai'

const aiStore = useAIStore()
const userStore = useUserStore()

const showAddModal = ref(false)
const showTestModal = ref(false)
const editingConfig = ref<UserAIConfig | null>(null)
const saving = ref(false)
const testingConfig = ref<number | null>(null)
const testResult = ref<any>(null)
const configFormRef = ref()

const loading = computed(() => aiStore.loading)
const sortedConfigs = computed(() => aiStore.sortedConfigs)

const getProviderIcon = (providerId: string) => {
  const provider = AI_PROVIDERS.find(p => p.id === providerId)
  return provider?.icon || '🤖'
}

const getProviderName = (providerId: string) => {
  const provider = AI_PROVIDERS.find(p => p.id === providerId)
  return provider?.displayName || providerId
}

const getModelName = (providerId: string, modelId: string) => {
  const provider = AI_PROVIDERS.find(p => p.id === providerId)
  const model = provider?.models.find(m => m.id === modelId)
  return model?.displayName || modelId
}

const getConfigSummaryFields = (providerId: string, config: any) => {
  const provider = AI_PROVIDERS.find(p => p.id === providerId)
  if (!provider) return []

  return provider.configFields
    .filter(field => field.required || config[field.key])
    .slice(0, 3) // 只显示前3个字段
    .map(field => ({
      key: field.key,
      label: field.label,
      displayValue: field.type === 'password' ? '••••••••' : (config[field.key] || '未设置')
    }))
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN')
}

const editConfig = (config: UserAIConfig) => {
  editingConfig.value = { ...config }
  showAddModal.value = true
}

const deleteConfig = (configId: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个AI配置吗？此操作不可恢复。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await aiStore.removeConfig(configId)
        message.success('配置已删除')
      } catch (error: any) {
        message.error('删除失败：' + error.message)
      }
    }
  })
}

const setAsActive = async (configId: number) => {
  try {
    await aiStore.setActiveConfig(configId)
    message.success('已设置为当前使用的配置')
  } catch (error: any) {
    message.error('设置失败：' + error.message)
  }
}

const testConfig = async (config: UserAIConfig) => {
  testingConfig.value = config.id
  testResult.value = null
  showTestModal.value = true

  try {
    const result = await aiStore.testConfig(config)
    testResult.value = result
  } catch (error: any) {
    testResult.value = { success: false, error: error.message }
  } finally {
    testingConfig.value = null
  }
}

const handleConfigSubmit = async (configData: any) => {
  try {
    saving.value = true
    
    const configPayload = {
      ...configData,
      userId: userStore.userInfo?.id || 1
    }

    await aiStore.saveConfig(configPayload)
    message.success(editingConfig.value ? '配置已更新' : '配置已添加')
    showAddModal.value = false
    resetForm()
  } catch (error: any) {
    message.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

const handleSaveConfig = () => {
  // 触发表单提交
  const formRef = configFormRef.value
  if (formRef) {
    formRef.submit()
  }
}

const resetForm = () => {
  editingConfig.value = null
  testResult.value = null
}

onMounted(() => {
  if (userStore.userInfo) {
    aiStore.fetchConfigs(userStore.userInfo.id)
  }
})
</script>

<style scoped>
.ai-config-page {
  padding: 24px;
  background: #fff;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.config-list {
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.config-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
}

.config-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
}

.config-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.config-card.active {
  border-color: #1890ff;
  background: #f6ffed;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.provider-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.provider-icon {
  font-size: 24px;
}

.provider-name {
  font-weight: 600;
  font-size: 16px;
}

.model-name {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-content {
  font-size: 12px;
}

.config-summary {
  margin-bottom: 12px;
}

.config-meta {
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
  margin-top: 12px;
}

.update-time {
  color: #999;
  font-size: 11px;
}

.test-result-details {
  text-align: left;
  max-width: 400px;
}

.test-result-details p {
  margin: 8px 0;
}

.test-loading {
  text-align: center;
  padding: 40px 0;
}

:deep(.ant-descriptions-item-label) {
  font-weight: 500;
  color: #666;
}

:deep(.ant-descriptions-item-content) {
  color: #333;
}
</style>