<template>
  <div class="file-tree">
    <a-tree
      :tree-data="treeData"
      :selected-keys="selectedKeys"
      @select="onSelect"
      :show-icon="true"
      block-node
    >
      <template #title="{ title, key }">
        <a-dropdown :trigger="['contextmenu']">
          <span class="file-title">
            <FileTextOutlined v-if="isFile(key)" />
            <FolderOutlined v-else />
            <span>{{ title }}</span>
          </span>
          <template #overlay>
            <a-menu>
              <a-menu-item key="rename" @click="editFileName(key)" v-if="isFile(key)">
                <EditOutlined />
                重命名
              </a-menu-item>
              <a-menu-item key="delete" @click="deleteFile(key)" danger>
                <DeleteOutlined />
                删除
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </template>
      <template #icon="{ dataRef }">
        <FolderOutlined v-if="!dataRef.isLeaf" />
        <component :is="getFileIcon(dataRef.key)" v-else />
      </template>
    </a-tree>
    <div class="actions">
      <a-dropdown :trigger="['click']">
        <a-button type="dashed" size="small" block>
          <PlusOutlined /> 新建文件
        </a-button>
        <template #overlay>
          <a-menu @click="(e: any) => handleCreate(e.key)">
            <a-menu-item key="vue">
              <span style="color: #42b883">V</span> Vue 文件
            </a-menu-item>
            <a-menu-item key="ts">
              <span style="color: #3178c6">TS</span> TypeScript 文件
            </a-menu-item>
            <a-menu-item key="js">
              <span style="color: #f7df1e">JS</span> JavaScript 文件
            </a-menu-item>
            <a-menu-item key="css">
              <span style="color: #264de4">#</span> CSS 文件
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useProjectStore } from '@/stores/project'
import { storeToRefs } from 'pinia'
import { 
  PlusOutlined, 
  FolderOutlined, 
  FileTextOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { h } from 'vue'

interface TreeNode {
  title: string
  key: string
  isLeaf?: boolean
  children?: TreeNode[]
}

const store = useProjectStore()
const { activeFile, files } = storeToRefs(store)

const selectedKeys = computed(() => [activeFile.value])

const isFile = (key: string) => key.includes('.')

const getFileIcon = (key: string) => {
  if (key.endsWith('.vue')) {
    return () => h('span', { style: { color: '#42b883', fontWeight: 'bold' } }, 'Vue')
  }
  if (key.endsWith('.ts')) {
    return () => h('span', { style: { color: '#3178c6', fontWeight: 'bold' } }, 'TS')
  }
  if (key.endsWith('.js')) {
    return () => h('span', { style: { color: '#f7df1e', fontWeight: 'bold' } }, 'JS')
  }
  if (key.endsWith('.css')) {
    return () => h('span', { style: { color: '#264de4', fontWeight: 'bold' } }, 'CSS')
  }
  return FileTextOutlined
}

const buildTree = (): TreeNode[] => {
  const root: TreeNode[] = []
  const dirs: Record<string, TreeNode> = {}
  
  Object.keys(files.value).forEach(filename => {
    const parts = filename.split('/')
    let currentPath = ''
    
    for (let i = 0; i < parts.length; i++) {
      const part = parts[i]
      const isLast = i === parts.length - 1
      
      if (isLast) {
        currentPath = currentPath ? `${currentPath}/${part}` : part
        root.push({
          title: part,
          key: currentPath,
          isLeaf: true
        })
      } else {
        const dirPath = currentPath ? `${currentPath}/${part}` : part
        currentPath = dirPath
        
        if (!dirs[dirPath]) {
          dirs[dirPath] = {
            title: part,
            key: dirPath,
            isLeaf: false,
            children: []
          }
          root.push(dirs[dirPath])
        }
      }
    }
  })
  
  return root
}

const treeData = computed(() => buildTree())

const onSelect = (keys: any[]) => {
  if (keys.length > 0) {
    const key = keys[0] as string
    if (files.value[key] !== undefined) {
      store.setActiveFile(key)
    }
  }
}

const getFileTemplate = (type: string) => {
  const templates: Record<string, string> = {
    vue: '<template>\n  <div></div>\n</template>\n\n<scr' + 'ipt setup lang="ts">\n</scr' + 'ipt>\n\n<style scoped>\n</style>',
    ts: '// TypeScript file\nexport const hello = \'world\'',
    js: '// JavaScript file\nexport const hello = \'world\'',
    css: '/* CSS */\n.hello { color: red; }'
  }
  return templates[type] || ''
}

const handleCreate = ({ key }: { key: string }) => {
  const extMap: Record<string, string> = {
    vue: '.vue',
    ts: '.ts',
    js: '.js',
    css: '.css'
  }
  
  const ext = extMap[key] || '.vue'
  const defaultName = `NewFile${ext}`
  const name = prompt(`输入文件名:`, defaultName)
  if (!name) return
  
  const filename = name.endsWith(ext) ? name : `${name}${ext}`
  
  if (files.value[filename] !== undefined) {
    alert('文件已存在')
    return
  }
  
  store.updateFile(filename, getFileTemplate(key))
  store.setActiveFile(filename)
}

const editFileName = (oldKey: string) => {
  const parts = oldKey.split('/')
  const oldName = parts.pop()!
  const parentPath = parts.join('/')
  
  const newName = prompt('输入新文件名:', oldName)
  if (!newName || newName === oldName) return
  
  const newKey = parentPath ? `${parentPath}/${newName}` : newName
  
  if (files.value[newKey] !== undefined) {
    alert('同名文件已存在')
    return
  }
  
  const content = files.value[oldKey]
  store.updateFile(newKey, content)
  store.deleteFile(oldKey)
  
  if (activeFile.value === oldKey) {
    store.setActiveFile(newKey)
  }
}

const deleteFile = (key: string) => {
  if (confirm(`确定删除文件 "${key}" 吗？`)) {
    store.deleteFile(key)
    if (activeFile.value === key) {
      store.setActiveFile('App.vue')
    }
  }
}
</script>

<style scoped>
.file-tree {
  padding: 10px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.file-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.actions {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

:deep(.ant-tree-node-content-wrapper) {
  flex: 1;
}
</style>
