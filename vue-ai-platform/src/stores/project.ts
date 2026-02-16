import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

let autoSaveTimer: ReturnType<typeof setTimeout> | null = null
let lastSavedContent: string = ''

export const useProjectStore = defineStore('project', () => {
  const files = ref<Record<string, string>>({
    'App.vue': `<template>
  <h1>Hello World</h1>
</template>
<script setup>
</script>
<style>
h1 { color: red; }
</style>`
  })
  const activeFile = ref('App.vue')
  const currentProjectId = ref<number | null>(null)
  const fromMyApps = ref(false)
  const projectName = ref('')
  const lastAutoSaveTime = ref<Date | null>(null)
  const isAutoSaving = ref(false)
  const autoSaveEnabled = ref(true)
  const autoSaveDelay = 3000

  function updateFile(filename: string, content: string) {
    files.value[filename] = content
  }

  function deleteFile(filename: string) {
    delete files.value[filename]
  }

  function setActiveFile(filename: string) {
    activeFile.value = filename
  }

  function setCurrentProjectId(id: number) {
    currentProjectId.value = id
  }

  function setFromMyApps(flag: boolean) {
    fromMyApps.value = flag
  }

  function setProjectName(name: string) {
    projectName.value = name
  }

  function setFiles(newFiles: Record<string, string>) {
    files.value = newFiles
    lastSavedContent = JSON.stringify(newFiles)
  }

  function clearProjectContext() {
    currentProjectId.value = null
    fromMyApps.value = false
    projectName.value = ''
    lastAutoSaveTime.value = null
    files.value = {
      'App.vue': `<template>
  <h1>Hello World</h1>
</template>
<script setup>
</script>
<style>
h1 { color: red; }
</style>`
    }
    activeFile.value = 'App.vue'
    lastSavedContent = JSON.stringify(files.value)
  }

  function getContentHash(): string {
    return JSON.stringify(files.value)
  }

  function hasUnsavedChanges(): boolean {
    return getContentHash() !== lastSavedContent
  }

  function markAsSaved() {
    lastSavedContent = getContentHash()
  }

  return { 
    files, 
    activeFile, 
    updateFile, 
    deleteFile,
    setActiveFile,
    setFiles,
    currentProjectId,
    fromMyApps,
    projectName,
    setCurrentProjectId,
    setFromMyApps,
    setProjectName,
    clearProjectContext,
    lastAutoSaveTime,
    isAutoSaving,
    autoSaveEnabled,
    autoSaveDelay,
    getContentHash,
    hasUnsavedChanges,
    markAsSaved
  }
})
