<template>
  <div class="ai-assistant">
    <!-- AI配置状态栏 -->
    <div class="ai-status-bar">
      <div class="current-model">
        <span class="model-icon">{{ currentProvider?.icon || '🤖' }}</span>
        <span class="model-info">
          <span class="provider-name">{{ currentProvider?.displayName }}</span>
          <span class="model-name">{{ currentModel?.displayName }}</span>
        </span>
      </div>
      <div class="config-actions">
        <a-tooltip title="AI配置">
          <a-button type="text" size="small" @click="showConfigModal = true">
            <SettingOutlined />
          </a-button>
        </a-tooltip>
        <a-tooltip title="重新配置">
          <a-button type="text" size="small" @click="showQuickConfig = true">
            <EditOutlined />
          </a-button>
        </a-tooltip>
      </div>
    </div>

    <!-- 聊天历史 -->
    <div class="chat-history">
      <div v-if="messages.length === 0" class="welcome-message">
        <div class="welcome-icon">🤖</div>
        <h3>AI代码助手</h3>
        <p>告诉我你想要创建什么，我会帮你生成相应的Vue组件代码。</p>
        <div class="example-prompts">
          <h4>试试这些示例：</h4>
          <div class="prompt-examples">
            <a-tag 
              v-for="example in examplePrompts" 
              :key="example"
              @click="prompt = example"
              style="cursor: pointer; margin: 4px;"
            >
              {{ example }}
            </a-tag>
          </div>
        </div>
      </div>
      
      <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
        <div class="message-content">
          <div v-if="msg.role === 'user'" class="user-avatar">👤</div>
          <div v-else class="ai-avatar">{{ currentProvider?.icon || '🤖' }}</div>
          <div class="message-text">
            {{ msg.content }}
            <div v-if="msg.usage" class="usage-info">
              Tokens: {{ msg.usage.totalTokens }} ({{ msg.usage.promptTokens }} + {{ msg.usage.completionTokens }})
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <div class="advanced-options" v-if="showAdvancedOptions">
        <a-row :gutter="8">
          <a-col :span="8">
            <a-input-number
              v-model:value="options.maxTokens"
              placeholder="最大Token数"
              :min="100"
              :max="4000"
              style="width: 100%"
              size="small"
            />
          </a-col>
          <a-col :span="8">
            <a-input-number
              v-model:value="options.temperature"
              placeholder="创造性"
              :min="0"
              :max="1"
              :step="0.1"
              style="width: 100%"
              size="small"
            />
          </a-col>
          <a-col :span="8">
            <a-input-number
              v-model:value="options.topP"
              placeholder="Top P"
              :min="0"
              :max="1"
              :step="0.1"
              style="width: 100%"
              size="small"
            />
          </a-col>
        </a-row>
      </div>
      
      <div class="input-controls">
        <a-textarea
          v-model:value="prompt"
          :placeholder="currentProvider ? '描述你想要创建的组件...' : '请先配置AI模型...'"
          :auto-size="{ minRows: 2, maxRows: 5 }"
          @pressEnter.prevent="sendMessage"
          :disabled="!currentProvider"
        />
        <div class="input-actions">
          <a-tooltip title="高级选项">
            <a-button 
              type="text" 
              size="small" 
              @click="showAdvancedOptions = !showAdvancedOptions"
              :class="{ active: showAdvancedOptions }"
            >
              <AppstoreOutlined />
            </a-button>
          </a-tooltip>
          <a-button 
            type="primary" 
            :loading="loading" 
            @click="sendMessage"
            :disabled="!currentProvider || !prompt.trim()"
          >
            {{ loading ? '生成中...' : '生成代码' }}
          </a-button>
        </div>
        <div v-if="loadingText" class="loading-text">
          {{ loadingText }}
        </div>
      </div>
    </div>

    <!-- 配置对话框 -->
    <a-modal
      v-model:open="showConfigModal"
      title="AI配置管理"
      width="900px"
      :footer="null"
    >
      <AIConfig />
    </a-modal>

    <!-- 快速配置对话框 -->
    <a-modal
      v-model:open="showQuickConfig"
      title="快速配置AI模型"
      width="600px"
      @ok="handleQuickConfig"
    >
      <AIConfigForm ref="quickConfigFormRef" @submit="handleQuickConfigSubmit" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SettingOutlined, EditOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
