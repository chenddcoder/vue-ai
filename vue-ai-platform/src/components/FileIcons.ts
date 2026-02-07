import { h } from 'vue'
import { FileTextOutlined } from '@ant-design/icons-vue'

// Vue 文件图标
export const VueIcon = () => h('span', { style: { color: '#42b883' } }, 'Vue')

// JavaScript 文件图标
export const FileJsIcon = () => h('span', { style: { color: '#f7df1e' } }, 'JS')

// TypeScript 文件图标
export const FileTsIcon = () => h('span', { style: { color: '#3178c6' } }, 'TS')

// CSS 文件图标
export const FileCssIcon = () => h('span', { style: { color: '#264de4' } }, 'CSS')
