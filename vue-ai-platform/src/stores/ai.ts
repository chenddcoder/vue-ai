import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { AI_PROVIDERS, AIProvider, UserAIConfig, DEFAULT_AI_CONFIG } from '@/types/ai'
import { getAIConfigs, saveAIConfig, deleteAIConfig, setActiveAIConfig, testAIConfig, generateCode } from '@/api'

export const useAIStore = defineStore('ai', () => {
  // 状态
  const configs = ref<UserAIConfig[]>([])
  const activeConfig = ref<UserAIConfig | null>(null)
  const loading = ref(false)

  // 计算属性
  const activeProvider = computed(() => {
    if (!activeConfig.value) return null
    return AI_PROVIDERS.find(p => p.id === activeConfig.value!.providerId)
  })

  const activeModel = computed(() => {
    if (!activeConfig.value || !activeProvider.value) return null
    return activeProvider.value.models.find(m => m.id === activeConfig.value!.modelId)
  })

  const sortedConfigs = computed(() => {
    return [...configs.value].sort((a, b) => {
      if (a.isActive && !b.isActive) return -1
      if (!a.isActive && b.isActive) return 1
      return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    })
  })

  // 获取AI提供商
  const getProvider = (providerId: string): AIProvider | undefined => {
    return AI_PROVIDERS.find(p => p.id === providerId)
  }

  // 获取用户配置
  const fetchConfigs = async (userId: number) => {
    try {
      loading.value = true
      const res: any = await getAIConfigs(userId)
      configs.value = (res.data || []).map((c: any) => ({
        ...c,
        config: typeof c.config === 'string' ? JSON.parse(c.config) : c.config
      }))

      // 设置活动配置
      const active = configs.value.find(c => c.isActive)
      if (active) {
        activeConfig.value = active
      } else if (configs.value.length > 0) {
        // 如果没有活动配置，设置第一个为活动配置
        await setActiveConfig(configs.value[0].id)
      }
    } catch (error) {
      console.error('Failed to fetch AI configs:', error)
    } finally {
      loading.value = false
    }
  }

  // 保存AI配置
  const saveConfig = async (config: Partial<UserAIConfig> & { userId: number }) => {
    try {
      loading.value = true
      // 确保必填字段存在
      const configPayload = {
        id: config.id,
        userId: config.userId,
        providerId: config.providerId!,
        modelId: config.modelId!,
        config: config.config || {},
        isActive: config.isActive
      }
      const res: any = await saveAIConfig(configPayload)
      
      if (config.id) {
        // 更新现有配置
        const index = configs.value.findIndex(c => c.id === config.id)
        if (index !== -1) {
          configs.value[index] = res.data
        }
      } else {
        // 添加新配置
        configs.value.push(res.data)
      }

      // 如果这是第一个配置，设置为活动配置
      if (configs.value.length === 1) {
        await setActiveConfig(res.data.id)
      }

      return res.data
    } catch (error) {
      console.error('Failed to save AI config:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 删除AI配置
  const removeConfig = async (configId: number) => {
    try {
      loading.value = true
      await deleteAIConfig(configId)
      
      const index = configs.value.findIndex(c => c.id === configId)
      if (index !== -1) {
        const config = configs.value[index]
        configs.value.splice(index, 1)
        
        // 如果删除的是活动配置，设置另一个为活动配置
        if (config.isActive && configs.value.length > 0) {
          await setActiveConfig(configs.value[0].id)
        }
      }
    } catch (error) {
      console.error('Failed to delete AI config:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 设置活动配置
  const setActiveConfig = async (configId: number) => {
    try {
      const res: any = await setActiveAIConfig(configId)
      
      // 更新本地状态
      configs.value.forEach(c => {
        c.isActive = c.id === configId
      })
      
      activeConfig.value = configs.value.find(c => c.id === configId) || null
      
      return res.data
    } catch (error) {
      console.error('Failed to set active AI config:', error)
      throw error
    }
  }

  // 测试AI配置
  const testConfig = async (config: UserAIConfig, testPrompt = 'Hello, can you write a simple Vue component?') => {
    try {
      loading.value = true
      const res: any = await generateCodeWithConfig(config, testPrompt)
      return { success: true, data: res.data }
    } catch (error: any) {
      return { success: false, error: error.message }
    } finally {
      loading.value = false
    }
  }

  // 使用指定配置生成代码
  const generateCodeWithConfig = async (config: UserAIConfig, prompt: string, options?: {
    maxTokens?: number
    temperature?: number
    context?: string
  }) => {
    const provider = getProvider(config.providerId)
    if (!provider) {
      throw new Error(`Unknown provider: ${config.providerId}`)
    }

    const request = {
      provider: config.providerId,
      model: config.modelId,
      config: config.config,
      prompt,
      ...options
    }

    // 这里会调用AI生成API
    return generateCode(request)
  }

  // 初始化默认配置
  const initDefaultConfig = (userId: number) => {
    const defaultConfig: UserAIConfig = {
      ...DEFAULT_AI_CONFIG,
      userId,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    return defaultConfig
  }

  // 验证配置完整性
  const validateConfig = (provider: AIProvider, config: Record<string, any>) => {
    const errors: string[] = []
    
    // 检查必填字段
    for (const field of provider.configFields) {
      if (field.required && !config[field.key]) {
        errors.push(`${field.label}是必填项`)
      }
    }

    // 检查数据类型和格式
    for (const field of provider.configFields) {
      const value = config[field.key]
      if (!value) continue

      if (field.validation) {
        const { min, max, pattern, message } = field.validation
        
        if (field.type === 'number' && typeof value === 'string') {
          const num = parseFloat(value)
          if (!isNaN(num)) {
            if (min !== undefined && num < min) {
              errors.push(`${field.label}不能小于${min}`)
            }
            if (max !== undefined && num > max) {
              errors.push(`${field.label}不能大于${max}`)
            }
          }
        }

        if (pattern && typeof value === 'string') {
          const regex = new RegExp(pattern)
          if (!regex.test(value)) {
            errors.push(message || `${field.label}格式不正确`)
          }
        }
      }
    }

    return errors
  }

  return {
    // 状态
    configs,
    activeConfig,
    loading,
    
    // 计算属性
    activeProvider,
    activeModel,
    sortedConfigs,
    
    // 方法
    getProvider,
    fetchConfigs,
    saveConfig,
    removeConfig,
    setActiveConfig,
    testConfig,
    generateCodeWithConfig,
    initDefaultConfig,
    validateConfig
  }
})