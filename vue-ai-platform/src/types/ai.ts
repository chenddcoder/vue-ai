// AI配置相关的类型定义

export interface AIProvider {
  id: string
  name: string
  displayName: string
  type: 'openai' | 'anthropic' | 'azure' | 'local' | 'custom'
  models: AIModel[]
  required: string[]
  optional: string[]
  configFields: AIConfigField[]
  description: string
  icon: string
  website?: string
}

export interface AIModel {
  id: string
  name: string
  displayName: string
  contextWindow?: number
  maxTokens?: number
  inputCost?: number
  outputCost?: number
}

export interface AIConfigField {
  key: string
  label: string
  type: 'text' | 'password' | 'select' | 'number' | 'url' | 'textarea'
  required: boolean
  description: string
  placeholder?: string
  options?: { label: string; value: string }[]
  defaultValue?: any
  validation?: {
    min?: number
    max?: number
    pattern?: string
    message?: string
  }
}

export interface UserAIConfig {
  id: number
  userId: number
  providerId: string
  modelId: string
  config: Record<string, any>
  isActive: boolean
  createdAt: string
  updatedAt: string
}

// 预定义的AI提供商
export const AI_PROVIDERS: AIProvider[] = [
  {
    id: 'openai',
    name: 'OpenAI',
    displayName: 'OpenAI GPT',
    type: 'openai',
    models: [
      {
        id: 'gpt-4',
        name: 'gpt-4',
        displayName: 'GPT-4',
        contextWindow: 8192,
        maxTokens: 4096,
        inputCost: 0.03,
        outputCost: 0.06
      },
      {
        id: 'gpt-4-turbo',
        name: 'gpt-4-turbo',
        displayName: 'GPT-4 Turbo',
        contextWindow: 128000,
        maxTokens: 4096,
        inputCost: 0.01,
        outputCost: 0.03
      },
      {
        id: 'gpt-3.5-turbo',
        name: 'gpt-3.5-turbo',
        displayName: 'GPT-3.5 Turbo',
        contextWindow: 16384,
        maxTokens: 4096,
        inputCost: 0.0005,
        outputCost: 0.0015
      }
    ],
    required: ['apiKey'],
    optional: ['organization', 'baseUrl'],
    configFields: [
      {
        key: 'apiKey',
        label: 'API Key',
        type: 'password',
        required: true,
        description: '您的OpenAI API密钥',
        placeholder: 'sk-...'
      },
      {
        key: 'organization',
        label: 'Organization ID',
        type: 'text',
        required: false,
        description: '您的组织ID（可选）',
        placeholder: 'org-...'
      },
      {
        key: 'baseUrl',
        label: 'Base URL',
        type: 'url',
        required: false,
        description: '自定义API端点（可选）',
        placeholder: 'https://api.openai.com/v1'
      }
    ],
    description: 'OpenAI的GPT系列模型，提供强大的代码生成能力',
    icon: '🤖',
    website: 'https://openai.com'
  },
  {
    id: 'anthropic',
    name: 'Anthropic',
    displayName: 'Claude',
    type: 'anthropic',
    models: [
      {
        id: 'claude-3-opus-20240229',
        name: 'claude-3-opus-20240229',
        displayName: 'Claude 3 Opus',
        contextWindow: 200000,
        maxTokens: 4096,
        inputCost: 0.015,
        outputCost: 0.075
      },
      {
        id: 'claude-3-sonnet-20240229',
        name: 'claude-3-sonnet-20240229',
        displayName: 'Claude 3 Sonnet',
        contextWindow: 200000,
        maxTokens: 4096,
        inputCost: 0.003,
        outputCost: 0.015
      },
      {
        id: 'claude-3-haiku-20240307',
        name: 'claude-3-haiku-20240307',
        displayName: 'Claude 3 Haiku',
        contextWindow: 200000,
        maxTokens: 4096,
        inputCost: 0.00025,
        outputCost: 0.00125
      }
    ],
    required: ['apiKey'],
    optional: [],
    configFields: [
      {
        key: 'apiKey',
        label: 'API Key',
        type: 'password',
        required: true,
        description: '您的Anthropic API密钥',
        placeholder: 'sk-ant-...'
      }
    ],
    description: 'Anthropic的Claude系列模型，专注于安全和有用的AI助手',
    icon: '🧠',
    website: 'https://anthropic.com'
  },
  {
    id: 'azure',
    name: 'Azure OpenAI',
    displayName: 'Azure OpenAI',
    type: 'azure',
    models: [
      {
        id: 'gpt-4',
        name: 'gpt-4',
        displayName: 'GPT-4 (Azure)',
        contextWindow: 8192,
        maxTokens: 4096
      },
      {
        id: 'gpt-35-turbo',
        name: 'gpt-35-turbo',
        displayName: 'GPT-3.5 Turbo (Azure)',
        contextWindow: 16384,
        maxTokens: 4096
      }
    ],
    required: ['apiKey', 'endpoint', 'deployment'],
    optional: ['apiVersion'],
    configFields: [
      {
        key: 'apiKey',
        label: 'API Key',
        type: 'password',
        required: true,
        description: '您的Azure OpenAI API密钥',
        placeholder: '请输入API密钥'
      },
      {
        key: 'endpoint',
        label: 'Endpoint',
        type: 'url',
        required: true,
        description: '您的Azure OpenAI端点',
        placeholder: 'https://your-resource.openai.azure.com'
      },
      {
        key: 'deployment',
        label: 'Deployment Name',
        type: 'text',
        required: true,
        description: '模型部署名称',
        placeholder: '请输入部署名称'
      },
      {
        key: 'apiVersion',
        label: 'API Version',
        type: 'select',
        required: false,
        description: 'API版本',
        options: [
          { label: '2024-02-15-preview', value: '2024-02-15-preview' },
          { label: '2023-12-01-preview', value: '2023-12-01-preview' },
          { label: '2023-07-01-preview', value: '2023-07-01-preview' }
        ],
        defaultValue: '2024-02-15-preview'
      }
    ],
    description: '微软Azure托管的OpenAI服务，提供企业级的稳定性保障',
    icon: '☁️',
    website: 'https://azure.microsoft.com/products/ai-services/openai-service'
  },
  {
    id: 'local',
    name: 'Local AI',
    displayName: '本地模型',
    type: 'local',
    models: [
      {
        id: 'llama-3.1-8b',
        name: 'llama-3.1-8b',
        displayName: 'Llama 3.1 8B',
        contextWindow: 128000
      },
      {
        id: 'qwen-2.5-7b',
        name: 'qwen-2.5-7b',
        displayName: 'Qwen 2.5 7B',
        contextWindow: 32768
      },
      {
        id: 'custom',
        name: 'custom',
        displayName: '自定义模型',
        contextWindow: 4096
      }
    ],
    required: ['baseUrl'],
    optional: ['apiKey', 'model'],
    configFields: [
      {
        key: 'baseUrl',
        label: 'API地址',
        type: 'url',
        required: true,
        description: '本地模型的API地址',
        placeholder: 'http://localhost:11434/v1'
      },
      {
        key: 'apiKey',
        label: 'API Key',
        type: 'password',
        required: false,
        description: 'API密钥（如果需要）',
        placeholder: '可选的API密钥'
      },
      {
        key: 'model',
        label: '模型名称',
        type: 'text',
        required: false,
        description: '模型名称（如果不使用预设模型）',
        placeholder: 'llama3.1:8b'
      }
    ],
    description: '在本地运行的AI模型，如Ollama、LM Studio等',
    icon: '🏠'
  },
  {
    id: 'custom',
    name: 'Custom',
    displayName: '自定义API',
    type: 'custom',
    models: [
      {
        id: 'custom-model',
        name: 'custom-model',
        displayName: '自定义模型',
        contextWindow: 4096
      }
    ],
    required: ['baseUrl'],
    optional: ['apiKey', 'headers', 'requestFormat', 'responseFormat'],
    configFields: [
      {
        key: 'baseUrl',
        label: 'API地址',
        type: 'url',
        required: true,
        description: '自定义API的端点地址',
        placeholder: 'https://your-api.com/v1/chat/completions'
      },
      {
        key: 'modelName',
        label: '模型名称',
        type: 'text',
        required: false,
        description: 'API模型名称（可选，如 gpt-4, glm-4）',
        placeholder: 'gpt-4'
      },
      {
        key: 'apiKey',
        label: 'API Key',
        type: 'password',
        required: false,
        description: 'API密钥',
        placeholder: 'Bearer token或API密钥'
      },
      {
        key: 'headers',
        label: '自定义请求头',
        type: 'textarea',
        required: false,
        description: 'JSON格式的自定义请求头',
        placeholder: '{"Custom-Header": "value"}'
      }
    ],
    description: '支持任何兼容OpenAI格式的自定义API',
    icon: '⚙️'
  },
  {
    id: 'zhipu',
    name: 'ZhipuAI',
    displayName: '智谱AI',
    type: 'custom',
    models: [
      {
        id: 'glm-4',
        name: 'glm-4',
        displayName: 'GLM-4',
        contextWindow: 128000,
        maxTokens: 8192
      },
      {
        id: 'glm-4v',
        name: 'glm-4v',
        displayName: 'GLM-4V',
        contextWindow: 128000,
        maxTokens: 8192
      },
      {
        id: 'glm-3-turbo',
        name: 'glm-3-turbo',
        displayName: 'GLM-3-Turbo',
        contextWindow: 32000,
        maxTokens: 4096
      },
      {
        id: 'chatglm_turbo',
        name: 'chatglm_turbo',
        displayName: 'ChatGLM-Turbo',
        contextWindow: 130000,
        maxTokens: 4096
      }
    ],
    required: ['apiKey'],
    optional: ['baseUrl'],
    configFields: [
      {
        key: 'apiKey',
        label: 'API Key',
        type: 'password',
        required: true,
        description: '智谱AI API密钥',
        placeholder: '在控制台获取的API Key'
      },
      {
        key: 'baseUrl',
        label: 'Base URL',
        type: 'url',
        required: false,
        description: 'API地址（可选）',
        placeholder: 'https://open.bigmodel.cn/api/paas/v4',
        defaultValue: 'https://open.bigmodel.cn/api/paas/v4'
      }
    ],
    description: '智谱AI，清华大学技术支持的国产大语言模型',
    icon: '🧠',
    website: 'https://bigmodel.cn'
  }
]

// 默认配置
export const DEFAULT_AI_CONFIG: UserAIConfig = {
  id: 0,
  userId: 0,
  providerId: 'openai',
  modelId: 'gpt-3.5-turbo',
  config: {},
  isActive: true,
  createdAt: '',
  updatedAt: ''
}

// AI生成请求参数
export interface AIGenerateRequest {
  provider: string
  model: string
  config: Record<string, any>
  prompt: string
  context?: string
  maxTokens?: number
  temperature?: number
  topP?: number
}

// AI生成响应
export interface AIGenerateResponse {
  template: string
  methods: string
  style: string
  usage?: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}