<template>
  <div class="editor-wrapper">
    <div class="editor-toolbar">
      <a-space>
        <a-tooltip title="格式化代码 (Ctrl+Shift+F)">
          <a-button size="small" @click="formatCode">
            <template #icon><FormatPainterOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="AI解释代码 (Ctrl+/)">
          <a-button size="small" @click="explainCode" :loading="explaining">
            <template #icon><BulbOutlined /></template>
            解释
          </a-button>
        </a-tooltip>
        <a-tooltip title="AI优化代码">
          <a-button size="small" @click="optimizeCode" :loading="optimizing">
            <template #icon><ToolOutlined /></template>
            优化
          </a-button>
        </a-tooltip>
      </a-space>
      <div class="editor-status">
        <span v-if="cursorPosition" class="cursor-pos">{{ cursorPosition }}</span>
      </div>
    </div>
    <div ref="editorContainer" class="editor-container"></div>
    
    <a-modal
      v-model:open="explainModalVisible"
      title="AI 代码解释"
      :width="700"
      :footer="null"
    >
      <div class="explain-content">
        <pre>{{ aiExplanation }}</pre>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'
import { useProjectStore } from '@/stores/project'
import { generateCode } from '@/api'
import { message } from 'ant-design-vue'
import { FormatPainterOutlined, BulbOutlined, ToolOutlined } from '@ant-design/icons-vue'

// Worker setup for Monaco
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

self.MonacoEnvironment = {
  getWorker(_: any, label: string) {
    if (label === 'json') {
      return new jsonWorker()
    }
    if (label === 'css' || label === 'scss' || label === 'less') {
      return new cssWorker()
    }
    if (label === 'html' || label === 'handlebars' || label === 'razor') {
      return new htmlWorker()
    }
    if (label === 'typescript' || label === 'javascript') {
      return new tsWorker()
    }
    return new editorWorker()
  }
}

const editorContainer = ref<HTMLElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null
const projectStore = useProjectStore()

const cursorPosition = ref('')
const explaining = ref(false)
const optimizing = ref(false)
const explainModalVisible = ref(false)
const aiExplanation = ref('')

const getLanguage = (fileName: string) => {
  const ext = fileName.split('.').pop()
  let lang = 'plaintext'
  if (ext === 'vue' || ext === 'html') lang = 'html'
  else if (ext === 'ts') lang = 'typescript'
  else if (ext === 'js') lang = 'javascript'
  else if (ext === 'css') lang = 'css'
  else if (ext === 'json') lang = 'json'
  return lang
}

const formatCode = () => {
  if (editor) {
    editor.getAction('editor.action.formatDocument')?.run()
  }
}

const explainCode = async () => {
  if (!editor) return
  const selectedText = editor.getModel()?.getValueInRange(editor.getSelection()!)
  const code = selectedText || editor.getValue()
  
  if (!code.trim()) {
    message.warning('请先选择代码或确保编辑器有内容')
    return
  }
  
  explaining.value = true
  try {
    const res: any = await generateCode({
      prompt: `请用中文解释以下代码的功能和工作原理。如果有选择部分，只解释选中的部分：\n\n${code}`,
      maxTokens: 2000
    })
    
    if (res.code === 200 && res.data) {
      aiExplanation.value = res.data.content || res.data.message || '暂无解释'
      explainModalVisible.value = true
    } else {
      message.error(res.message || 'AI解释失败')
    }
  } catch (error: any) {
    message.error('AI解释失败: ' + error.message)
  } finally {
    explaining.value = false
  }
}

const optimizeCode = async () => {
  if (!editor) return
  const selectedText = editor.getModel()?.getValueInRange(editor.getSelection()!)
  const code = selectedText || editor.getValue()
  
  if (!code.trim()) {
    message.warning('请先选择代码或确保编辑器有内容')
    return
  }
  
  optimizing.value = true
  try {
    const res: any = await generateCode({
      prompt: `请优化以下代码，可以从性能、可读性、最佳实践等方面进行优化。只返回优化后的代码，不要有其他解释：\n\n${code}`,
      maxTokens: 2000
    })
    
    if (res.code === 200 && res.data) {
      const optimizedCode = res.data.content || res.data.message
      if (selectedText) {
        const selection = editor.getSelection()
        if (selection) {
          editor.executeEdits('ai-optimize', [{
            range: selection,
            text: optimizedCode
          }])
        }
      } else {
        editor.setValue(optimizedCode)
      }
      message.success('代码已优化')
    } else {
      message.error(res.message || 'AI优化失败')
    }
  } catch (error: any) {
    message.error('AI优化失败: ' + error.message)
  } finally {
    optimizing.value = false
  }
}

onMounted(() => {
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: projectStore.files[projectStore.activeFile] || '',
      language: 'html',
      theme: 'vs-dark',
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: 14,
      scrollBeyondLastLine: false,
      quickSuggestions: true,
      suggestOnTriggerCharacters: true,
      acceptSuggestionOnEnter: 'on',
      tabSize: 2,
      formatOnPaste: true,
      formatOnType: true
    })

    editor.onDidChangeModelContent(() => {
      if (editor) {
        projectStore.updateFile(projectStore.activeFile, editor.getValue())
      }
    })

    editor.onDidChangeCursorPosition((e) => {
      cursorPosition.value = `行 ${e.position.lineNumber}, 列 ${e.position.column}`
    })

    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.KeyF, () => {
      formatCode()
    })

    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Slash, () => {
      explainCode()
    })

    const initialContent = projectStore.files[projectStore.activeFile] || ''
    if (editor.getValue() !== initialContent) {
      editor.setValue(initialContent)
    }
  }
})

watch(() => projectStore.files[projectStore.activeFile], (newContent) => {
  if (editor && newContent !== editor.getValue()) {
    editor.setValue(newContent || '')
  }
})

watch(() => projectStore.activeFile, (newFile) => {
  if (editor) {
    const content = projectStore.files[newFile]
    if (editor.getValue() !== content) {
      editor.setValue(content)
    }
    monaco.editor.setModelLanguage(editor.getModel()!, getLanguage(newFile))
  }
})

onBeforeUnmount(() => {
  editor?.dispose()
})
</script>

<style scoped>
.editor-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background: #1e1e1e;
  border-bottom: 1px solid #333;
}
.editor-status {
  color: #888;
  font-size: 12px;
}
.cursor-pos {
  font-family: monospace;
}
.editor-container {
  flex: 1;
  width: 100%;
}
.explain-content {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 8px;
  max-height: 400px;
  overflow: auto;
}
.explain-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: inherit;
}
</style>
