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

  function updateFile(filename: string, content: string) {
    files.value[filename] = content
  }

  function setActiveFile(filename: string) {
    activeFile.value = filename
  }

  return { files, activeFile, updateFile, setActiveFile }
})
