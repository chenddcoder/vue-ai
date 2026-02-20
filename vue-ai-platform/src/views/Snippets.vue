<template>
  <a-layout style="min-height: 100vh">
    <AppHeader />
    
    <a-layout-content class="snippets-page">
      <div class="page-header">
        <h1>代码片段库</h1>
        <a-button type="primary" @click="showAddModal = true">
          <template #icon><PlusOutlined /></template>
          添加片段
        </a-button>
      </div>

      <div class="filter-section">
        <a-space>
          <a-select v-model:value="selectedCategory" style="width: 150px" @change="loadSnippets" allowClear placeholder="全部分类">
            <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
          </a-select>
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索片段..."
            style="width: 250px"
            @search="loadSnippets"
            allowClear
          />
        </a-space>
      </div>

      <a-spin :spinning="loading">
        <div class="snippets-list" v-if="snippets.length > 0">
          <div
            v-for="snippet in snippets"
            :key="snippet.id"
            class="snippet-card"
          >
            <div class="snippet-header">
              <div class="snippet-title">
                <FileOutlined />
                <span>{{ snippet.title }}</span>
              </div>
              <div class="snippet-actions">
                <a-tag :color="getCategoryColor(snippet.category)">{{ snippet.category }}</a-tag>
                <a-dropdown>
                  <a-button type="text" size="small">
                    <MoreOutlined />
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="copySnippet(snippet)">
                        <CopyOutlined /> 复制代码
                      </a-menu-item>
                      <a-menu-item @click="editSnippet(snippet)">
                        <EditOutlined /> 编辑
                      </a-menu-item>
                      <a-menu-divider />
                      <a-menu-item danger @click="deleteSnippet(snippet)">
                        <DeleteOutlined /> 删除
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </div>
            </div>
            <div class="snippet-desc">{{ snippet.description }}</div>
            <div class="snippet-preview">
              <pre><code>{{ truncateCode(snippet.code) }}</code></pre>
            </div>
            <div class="snippet-meta">
              <span>{{ snippet.create_time }}</span>
            </div>
          </div>
        </div>
      </a-spin>

      <a-empty v-if="!loading && snippets.length === 0" description="暂无代码片段，点击上方按钮添加" />

      <a-modal
        v-model:open="showAddModal"
        :title="editingSnippet ? '编辑代码片段' : '添加代码片段'"
        width="700px"
        @ok="handleSave"
        :confirmLoading="saving"
      >
        <a-form :model="snippetForm" layout="vertical">
          <a-form-item label="标题" required>
            <a-input v-model:value="snippetForm.title" placeholder="片段标题" />
          </a-form-item>
          <a-form-item label="描述">
            <a-textarea v-model:value="snippetForm.description" placeholder="简短描述" :rows="2" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="分类" required>
                <a-select v-model:value="snippetForm.category" placeholder="选择分类">
                  <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="语言">
                <a-select v-model:value="snippetForm.language" placeholder="选择语言">
                  <a-select-option value="javascript">JavaScript</a-select-option>
                  <a-select-option value="typescript">TypeScript</a-select-option>
                  <a-select-option value="vue">Vue</a-select-option>
                  <a-select-option value="html">HTML</a-select-option>
                  <a-select-option value="css">CSS</a-select-option>
                  <a-select-option value="json">JSON</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="代码" required>
            <a-textarea
              v-model:value="snippetForm.code"
              placeholder="输入代码"
              :rows="10"
              style="font-family: monospace"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  FileOutlined, 
  MoreOutlined, 
  CopyOutlined, 
  EditOutlined, 
  DeleteOutlined 
} from '@ant-design/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/stores/user'

interface Snippet {
  id: number
  title: string
  description: string
  code: string
  category: string
  language: string
  create_time: string
}

const userStore = useUserStore()

const snippets = ref<Snippet[]>([])
const loading = ref(false)
const saving = ref(false)
const showAddModal = ref(false)
const editingSnippet = ref<Snippet | null>(null)
const searchKeyword = ref('')
const selectedCategory = ref<string | undefined>(undefined)

const categories = ['组件', '工具函数', '样式', 'API', 'Hooks', '其他']

const snippetForm = ref({
  title: '',
  description: '',
  code: '',
  category: '组件',
  language: 'vue'
})

