import type { AIProvider, UserAIConfig } from '@/types/ai'

export interface GenerateRequest {
  projectId?: number | null
  requirement: string
  userId: number
  files?: Record<string, string>
}

export interface GeneratedFile {
  path: string
  content: string
}

export interface AIResponse {
  content: string
  files?: GeneratedFile[]
  usage?: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}

export class AIClient {
  private provider: AIProvider
  private config: UserAIConfig

  constructor(provider: AIProvider, config: UserAIConfig) {
    this.provider = provider
    this.config = config
  }

  async generate(request: GenerateRequest): Promise<AIResponse> {
    const messages = this.buildMessages(request)
    
    let result: AIResponse
    switch (this.provider.id) {
      case 'openai':
      case 'custom':
        result = await this.callOpenAICompatible(messages)
        break
      case 'anthropic':
        result = await this.callAnthropic(messages)
        break
      case 'zhipu':
        result = await this.callZhipu(messages)
        break
      case 'qwen':
        result = await this.callQwen(messages)
        break
      default:
        result = await this.callOpenAICompatible(messages)
    }

    result.files = this.parseGeneratedContent(result.content)
    return result
  }

  private buildMessages(request: GenerateRequest) {
    const systemPrompt = [
      'You are a senior full-stack engineer and Vue 3 expert.',
      'Your task is to generate code based on project context and requirements.',
      '',
      'Rules to follow:',
      '1. Generated code must follow Vue 3 Composition API standards with <script setup>.',
      '2. Use TypeScript.',
      '3. Strictly follow existing project directory structure and naming conventions.',
      '4. Note: Project root is the source directory. App.vue is the entry page (at root).',
      '   Do not include src/ prefix in generated file paths. Example: views/Home.vue, components/Header.vue.',
      '5. Return format must be XML with multiple files:',
      '<file path="views/NewPage.vue"><![CDATA[<template>...</template><script setup>...</script>]]></file>',
      '<file path="api/new-api.ts"><![CDATA[export const ...]]></file>',
      '6. Do not return Markdown code block markers like ```xml, only return XML content.',
      '7. Ensure generated code is complete and runnable with all necessary imports.',
      '8. For simple tools or single-page apps, do NOT use vue-router.',
      '   Render main components directly in App.vue (e.g., <JsonFormatter />).',
      '   Only use routing when multi-page navigation is truly needed, and must generate router/index.ts config.'
    ].join('\n')

    let userContent = 'Development Requirement:\n' + request.requirement

    if (request.files && Object.keys(request.files).length > 0) {
      const structure = this.analyzeStructure(request.files)
      const dependencies = this.getDependencies(request.files)
      
      userContent = 'Project Directory Structure:\n' + structure + '\n\n' +
        'Package.json Dependencies:\n' + dependencies + '\n\n' +
        'Development Requirement:\n' + request.requirement
    }

    return [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userContent }
    ]
  }

  private analyzeStructure(files: Record<string, string>): string {
    const paths = Object.keys(files).sort()
    const result: string[] = []
    
    for (const path of paths) {
      const parts = path.split('/')
      const indent = '  '.repeat(parts.length - 1)
      const fileName = parts[parts.length - 1]
      const ext = fileName.split('.').pop()
      const icon = this.getFileIcon(ext)
      result.push(indent + icon + ' ' + fileName)
    }
    
    return result.join('\n')
  }

  private getFileIcon(ext?: string): string {
    const icons: Record<string, string> = {
      vue: '[VUE]',
      ts: '[TS]',
      js: '[JS]',
      json: '[JSON]',
      css: '[CSS]',
      scss: '[SCSS]',
      less: '[LESS]',
      html: '[HTML]',
      md: '[MD]',
      png: '[IMG]',
      jpg: '[IMG]',
      svg: '[IMG]'
    }
    return icons[ext || ''] || '[FILE]'
  }

  private getDependencies(files: Record<string, string>): string {
    for (const [path, content] of Object.entries(files)) {
      if (path.endsWith('package.json')) {
        try {
          const pkg = JSON.parse(content)
          const deps = { ...pkg.dependencies, ...pkg.devDependencies }
          const lines = ['{']
          for (const [name, version] of Object.entries(deps)) {
            lines.push('  "' + name + '": "' + version + '"')
          }
          lines.push('}')
          return lines.join('\n')
        } catch {
          return 'Failed to parse package.json'
        }
      }
    }
    return 'No package.json found, using default Vue 3 config'
  }

  private getConfig() {
    return this.config.config || {}
  }

  private async callOpenAICompatible(messages: any[]): Promise<AIResponse> {
    const cfg = this.getConfig()
    const baseUrl = cfg.baseUrl || 'https://api.openai.com/v1'
    const apiKey = cfg.apiKey
    const modelId = this.config.modelId

    const response = await fetch(baseUrl + '/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + apiKey
      },
      body: JSON.stringify({
        model: modelId,
        messages,
        max_tokens: cfg.maxTokens || 4096,
        temperature: cfg.temperature || 0.7
      })
    })

    if (!response.ok) {
      const error = await response.text()
      throw new Error('OpenAI API Error: ' + response.status + ' ' + error)
    }

    const data = await response.json()
    return {
      content: data.choices[0].message.content,
      usage: data.usage
    }
  }

  private async callAnthropic(messages: any[]): Promise<AIResponse> {
    const cfg = this.getConfig()
    const apiKey = cfg.apiKey
    const modelId = this.config.modelId

    const userMessages = messages.filter(function(m: any) { return m.role !== 'system' })
    
    const response = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': apiKey,
        'anthropic-version': '2023-06-01'
      },
      body: JSON.stringify({
        model: modelId,
        messages: userMessages,
        max_tokens: cfg.maxTokens || 4096
      })
    })

    if (!response.ok) {
      const error = await response.text()
      throw new Error('Anthropic API Error: ' + response.status + ' ' + error)
    }

    const data = await response.json()
    return {
      content: data.content[0].text,
      usage: {
        promptTokens: data.usage.input_tokens,
        completionTokens: data.usage.output_tokens,
        totalTokens: data.usage.input_tokens + data.usage.output_tokens
      }
    }
  }

  private async callZhipu(messages: any[]): Promise<AIResponse> {
    const cfg = this.getConfig()
    const baseUrl = cfg.baseUrl || 'https://open.bigmodel.cn/api/paas/v4'
    const apiKey = cfg.apiKey
    const modelId = this.config.modelId

    const response = await fetch(baseUrl + '/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': apiKey
      },
      body: JSON.stringify({
        model: modelId,
        messages,
        max_tokens: cfg.maxTokens || 4096,
        temperature: cfg.temperature || 0.7
      })
    })

    if (!response.ok) {
      const error = await response.text()
      throw new Error('Zhipu API Error: ' + response.status + ' ' + error)
    }

    const data = await response.json()
    return {
      content: data.choices[0].message.content
    }
  }

  private async callQwen(messages: any[]): Promise<AIResponse> {
    const cfg = this.getConfig()
    const apiKey = cfg.apiKey
    const modelId = this.config.modelId

    const response = await fetch('https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + apiKey
      },
      body: JSON.stringify({
        model: modelId,
        messages,
        max_tokens: cfg.maxTokens || 4096,
        temperature: cfg.temperature || 0.7
      })
    })

    if (!response.ok) {
      const error = await response.text()
      throw new Error('Qwen API Error: ' + response.status + ' ' + error)
    }

    const data = await response.json()
    return {
      content: data.choices[0].message.content
    }
  }

  private parseGeneratedContent(content: string): GeneratedFile[] {
    const files: GeneratedFile[] = []
    
    const cleanedContent = content
      .replace(/```xml\s*/g, '')
      .replace(/```\s*/g, '')
      .replace(/<\?xml[^>]*\?>/g, '')
      .trim()

    const filePattern = /<file path="([^"]+)">\s*(?:<!\[CDATA\[)?(.*?)(?:\]\]>)?\s*<\/file>/gs
    let match
    
    while ((match = filePattern.exec(cleanedContent)) !== null) {
      let path = match[1].trim()
      let fileContent = match[2].trim()
      
      if (path.startsWith('src/')) {
        path = path.substring(4)
      }
      
      files.push({ path: path, content: fileContent })
    }

    if (files.length === 0) {
      const fileNameMatch = content.match(/create\s+(\w+\.vue)/i)
      const fileName = fileNameMatch ? fileNameMatch[1] : 'NewComponent.vue'
      const filePath = 'components/' + fileName
      
      files.push({
        path: filePath,
        content: this.extractCodeFromMarkdown(content)
      })
    }

    return files
  }

  private extractCodeFromMarkdown(content: string): string {
    const codeBlockPattern = /```(?:vue|html|xml)?\s*([\s\S]*?)```/g
    const matches = codeBlockPattern.exec(content)
    if (matches && matches[1]) {
      return matches[1].trim()
    }
    return content.replace(/```vue\s*/, '').replace(/```\s*$/, '').trim()
  }
}

export async function generateWithClient(
  provider: AIProvider,
  config: UserAIConfig,
  request: GenerateRequest
): Promise<AIResponse> {
  const client = new AIClient(provider, config)
  return client.generate(request)
}