import { useAIStore } from '@/stores/ai'
import { useUserStore } from '@/stores/user'
import { useProjectStore } from '@/stores/project'
import { generateCode, generateModuleCode } from '@/api'
import AIConfig from '@/views/AIConfig.vue'
import AIConfigForm from '@/components/AIConfigForm.vue'
import { generateWithClient } from '@/services/aiClient'

const aiStore = useAIStore()
const userStore = useUserStore()
const projectStore = useProjectStore()

const prompt = ref('')
const loading = ref(false)
const loadingText = ref('')
const messages = ref<{ role: 'user' | 'assistant', content: string; usage?: any }[]>([])
const showConfigModal = ref(false)
const showQuickConfig = ref(false)
const showAdvancedOptions = ref(false)
const quickConfigFormRef = ref()

// 高级选项
const options = ref({
  maxTokens: 2000,
  temperature: 0.7,
  topP: 1
})

// 当前AI配置
const currentProvider = computed(() => aiStore.activeProvider)
const currentModel = computed(() => aiStore.activeModel)

// 示例提示
const examplePrompts = ref([
  '创建一个用户登录表单',
  '制作一个商品卡片组件',
  '写一个数据表格，支持搜索和排序',
  '创建一个响应式的导航栏',
  '做一个简单的待办事项列表'
])

const sendMessage = async () => {
  if (!prompt.value.trim() || !currentProvider.value || !aiStore.activeConfig) {
    message.warning('请先配置AI模型')
    return
  }

  const userPrompt = prompt.value
  messages.value.push({ role: 'user', content: userPrompt })
  prompt.value = ''
  loading.value = true
  loadingText.value = '正在连接AI服务...'

  const request = {
    projectId: projectStore.currentProjectId || null,
    requirement: userPrompt,
    userId: userStore.userInfo?.id || 0,
    files: projectStore.files
  }

  let clientSuccess = false
  let clientResponse: any = null

  try {
    loadingText.value = '正在通过客户端调用AI（包含项目上下文）...'
    clientResponse = await generateWithClient(
      currentProvider.value,
      aiStore.activeConfig,
      request
    )
    clientSuccess = true
  } catch (clientErr: any) {
    console.warn('客户端AI调用失败，将尝试后端调用:', clientErr.message)
    loadingText.value = '客户端调用失败，尝试后端调用...'
  }

  try {
    if (clientSuccess && clientResponse && clientResponse.files && clientResponse.files.length > 0) {
      const files = clientResponse.files
      let firstFile = ''
      for (const file of files) {
        projectStore.updateFile(file.path, file.content)
        if (!firstFile) firstFile = file.path
      }
      if (firstFile) {
        projectStore.setActiveFile(firstFile)
      }
      messages.value.push({ 
        role: 'assistant', 
        content: `已成功生成 ${files.length} 个文件：\n${files.map((f: any) => f.path).join('\n')}`
      })
      loadingText.value = ''
    } else {
      loadingText.value = '正在通过后端调用AI...'
      const res: any = await generateModuleCode(request)
      if (res.code !== 1) {
        throw new Error(res.message || 'AI生成失败')
      }
      const files = res.files || []
      if (!files || files.length === 0) {
        throw new Error('AI未返回任何文件')
      }
      let firstFile = ''
      for (const file of files) {
        projectStore.updateFile(file.path, file.content)
        if (!firstFile) firstFile = file.path
      }
      if (firstFile) {
        projectStore.setActiveFile(firstFile)
      }
      messages.value.push({ 
        role: 'assistant', 
        content: `已成功生成 ${files.length} 个文件：\n${files.map((f: any) => f.path).join('\n')}`
      })
      loadingText.value = ''
    }
  } catch (err: any) {
    console.error('AI生成错误:', err)
    
    let errorMessage = err.response?.data?.message || err.message || '生成代码时遇到了错误'
    
    if (errorMessage.includes('401') || errorMessage.includes('Unauthorized') || errorMessage.includes('API密钥')) {
      errorMessage = 'API密钥无效或已过期，请检查AI配置中的API Key'
      loadingText.value = '🔑 API密钥错误'
    } else if (errorMessage.includes('timeout') || errorMessage.includes('超时')) {
      errorMessage = 'AI响应超时，请稍后重试'
      loadingText.value = '⏱️ 请求超时'
    } else if (errorMessage.includes('network') || errorMessage.includes('网络') || errorMessage.includes('connection') || errorMessage.includes('CORS')) {
      errorMessage = '网络连接失败，请检查网络设置或API密钥'
      loadingText.value = '🌐 网络错误'
    } else {
      loadingText.value = `❌ ${errorMessage}`
    }
    
    message.error(errorMessage)
    messages.value.push({ role: 'assistant', content: `❌ ${errorMessage}` })
  } finally {
    setTimeout(() => {
      loading.value = false
      loadingText.value = ''
    }, 2000)
  }
}

