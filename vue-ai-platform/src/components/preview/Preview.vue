<template>
  <div class="preview-container">
    <iframe ref="iframe" src="/sandbox.html" class="sandbox-iframe" sandbox="allow-scripts allow-same-origin"></iframe>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useProjectStore } from '@/stores/project'
import { storeToRefs } from 'pinia'

const iframe = ref<HTMLIFrameElement | null>(null)
const projectStore = useProjectStore()
const { files } = storeToRefs(projectStore)

const handleMessage = (event: MessageEvent) => {
  const data = event.data
  if (data.type === 'sandboxReady') {
    refreshSandbox()
  } else if (data.type === 'getFile') {
    // Sandbox asks for file content
    // Remove leading slash if present
    const filename = data.filename.startsWith('/') ? data.filename.slice(1) : data.filename
    const content = projectStore.files[filename] ?? null
    
    if (iframe.value && iframe.value.contentWindow) {
      iframe.value.contentWindow.postMessage({
        type: 'fileContent',
        id: data.id,
        content
      }, '*')
    }
  }
}

const refreshSandbox = () => {
  if (iframe.value && iframe.value.contentWindow) {
    iframe.value.contentWindow.postMessage({ type: 'reload' }, '*')
  }
}

// Debounced refresh on file change
let timeout: any = null
watch(files, () => {
  clearTimeout(timeout)
  timeout = setTimeout(() => {
    refreshSandbox()
  }, 500) // 500ms delay as per requirement
}, { deep: true })

onMounted(() => {
  window.addEventListener('message', handleMessage)
})

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage)
})
</script>

<style scoped>
.preview-container {
  width: 100%;
  height: 100%;
}
.sandbox-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
