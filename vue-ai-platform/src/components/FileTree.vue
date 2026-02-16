<template>
  <div class="file-tree-container">
    <div class="file-tree-header">
      <span class="header-title">资源管理器</span>
      <div class="header-actions">
        <a-tooltip title="新建文件">
          <a-dropdown :trigger="['click']">
            <span class="action-btn"><FileAddOutlined /></span>
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
        </a-tooltip>
        <a-tooltip title="新建文件夹">
          <span class="action-btn" @click="createFolder('')"><FolderAddOutlined /></span>
        </a-tooltip>
        <a-tooltip title="刷新">
          <span class="action-btn"><ReloadOutlined /></span>
        </a-tooltip>
        <a-tooltip title="导入项目">
          <span class="action-btn" @click="handleImport"><UploadOutlined /></span>
        </a-tooltip>
        <input
          ref="fileInput"
          type="file"
          accept=".zip"
          style="display: none"
          @change="onFileSelected"
        />
        <a-tooltip title="导出项目">
          <span class="action-btn" @click="handleExport"><DownloadOutlined /></span>
        </a-tooltip>
      </div>
    </div>
    <div class="file-tree-content">
      <a-tree
        :tree-data="treeData"
        :selected-keys="selectedKeys"
        @select="onSelect"
        :show-icon="true"
        block-node
        :show-line="false"
        class="vscode-tree"
        draggable
        @drop="onDrop"
      >
        <template #switcherIcon="{ dataRef, expanded }">
          <DownOutlined v-if="!dataRef.isLeaf && expanded" style="font-size: 10px" />
          <RightOutlined v-else-if="!dataRef.isLeaf" style="font-size: 10px" />
        </template>
        <template #title="{ title, key, dataRef }">
          <a-dropdown :trigger="['contextmenu']">
            <span class="file-node-content">
              <span class="file-title">{{ title }}</span>
            </span>
            <template #overlay>
              <a-menu @click="(e: any) => handleMenuClick(e, key, dataRef)">
                <a-menu-item key="createFile" v-if="!isFile(key)">
                  <PlusOutlined />
                  新建文件
                </a-menu-item>
                <a-menu-item key="createFolder" v-if="!isFile(key)">
                  <FolderAddOutlined />
                  新建文件夹
                </a-menu-item>
                <a-menu-item key="rename">
                  <EditOutlined />
                  重命名
                </a-menu-item>
                <a-menu-item key="delete" danger>
                  <DeleteOutlined />
                  删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <template #icon="{ dataRef }">
          <FolderOutlined v-if="!dataRef.isLeaf && !dataRef.expanded" />
          <FolderOpenOutlined v-else-if="!dataRef.isLeaf && dataRef.expanded" />
          <component :is="getFileIcon(dataRef.key)" v-else />
        </template>
      </a-tree>
    </div>
    
    <GitPanel
      v-if="store.currentProjectId"
      :project-id="store.currentProjectId"
      :project-content="store.getContentHash()"
      @refresh="handleRefresh"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useProjectStore } from '@/stores/project'
