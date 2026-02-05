<template>
  <a-layout style="height: 100vh">
    <a-layout-header class="header">
      <div class="logo">Vue AI Platform</div>
      <a-menu theme="dark" mode="horizontal" :style="{ lineHeight: '64px' }">
        <a-menu-item key="1">File</a-menu-item>
        <a-menu-item key="2" @click="save">Save</a-menu-item>
        <a-menu-item key="3">Publish</a-menu-item>
      </a-menu>
    </a-layout-header>
    <a-layout>
      <a-layout-sider width="200" style="background: #fff">
        <FileTree />
      </a-layout-sider>
      <a-layout style="padding: 0 24px 24px">
        <a-layout-content :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px', display: 'flex' }">
          <div class="editor-pane">
            <MonacoEditor />
          </div>
          <div class="preview-pane">
            <Preview />
          </div>
          <div class="ai-pane" v-if="showAI">
            <AIAssistant />
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import FileTree from '@/components/FileTree.vue'
import MonacoEditor from '@/components/editor/MonacoEditor.vue'
import Preview from '@/components/preview/Preview.vue'
import AIAssistant from '@/components/editor/AIAssistant.vue'
import { useProjectStore } from '@/stores/project'
import { saveProject } from '@/api'
import { message } from 'ant-design-vue'

const showAI = ref(true)
const projectStore = useProjectStore()

const save = async () => {
  try {
    // Mock user ID and project details for now
    await saveProject({
      id: 1, // hardcoded for demo
      name: 'Demo Project',
      description: 'Vue AI Project',
      ownerId: 1,
      content: projectStore.files
    })
    message.success('Project saved!')
  } catch (err: any) {
    message.error('Failed to save: ' + err.message)
  }
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
}
.logo {
  color: white;
  font-size: 1.2rem;
  margin-right: 2rem;
}
.editor-pane, .preview-pane {
  flex: 1;
  border: 1px solid #eee;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.ai-pane {
  width: 300px;
  height: 100%;
  border-left: 1px solid #ccc;
}
</style>
