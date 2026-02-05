<template>
  <div class="ai-assistant">
    <div class="chat-history">
      <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
        {{ msg.content }}
      </div>
    </div>
    <div class="input-area">
      <a-textarea
        v-model:value="prompt"
        placeholder="Describe what you want to build..."
        :auto-size="{ minRows: 2, maxRows: 5 }"
        @pressEnter.prevent="sendMessage"
      />
      <a-button type="primary" :loading="loading" @click="sendMessage">Send</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { generateCode } from '@/api'
import { useProjectStore } from '@/stores/project'
import { message } from 'ant-design-vue'

const prompt = ref('')
const loading = ref(false)
const messages = ref<{ role: 'user' | 'assistant', content: string }[]>([])
const projectStore = useProjectStore()

const sendMessage = async () => {
  if (!prompt.value.trim()) return

  const userPrompt = prompt.value
  messages.value.push({ role: 'user', content: userPrompt })
  prompt.value = ''
  loading.value = true

  try {
    const res = await generateCode(userPrompt)
    const { template, methods, style } = res.data
    
    // Construct Vue component
    const code = `${template}

<script>
${methods}
<\/script>

<style scoped>
${style}
</style>`

    // Create new file
    const filename = `Generated-${Date.now()}.vue`
    projectStore.updateFile(filename, code)
    projectStore.setActiveFile(filename)
    
    messages.value.push({ role: 'assistant', content: `Created ${filename} based on your description.` })
  } catch (err: any) {
    message.error('Failed to generate code: ' + err.message)
    messages.value.push({ role: 'assistant', content: 'Sorry, I encountered an error.' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-assistant {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-left: 1px solid #eee;
  background: #f9f9f9;
}
.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.message {
  margin-bottom: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  max-width: 80%;
}
.message.user {
  background: #1890ff;
  color: white;
  align-self: flex-end;
  margin-left: auto;
}
.message.assistant {
  background: #fff;
  border: 1px solid #ddd;
}
.input-area {
  padding: 10px;
  border-top: 1px solid #eee;
  background: #fff;
  display: flex;
  gap: 10px;
}
</style>
