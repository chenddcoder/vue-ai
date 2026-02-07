<template>
  <div ref="editorContainer" class="editor-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'
import { useProjectStore } from '@/stores/project'

// Worker setup for Monaco
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

// Define MonacoEnvironment on window/self
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

onMounted(() => {
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: projectStore.files[projectStore.activeFile] || '',
      language: 'html', // Default to html for Vue files for basic highlighting
      theme: 'vs-dark',
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: 14,
      scrollBeyondLastLine: false
    })

    editor.onDidChangeModelContent(() => {
      if (editor) {
        projectStore.updateFile(projectStore.activeFile, editor.getValue())
      }
    })
    
    // Initial content sync
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
    
    // Simple language detection
    const ext = newFile.split('.').pop()
    let lang = 'plaintext'
    if (ext === 'vue' || ext === 'html') lang = 'html'
    else if (ext === 'ts') lang = 'typescript'
    else if (ext === 'js') lang = 'javascript'
    else if (ext === 'css') lang = 'css'
    else if (ext === 'json') lang = 'json'
    
    monaco.editor.setModelLanguage(editor.getModel()!, lang)
  }
})

onBeforeUnmount(() => {
  editor?.dispose()
})
</script>

<style scoped>
.editor-container {
  width: 100%;
  height: 100%;
}
</style>
