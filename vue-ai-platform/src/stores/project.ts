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

  function updateFile(filename: string, content: string) {
    files.value[filename] = content
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

  function clearProjectContext() {
    currentProjectId.value = null
    fromMyApps.value = false
  }

  return { 
    files, 
    activeFile, 
    updateFile, 
    setActiveFile,
    currentProjectId,
    fromMyApps,
    setCurrentProjectId,
    setFromMyApps,
    clearProjectContext
  }
})
