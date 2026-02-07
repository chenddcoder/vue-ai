<template>
  <a-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    layout="vertical"
    @finish="handleSubmit"
  >
    <!-- 选择AI提供商 -->
    <a-form-item label="AI提供商" name="providerId" required>
      <a-select
        v-model:value="formData.providerId"
        placeholder="选择AI提供商"
        :options="providerOptions"
        @change="handleProviderChange"
        :disabled="!!config"
      />
    </a-form-item>

    <!-- 选择模型 -->
    <a-form-item label="模型" name="modelId" required>
      <a-select
        v-model:value="formData.modelId"
        placeholder="选择模型"
        :options="modelOptions"
        :loading="loadingModels"
        :disabled="!formData.providerId || !!config"
      />
    </a-form-item>

    <!-- 配置字段 -->
    <template v-if="currentProvider">
      <div class="config-section">
        <h4>配置信息</h4>
        <a-row :gutter="16">
          <a-col :span="field.type === 'textarea' ? 24 : 12" v-for="field in currentProvider.configFields" :key="field.key">
            <a-form-item
              :label="field.label"
              :name="['config', field.key]"
              :rules="getFieldRules(field)"
            >
              <!-- 文本输入 -->
              <a-input
                v-if="field.type === 'text'"
                v-model:value="formData.config[field.key]"
                :placeholder="field.placeholder"
                :disabled="!formData.providerId"
              />
              
              <!-- 密码输入 -->
              <a-input-password
                v-else-if="field.type === 'password'"
                v-model:value="formData.config[field.key]"
                :placeholder="field.placeholder"
                :disabled="!formData.providerId"
              />
              
              <!-- URL输入 -->
              <a-input
                v-else-if="field.type === 'url'"
                v-model:value="formData.config[field.key]"
                :placeholder="field.placeholder"
                :disabled="!formData.providerId"
              />
              
              <!-- 数字输入 -->
              <a-input-number
                v-else-if="field.type === 'number'"
                v-model:value="formData.config[field.key]"
                :placeholder="field.placeholder"
                :min="field.validation?.min"
                :max="field.validation?.max"
                :disabled="!formData.providerId"
                style="width: 100%"
              />
              
              <!-- 选择框 -->
              <a-select
                v-else-if="field.type === 'select'"
                v-model:value="formData.config[field.key]"
                :placeholder="field.placeholder"
                :options="field.options"
                :disabled="!formData.providerId"
              />
              
              <!-- 多行文本 -->
              <a-textarea
                v-else-if="field.type === 'textarea'"
                v-model:value="formData.config[field.key]"
                :placeholder="field.placeholder"
                :auto-size="{ minRows: 3, maxRows: 6 }"
                :disabled="!formData.providerId"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </div>
    </template>

    <!-- 提供商信息 -->
    <template v-if="currentProvider">
      <a-alert
        type="info"
        show-icon
        style="margin-top: 16px"
      >
        <template #message>
          <div>
            <strong>{{ currentProvider.displayName }}</strong>
            <p style="margin: 8px 0 0 0; color: #666; font-size: 12px;">
              {{ currentProvider.description }}
              <a v-if="currentProvider.website" :href="currentProvider.website" target="_blank" style="margin-left: 8px;">
                了解更多 →
              </a>
            </p>
          </div>
        </template>
      </a-alert>
    </template>

    <!-- 隐藏的提交按钮 -->
    <a-form-item style="display: none;">
      <button type="submit">Submit</button>
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { AI_PROVIDERS } from '@/types/ai'
import type { AIProvider, UserAIConfig } from '@/types/ai'
import { useAIStore } from '@/stores/ai'

interface Props {
  config?: UserAIConfig | null
}

interface Emits {
  (e: 'submit', data: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const aiStore = useAIStore()

const formRef = ref<FormInstance>()
const loadingModels = ref(false)

// 表单数据
const formData = reactive({
  providerId: '',
  modelId: '',
  config: {} as Record<string, any>
})

// 当前选择的提供商
const currentProvider = computed(() => {
  return AI_PROVIDERS.find(p => p.id === formData.providerId)
})

// 提供商选项
const providerOptions = computed(() => {
  return AI_PROVIDERS.map(provider => ({
    label: provider.displayName,
    value: provider.id,
    // disabled: !provider.available
  }))
})

// 模型选项
const modelOptions = computed(() => {
  if (!currentProvider.value) return []
  return currentProvider.value.models.map(model => ({
    label: `${model.displayName}${model.contextWindow ? ` (${model.contextWindow} tokens)` : ''}`,
    value: model.id
  }))
})

// 表单验证规则
const formRules = computed(() => {
  if (!currentProvider.value) return {}
  
  const rules: any = {
    providerId: [{ required: true, message: '请选择AI提供商' }],
    modelId: [{ required: true, message: '请选择模型' }],
    config: {}
  }

  // 动态添加配置字段验证规则
  currentProvider.value.configFields.forEach(field => {
    rules.config[field.key] = getFieldRules(field)
  })

  return rules
})

// 获取字段验证规则
const getFieldRules = (field: any) => {
  const rules: any[] = []
  
  if (field.required) {
    rules.push({ required: true, message: `${field.label}是必填项` })
  }

  if (field.validation) {
    const { pattern, message } = field.validation
    if (pattern) {
      rules.push({ 
        pattern: new RegExp(pattern), 
        message: message || `${field.label}格式不正确` 
      })
    }
  }

  return rules
}

// 处理提供商变化
const handleProviderChange = (providerId: string) => {
  formData.modelId = ''
  formData.config = {}
  
  // 设置默认值
  const provider = AI_PROVIDERS.find(p => p.id === providerId)
  if (provider) {
    provider.configFields.forEach(field => {
      if (field.defaultValue) {
        formData.config[field.key] = field.defaultValue
      }
    })
    
    // 如果有模型，选择第一个作为默认值
    if (provider.models.length > 0) {
      formData.modelId = provider.models[0].id
    }
  }
}

// 提交表单
const handleSubmit = async (values: any) => {
  try {
    // 额外的配置验证
    if (currentProvider.value) {
      const errors = aiStore.validateConfig(currentProvider.value, values.config)
      if (errors.length > 0) {
        throw new Error(errors.join('; '))
      }
    }

    emit('submit', values)
  } catch (error: any) {
    throw error
  }
}

// 手动提交
const submit = async () => {
  try {
    await formRef.value?.validate()
    await handleSubmit(formData)
  } catch (error) {
    console.error('Form validation failed:', error)
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    providerId: '',
    modelId: '',
    config: {}
  })
}

// 初始化编辑数据
const initEditData = () => {
  if (props.config) {
    Object.assign(formData, {
      providerId: props.config.providerId,
      modelId: props.config.modelId,
      config: typeof props.config.config === 'string' ? JSON.parse(props.config.config) : props.config.config
    })
  }
}

// 监听配置变化
watch(() => props.config, () => {
  if (props.config) {
    initEditData()
  } else {
    resetForm()
  }
}, { immediate: true })

onMounted(() => {
  if (props.config) {
    initEditData()
  }
})

// 暴露方法给父组件
defineExpose({
  submit,
  resetForm
})
</script>

<style scoped>
.config-section {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.config-section h4 {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

:deep(.ant-form-item-label > label) {
  font-weight: 500;
}

:deep(.ant-select-selector),
:deep(.ant-input) {
  border-radius: 6px;
}

:deep(.ant-alert-message) {
  line-height: 1.5;
}
</style>