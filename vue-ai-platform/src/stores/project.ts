import { defineStore } from 'pinia'
import { ref } from 'vue'

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

  function clearProjectContext() {
    currentProjectId.value = null
    fromMyApps.value = false
    projectName.value = ''
  }

  return { 
    files, 
    activeFile, 
    updateFile, 
    deleteFile,
    setActiveFile,
    currentProjectId,
    fromMyApps,
    projectName,
    setCurrentProjectId,
    setFromMyApps,
    setProjectName,
    clearProjectContext
  }
})