import { storeToRefs } from 'pinia'
import JSZip from 'jszip'
import { 
  PlusOutlined, 
  FolderOutlined, 
  FolderOpenOutlined,
  FolderAddOutlined,
  FileTextOutlined,
  EditOutlined,
  DeleteOutlined,
  FileAddOutlined,
  ReloadOutlined,
  DownloadOutlined,
  UploadOutlined,
  DownOutlined,
  RightOutlined
} from '@ant-design/icons-vue'
import { h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import GitPanel from './GitPanel.vue'

interface TreeNode {
  title: string
  key: string
  isLeaf?: boolean
  children?: TreeNode[]
  expanded?: boolean // Track expansion state locally if needed, or rely on a-tree
}

const store = useProjectStore()
const { activeFile, files } = storeToRefs(store)

const selectedKeys = computed(() => [activeFile.value])

const isFile = (key: string) => key.includes('.')

const getFileIcon = (key: string) => {
  if (key.endsWith('.vue')) {
    return () => h('span', { style: { color: '#42b883', fontWeight: 'bold', fontSize: '14px' } }, 'V')
  }
  if (key.endsWith('.ts')) {
    return () => h('span', { style: { color: '#3178c6', fontWeight: 'bold', fontSize: '14px' } }, 'TS')
  }
  if (key.endsWith('.js')) {
    return () => h('span', { style: { color: '#f7df1e', fontWeight: 'bold', fontSize: '14px' } }, 'JS')
  }
  if (key.endsWith('.css')) {
    return () => h('span', { style: { color: '#264de4', fontWeight: 'bold', fontSize: '14px' } }, '#')
  }
  return FileTextOutlined
}

const sortNodes = (nodes: TreeNode[]) => {
  nodes.sort((a, b) => {
    // Folders first
    if (a.isLeaf !== b.isLeaf) {
      return a.isLeaf ? 1 : -1
    }
    // Then alphabetical
    return a.title.localeCompare(b.title)
  })
  
  nodes.forEach(node => {
    if (node.children) {
      sortNodes(node.children)
    }
  })
}

const buildTree = (): TreeNode[] => {
  const root: TreeNode[] = []
  const nodeMap: Record<string, TreeNode> = {}
  
  // Sort files to ensure consistent order
  const filenames = Object.keys(files.value).sort()
  
  filenames.forEach(filename => {
    const parts = filename.split('/')
    let currentPath = ''
    let parentPath = ''
    
    for (let i = 0; i < parts.length; i++) {
      const part = parts[i]
      const isLast = i === parts.length - 1
      
      currentPath = currentPath ? `${currentPath}/${part}` : part
      
      if (isLast) {
        if (part !== '.placeholder') {
          const fileNode: TreeNode = {
            title: part,
            key: currentPath,
            isLeaf: true
          }
          
          if (parentPath && nodeMap[parentPath]) {
            nodeMap[parentPath].children?.push(fileNode)
          } else {
            root.push(fileNode)
          }
        }
      } else {
        if (!nodeMap[currentPath]) {
          const dirNode: TreeNode = {
            title: part,
            key: currentPath,
            isLeaf: false,
            children: []
          }
          nodeMap[currentPath] = dirNode
          
          if (parentPath && nodeMap[parentPath]) {
            nodeMap[parentPath].children?.push(dirNode)
          } else {
            root.push(dirNode)
          }
        }
      }
      parentPath = currentPath
    }
  })
  
  sortNodes(root)
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

  if (isFile(oldKey)) {
    if (!newName.includes('.')) {
      message.error('文件名必须包含扩展名')
      return
    }
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
  } else {
    const newKey = parentPath ? `${parentPath}/${newName}` : newName
    
    const filesToRename: Record<string, string> = {}
    Object.keys(files.value).forEach(filePath => {
      if (filePath.startsWith(oldKey + '/')) {
        const relativePath = filePath.substring(oldKey.length + 1)
        filesToRename[newKey + '/' + relativePath] = files.value[filePath]
      }
    })
    
    Object.keys(filesToRename).forEach(newPath => {
      store.updateFile(newPath, filesToRename[newPath])
      store.deleteFile(Object.keys(files.value).find(p => files.value[p] === filesToRename[newPath]) || '')
    })
    
    if (activeFile.value.startsWith(oldKey + '/')) {
      const relativePath = activeFile.value.substring(oldKey.length)
      store.setActiveFile(newKey + relativePath)
    }
  }
}

const deleteFile = (key: string) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除${isFile(key) ? '文件' : '文件夹'} "${key}" 吗？${!isFile(key) ? '\n注意：文件夹内的所有文件将被同时删除。' : ''}`,
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      try {
        if (!isFile(key)) {
          Object.keys(files.value).forEach(filePath => {
            if (filePath.startsWith(key + '/') || filePath === key) {
              store.deleteFile(filePath)
            }
          })
        } else {
          store.deleteFile(key)
        }
        if (activeFile.value === key || activeFile.value.startsWith(key + '/')) {
          store.setActiveFile('App.vue')
        }
        message.success('删除成功')
      } catch (error: any) {
        console.error('Delete error:', error)
        message.error('删除失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

const createFolder = (parentPath: string) => {
  const name = prompt('输入文件夹名称:', 'NewFolder')
  if (!name) return
  
  const folderPath = parentPath ? `${parentPath}/${name}` : name
  
  if (files.value[folderPath] !== undefined || Object.keys(files.value).some(p => p.startsWith(folderPath + '/'))) {
    alert('同名文件夹已存在')
    return
  }
  
  store.updateFile(`${folderPath}/.placeholder`, '')
  message.success(`文件夹 "${folderPath}" 已创建`)
}

const handleMenuClick = (e: any, pathKey: string, dataRef: TreeNode) => {
  const key = e?.key
  if (!key) return

  switch (key) {
    case 'createFile':
      createFileInFolder(pathKey)
      break
    case 'createFolder':
      createFolder(pathKey)
      break
    case 'rename':
      editFileName(pathKey)
      break
    case 'delete':
      deleteFile(pathKey)
      break
  }
}

const createFileInFolder = (parentPath: string) => {
  const key = 'vue'
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
  const filePath = parentPath ? `${parentPath}/${filename}` : filename
  
  if (files.value[filePath] !== undefined) {
    alert('文件已存在')
    return
  }
  
  store.updateFile(filePath, getFileTemplate(key))
  store.setActiveFile(filePath)
}

const handleRefresh = () => {
  // 刷新项目内容
  message.success('项目已刷新')
}

const handleExport = async () => {
  const zip = new JSZip()
  
  // Add files to zip
  Object.entries(files.value).forEach(([path, content]) => {
    // Remove leading slash if present (though usually keys don't have it in our store, but good to be safe)
    // Actually our store keys are like "src/App.vue" or "App.vue"
    const cleanPath = path.startsWith('/') ? path.slice(1) : path
    zip.file(cleanPath, content)
  })
  
  try {
    const blob = await zip.generateAsync({ type: 'blob' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${store.projectName || 'project'}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error) {
    console.error('Export failed:', error)
    message.error('导出失败')
  }
}

// 导入功能
const fileInput = ref<HTMLInputElement | null>(null)

const handleImport = () => {
  fileInput.value?.click()
}

const onFileSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  if (!file.name.endsWith('.zip')) {
    message.error('请选择ZIP文件')
    return
  }
  
  try {
    const zip = await JSZip.loadAsync(file)
    let importedCount = 0
    
    // 遍历ZIP中的所有文件
    for (const [path, zipEntry] of Object.entries(zip.files)) {
      // 跳过文件夹
      if (zipEntry.dir) continue
      
      // 读取文件内容
      const content = await zipEntry.async('string')
      
      // 添加到项目
      store.updateFile(path, content)
      importedCount++
    }
    
    if (importedCount > 0) {
      message.success(`成功导入 ${importedCount} 个文件`)
      // 如果有App.vue，设置为活动文件
      if (files.value['App.vue']) {
        store.setActiveFile('App.vue')
      }
    } else {
      message.warning('ZIP文件中没有找到可导入的文件')
    }
  } catch (error) {
    console.error('Import failed:', error)
    message.error('导入失败：文件格式错误')
  } finally {
    // 清空input，允许重复选择同一文件
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

const onDrop = (info: any) => {
  const dropKey = info.node.key as string
  const dragKey = info.dragNode.key as string
  const dropPos = info.node.pos.split('-')
  const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1])
  
  // 检查是否将父文件夹拖入子文件夹
  if (!isFile(dragKey) && (dropKey === dragKey || dropKey.startsWith(dragKey + '/'))) {
    return
  }

  let targetParentPath = ''
  
  // 如果是拖入文件夹内部
  if (!info.dropToGap) {
    // 如果目标是文件，则不允许拖入（实际上 Ant Design Vue 可能会阻止这种情况，或者我们可以在这里处理）
    // 这里假设如果 dropToGap 为 false，目标必须是文件夹
    if (isFile(dropKey)) {
      // 尝试获取目标文件的父目录作为目标目录
      const parts = dropKey.split('/')
      parts.pop()
      targetParentPath = parts.join('/')
    } else {
      targetParentPath = dropKey
    }
  } else {
    // 拖到缝隙中，目标是 dropKey 的同级目录
    const parts = dropKey.split('/')
    parts.pop()
    targetParentPath = parts.join('/')
  }

  const dragFileName = dragKey.split('/').pop()!
  const newKey = targetParentPath ? `${targetParentPath}/${dragFileName}` : dragFileName

  if (dragKey === newKey) return

  // 检查目标路径是否存在
  if (files.value[newKey] !== undefined) {
    message.error('目标路径已存在同名文件/文件夹')
    return
  }

  // 执行移动
  if (isFile(dragKey)) {
    const content = files.value[dragKey]
    store.updateFile(newKey, content)
    store.deleteFile(dragKey)
    
    if (activeFile.value === dragKey) {
      store.setActiveFile(newKey)
    }
  } else {
    // 移动文件夹
    const filesToMove: Record<string, string> = {}
    
    // 找出所有需要移动的文件
    Object.keys(files.value).forEach(filePath => {
      // 包括文件夹本身（如果有占位符）和子文件
      if (filePath === dragKey || filePath.startsWith(dragKey + '/')) {
        const relativePath = filePath.substring(dragKey.length)
        const newFilePath = newKey + relativePath
        filesToMove[newFilePath] = files.value[filePath]
      }
    })
    
    // 检查是否有冲突
    for (const newPath of Object.keys(filesToMove)) {
      if (files.value[newPath] !== undefined && !filesToMove[newPath]) { // 忽略自己移动到自己的情况（前面已拦截，这里是双重保险）
         // 这里的逻辑有点复杂，因为我们在批量移动。
         // 简单起见，如果根目标不存在，我们假设子目标也不会冲突，或者覆盖。
         // 文件系统的移动通常是允许覆盖或者提示。这里我们先直接移动。
      }
    }

    // 执行批量移动
    Object.keys(filesToMove).forEach(newPath => {
      store.updateFile(newPath, filesToMove[newPath])
    })
    
    // 删除旧文件
    Object.keys(files.value).forEach(filePath => {
      if (filePath === dragKey || filePath.startsWith(dragKey + '/')) {
        store.deleteFile(filePath)
      }
    })
    
    // 更新选中状态
    if (activeFile.value.startsWith(dragKey + '/')) {
      const relativePath = activeFile.value.substring(dragKey.length)
      store.setActiveFile(newKey + relativePath)
    }
  }
}
</script>

<style scoped>
.file-tree-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f8f8f8;
  border-right: 1px solid #e5e5e5;
}

.file-tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px 6px;
  font-size: 11px;
  font-weight: 600;
  color: #616161;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.header-actions {
  display: flex;
  gap: 2px;
}

.action-btn {
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  color: #616161;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.action-btn:hover {
  background-color: rgba(0, 0, 0, 0.06);
  color: #333;
}

.file-tree-content {
  flex: 1;
  overflow-y: auto;
  padding-top: 4px;
}

.file-node-content {
  display: flex;
  align-items: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-title {
  margin-left: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* VS Code Tree Styles */
:deep(.vscode-tree) {
  background: transparent;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #333;
}

:deep(.vscode-tree .ant-tree-treenode) {
  padding: 0 0 0 4px;
  width: 100%;
  position: relative;
}

:deep(.vscode-tree .ant-tree-node-content-wrapper) {
  line-height: 22px;
  padding: 0 4px;
  border-radius: 0;
  transition: none;
  min-height: 22px;
  display: flex;
  align-items: center;
}

:deep(.vscode-tree .ant-tree-node-content-wrapper:hover) {
  background-color: rgba(0, 0, 0, 0.04);
}

:deep(.vscode-tree .ant-tree-node-content-wrapper.ant-tree-node-selected) {
  background-color: #e4e6f1;
  color: #333;
}

/* Adjust switcher (arrow) */
:deep(.vscode-tree .ant-tree-switcher) {
  width: 20px;
  line-height: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}

:deep(.vscode-tree .ant-tree-switcher:hover) {
  color: #666;
}

/* Hide default folder icons if we are using custom ones in template, 
   but here we rely on the template slot so we just adjust spacing */
:deep(.vscode-tree .anticon) {
  font-size: 14px;
}

/* Scrollbar styling */
.file-tree-content::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.file-tree-content::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 5px;
}

.file-tree-content:hover::-webkit-scrollbar-thumb {
  background: #ccc;
}

.file-tree-content::-webkit-scrollbar-track {
  background: transparent;
}

/* 深色模式样式 */
.dark-theme .file-tree-container {
  background-color: #1f1f1f;
  border-right-color: #303030;
}
.dark-theme .file-tree-header {
  background: #2a2a2a;
  border-bottom-color: #303030;
}
.dark-theme .header-title {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme .action-btn {
  color: rgba(255, 255, 255, 0.65);
}
.dark-theme .action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme .file-tree-content {
  background: #1f1f1f;
}
.dark-theme :deep(.vscode-tree) {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme :deep(.vscode-tree .ant-tree-node-content-wrapper) {
  color: rgba(255, 255, 255, 0.85);
}
.dark-theme :deep(.vscode-tree .ant-tree-node-content-wrapper:hover) {
  background: rgba(255, 255, 255, 0.08);
}
.dark-theme :deep(.vscode-tree .ant-tree-node-selected) {
  background: #094771 !important;
  color: #fff;
}
.dark-theme :deep(.ant-tree-switcher) {
  color: rgba(255, 255, 255, 0.65);
}
</style>
