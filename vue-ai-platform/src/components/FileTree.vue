<template>
  <div class="file-tree">
    <a-tree
      :tree-data="treeData"
      :selected-keys="[activeFile]"
      @select="onSelect"
      block-node
    >
      <template #title="{ title }">
        <span>{{ title }}</span>
      </template>
    </a-tree>
    <div class="actions">
      <a-button type="dashed" size="small" block @click="addNewFile">Add File</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useProjectStore } from '@/stores/project'
import { storeToRefs } from 'pinia'

const store = useProjectStore()
const { activeFile, files } = storeToRefs(store)

const treeData = computed(() => {
  return Object.keys(files.value).map(filename => ({
    title: filename,
    key: filename,
    isLeaf: true // simplified: flat structure for now
  }))
})

const onSelect = (keys: any[]) => {
  if (keys.length > 0) {
    store.setActiveFile(keys[0] as string)
  }
}

const addNewFile = () => {
  const name = prompt('File name:', 'NewComponent.vue')
  if (name && !files.value[name]) {
    store.updateFile(name, '<template>\n  <div>New Component</div>\n</template>')
    store.setActiveFile(name)
  }
}
</script>

<style scoped>
.file-tree {
  padding: 10px;
}
.actions {
  margin-top: 10px;
}
</style>