const categoryColors: Record<string, string> = {
  '组件': 'blue',
  '工具函数': 'green',
  '样式': 'purple',
  'API': 'orange',
  'Hooks': 'cyan',
  '其他': 'default'
}

const getCategoryColor = (category: string) => categoryColors[category] || 'default'

const truncateCode = (code: string) => {
  const lines = code.split('\n').slice(0, 5)
  return lines.join('\n') + (code.split('\n').length > 5 ? '\n...' : '')
}

const loadSnippets = async () => {
  loading.value = true
  try {
    const stored = localStorage.getItem('codeSnippets')
    let allSnippets: Snippet[] = stored ? JSON.parse(stored) : []
    
    // 过滤
    allSnippets = allSnippets.filter(s => {
      const matchCategory = !selectedCategory.value || s.category === selectedCategory.value
      const matchKeyword = !searchKeyword.value || 
        s.title.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        s.description.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        s.code.toLowerCase().includes(searchKeyword.value.toLowerCase())
      return matchCategory && matchKeyword
    })
    
    snippets.value = allSnippets.sort((a, b) => 
      new Date(b.create_time).getTime() - new Date(a.create_time).getTime()
    )
  } finally {
    loading.value = false
  }
}

const handleSave = () => {
  if (!snippetForm.value.title.trim()) {
    message.error('请输入标题')
    return
  }
  if (!snippetForm.value.code.trim()) {
    message.error('请输入代码')
    return
  }
  
  saving.value = true
  
  try {
    const stored = localStorage.getItem('codeSnippets')
    let allSnippets: Snippet[] = stored ? JSON.parse(stored) : []
    
    if (editingSnippet.value) {
      // 编辑
      const index = allSnippets.findIndex(s => s.id === editingSnippet.value!.id)
      if (index !== -1) {
        allSnippets[index] = {
          ...editingSnippet.value,
          ...snippetForm.value,
          create_time: new Date().toISOString()
        }
      }
    } else {
      // 新增
      const newSnippet: Snippet = {
        id: Date.now(),
        ...snippetForm.value,
        create_time: new Date().toISOString()
      }
      allSnippets.push(newSnippet)
    }
    
    localStorage.setItem('codeSnippets', JSON.stringify(allSnippets))
    message.success(editingSnippet.value ? '修改成功' : '添加成功')
    
    showAddModal.value = false
    resetForm()
    loadSnippets()
  } finally {
    saving.value = false
  }
}

const editSnippet = (snippet: Snippet) => {
  editingSnippet.value = snippet
  snippetForm.value = { ...snippet }
  showAddModal.value = true
}

const deleteSnippet = (snippet: Snippet) => {
  const stored = localStorage.getItem('codeSnippets')
  if (stored) {
    let allSnippets: Snippet[] = JSON.parse(stored)
    allSnippets = allSnippets.filter(s => s.id !== snippet.id)
    localStorage.setItem('codeSnippets', JSON.stringify(allSnippets))
    message.success('删除成功')
    loadSnippets()
  }
}

const copySnippet = async (snippet: Snippet) => {
  try {
    await navigator.clipboard.writeText(snippet.code)
    message.success('代码已复制到剪贴板')
  } catch (e) {
    message.error('复制失败')
  }
}

const resetForm = () => {
  editingSnippet.value = null
  snippetForm.value = {
    title: '',
    description: '',
    code: '',
    category: '组件',
    language: 'vue'
  }
}

onMounted(() => {
  loadSnippets()
})
</script>

<style scoped>
.snippets-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  margin: 0;
}

.filter-section {
  margin-bottom: 24px;
}

.snippets-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
}

.snippet-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.snippet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.snippet-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.snippet-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.snippet-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
}

.snippet-preview {
  background: #f5f5f5;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 12px;
  max-height: 150px;
  overflow: hidden;
}

.snippet-preview pre {
  margin: 0;
  font-size: 12px;
}

.snippet-preview code {
  font-family: 'Monaco', 'Menlo', monospace;
}

.snippet-meta {
  font-size: 12px;
  color: #999;
}

.dark-theme .snippet-card {
  background: #1f1f1f;
  border-color: #303030;
}

.dark-theme .snippet-preview {
  background: #2a2a2a;
}

.dark-theme .snippet-desc {
  color: rgba(255, 255, 255, 0.65);
}
</style>