const handleQuickConfigSubmit = async (configData: any) => {
  try {
    configData.isActive = true
    await aiStore.saveConfig(configData)
    message.success('AI配置已更新')
    showQuickConfig.value = false
  } catch (error: any) {
    message.error('配置失败：' + error.message)
  }
}

const handleQuickConfig = () => {
  quickConfigFormRef.value?.submit()
}

onMounted(() => {
  // 加载AI配置
  if (userStore.userInfo) {
    aiStore.fetchConfigs(userStore.userInfo.id)
  }
})
</script>

<style scoped>
.ai-assistant {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #fafafa;
  border-left: 1px solid #f0f0f0;
}

/* AI状态栏 */
.ai-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.current-model {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-icon {
  font-size: 16px;
}

.model-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.provider-name {
  font-size: 12px;
  font-weight: 600;
  color: #333;
}

.model-name {
  font-size: 11px;
  color: #666;
}

.config-actions {
  display: flex;
  gap: 4px;
}

/* 聊天历史 */
.chat-history {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px;
  scroll-behavior: smooth;
}

.welcome-message {
  text-align: center;
  padding: 40px 20px;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-message h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #333;
}

.welcome-message p {
  margin: 0 0 24px 0;
  color: #666;
  font-size: 14px;
}

.example-prompts h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #333;
}

.prompt-examples {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.message {
  margin-bottom: 16px;
}

.message-content {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.user-avatar,
.ai-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.user-avatar {
  background: #1890ff;
  color: white;
}

.ai-avatar {
  background: #f0f0f0;
}

.message.user .message-content {
  flex-direction: row-reverse;
}

.message.user .message-text {
  background: #1890ff;
  color: white;
  border-radius: 16px 4px 16px 16px;
}

.message.assistant .message-text {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px 16px 16px 16px;
}

.message-text {
  padding: 10px 14px;
  max-width: calc(100% - 40px);
  word-wrap: break-word;
  line-height: 1.4;
}

.usage-info {
  font-size: 10px;
  color: #999;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

/* 输入区域 */
.input-area {
  background: #fff;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.advanced-options {
  padding: 12px 16px;
  background: #f9f9f9;
  border-bottom: 1px solid #f0f0f0;
}

.input-controls {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.input-actions .ant-btn {
  flex-shrink: 0;
}

.loading-text {
  padding: 8px 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 4px;
  color: #d48806;
  font-size: 13px;
  animation: pulse 2s infinite;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.input-actions .ant-btn.active {
  color: #1890ff;
  background: #e6f7ff;
}

:deep(.ant-input) {
  border-radius: 8px;
}

:deep(.ant-btn) {
  border-radius: 6px;
}

:deep(.ant-tag) {
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 11px;
  line-height: 1.4;
}

/* 滚动条样式 */
.chat-history::-webkit-scrollbar {
  width: 4px;
}

.chat-history::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.chat-history::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 2px;
}

.chat-history::-webkit-scrollbar-thumb:hover {
  background: #999;
}
</style>
