<script setup lang="ts">
import { RouterView } from 'vue-router'
import { onMounted } from 'vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

onMounted(() => {
  // 初始化主题
  themeStore.updateTheme()
})
</script>

<template>
  <RouterView />
</template>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
}

/* 深色模式全局样式 */
.dark-theme {
  color-scheme: dark;
}

.dark-theme body {
  background-color: #141414;
  color: rgba(255, 255, 255, 0.85);
}

/* 确保ant-design-vue组件在深色模式下正常工作 */
.dark-theme .ant-layout {
  background: #141414;
}

.dark-theme .ant-layout-header {
  background: #001529;
}

.dark-theme .ant-layout-content {
  background: #141414;
}

.dark-theme .ant-layout-sider {
  background: #141414;
}

.dark-theme .ant-card {
  background: #1f1f1f;
  border-color: #303030;
}

.dark-theme .ant-card-head {
  background: transparent;
  border-color: #303030;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-card-body {
  background: transparent;
}

.dark-theme .ant-menu {
  background: #1f1f1f;
}

.dark-theme .ant-menu-item {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-menu-item:hover {
  color: #1890ff;
}

.dark-theme .ant-menu-item-selected {
  background: #1890ff;
  color: #fff;
}

.dark-theme .ant-input,
.dark-theme .ant-input-affix-wrapper {
  background: transparent;
  border-color: #434343;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.dark-theme .ant-btn-default {
  background: transparent;
  border-color: #434343;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-btn-default:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.dark-theme .ant-select-selector {
  background: transparent !important;
  border-color: #434343 !important;
  color: rgba(255, 255, 255, 0.85) !important;
}

.dark-theme .ant-select-arrow {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .ant-modal-content,
.dark-theme .ant-modal-header {
  background: #1f1f1f;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-modal-title {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-form-item-label > label {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-divider {
  border-color: #303030;
}

.dark-theme .ant-empty-description {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .ant-tabs-tab {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-tabs-tab-active {
  color: #1890ff;
}

.dark-theme .ant-tag {
  background: transparent;
  border-color: #434343;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-dropdown-menu {
  background: #1f1f1f;
}

.dark-theme .ant-dropdown-menu-item {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-dropdown-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

/* 表格样式 */
.dark-theme .ant-table {
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-table-thead > tr > th {
  background: #1f1f1f;
  color: rgba(255, 255, 255, 0.85);
  border-color: #303030;
}

.dark-theme .ant-table-tbody > tr > td {
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
  border-color: #303030;
}

.dark-theme .ant-table-tbody > tr:hover > td {
  background: rgba(255, 255, 255, 0.08);
}

/* 列表样式 */
.dark-theme .ant-list {
  background: transparent;
}

.dark-theme .ant-list-item {
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
  border-color: #303030;
}

/* 描述列表 */
.dark-theme .ant-descriptions-item-label {
  background: #1f1f1f;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-descriptions-item-content {
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
}

/* 统计数字 */
.dark-theme .ant-statistic-title {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .ant-statistic-content {
  color: rgba(255, 255, 255, 0.85);
}

/* 分页 */
.dark-theme .ant-pagination-item {
  background: transparent;
  border-color: #434343;
}

.dark-theme .ant-pagination-item a {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-pagination-item:hover {
  border-color: #1890ff;
}

.dark-theme .ant-pagination-item:hover a {
  color: #1890ff;
}

.dark-theme .ant-pagination-item-active {
  background: #1890ff;
  border-color: #1890ff;
}

.dark-theme .ant-pagination-item-active a {
  color: #fff;
}

.dark-theme .ant-pagination-prev,
.dark-theme .ant-pagination-next {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-pagination-disabled {
  color: rgba(255, 255, 255, 0.25);
}

/* 时间轴 */
.dark-theme .ant-timeline-item-head {
  background: #141414;
}

.dark-theme .ant-timeline-item-content {
  color: rgba(255, 255, 255, 0.85);
}

/* 警告提示 */
.dark-theme .ant-alert {
  background: #1f1f1f;
  border-color: #303030;
}

.dark-theme .ant-alert-message {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-alert-description {
  color: rgba(255, 255, 255, 0.65);
}

/* 步骤条 */
.dark-theme .ant-steps-item-title {
  color: rgba(255, 255, 255, 0.85) !important;
}

.dark-theme .ant-steps-item-description {
  color: rgba(255, 255, 255, 0.45) !important;
}

/* 头像 */
.dark-theme .ant-avatar {
  background: #303030;
}

/* 徽标 */
.dark-theme .ant-badge-count {
  background: #ff4d4f;
}

/* 进度条 */
.dark-theme .ant-progress-text {
  color: rgba(255, 255, 255, 0.65);
}

/* 滑块 */
.dark-theme .ant-slider-rail {
  background: #434343;
}

/* 开关 */
.dark-theme .ant-switch {
  background: #434343;
}

/* 单选按钮 */
.dark-theme .ant-radio-wrapper {
  color: rgba(255, 255, 255, 0.85);
}

/* 复选框 */
.dark-theme .ant-checkbox-wrapper {
  color: rgba(255, 255, 255, 0.85);
}

/* 折叠面板 */
.dark-theme .ant-collapse {
  background: transparent;
  border-color: #303030;
}

.dark-theme .ant-collapse-header {
  color: rgba(255, 255, 255, 0.85) !important;
  background: #1f1f1f;
}

.dark-theme .ant-collapse-content {
  background: transparent;
  border-color: #303030;
}

.dark-theme .ant-collapse-content-box {
  color: rgba(255, 255, 255, 0.85);
}

/* 文字提示 */
.dark-theme .ant-typography {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-typography-secondary {
  color: rgba(255, 255, 255, 0.45);
}

/* 结果 */
.dark-theme .ant-result-title {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-result-subtitle {
  color: rgba(255, 255, 255, 0.45);
}

/* 抽屉 */
.dark-theme .ant-drawer-content {
  background: #1f1f1f;
}

.dark-theme .ant-drawer-header {
  background: #1f1f1f;
  border-color: #303030;
}

.dark-theme .ant-drawer-title {
  color: rgba(255, 255, 255, 0.85);
}

/* 文本域 */
.dark-theme .ant-input-textarea {
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-input-textarea::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

/* 数字输入框 */
.dark-theme .ant-input-number {
  background: transparent;
  border-color: #434343;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-input-number-handler {
  background: #1f1f1f;
  border-color: #434343;
}

.dark-theme .ant-input-number-handler-up,
.dark-theme .ant-input-number-handler-down {
  color: rgba(255, 255, 255, 0.65);
}

/* 级联选择 */
.dark-theme .ant-cascader-picker {
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-cascader-menu {
  background: #1f1f1f;
  border-color: #303030;
}

.dark-theme .ant-cascader-menu-item {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-cascader-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

/* 日期选择器 */
.dark-theme .ant-picker {
  background: transparent;
  border-color: #434343;
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-picker-suffix {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .ant-picker-dropdown {
  background: #1f1f1f;
}

.dark-theme .ant-picker-panel {
  background: #1f1f1f;
  border-color: #303030;
}

.dark-theme .ant-picker-cell {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-picker-cell-in-view {
  color: rgba(255, 255, 255, 0.85);
}

/* 上传 */
.dark-theme .ant-upload {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-upload-list-item {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme .ant-upload-list-item-card-actions-btn {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .ant-upload-list-item-card-actions-btn:hover {
  color: rgba(255, 255, 255, 0.85);
}

/* 加载中 */
.dark-theme .ant-spin-text {
  color: rgba(255, 255, 255, 0.45);
}

/* 回到顶部 */
.dark-theme .ant-back-top {
  background: #1f1f1f;
  color: rgba(255, 255, 255, 0.85);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
}

/* 锚点 */
.dark-theme .ant-anchor {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-anchor-link-title {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-anchor-link-title-active {
  color: #1890ff;
}

/* 菜单 - 深色主题适配 */
.dark-theme .ant-menu-dark {
  background: #001529;
}

.dark-theme .ant-menu-dark .ant-menu-item {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-menu-dark .ant-menu-item:hover {
  color: #fff;
}

.dark-theme .ant-menu-dark .ant-menu-item-selected {
  background: #1890ff;
  color: #fff;
}

/* 面包屑 */
.dark-theme .ant-breadcrumb {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .ant-breadcrumb a {
  color: rgba(255, 255, 255, 0.65);
}

.dark-theme .ant-breadcrumb a:hover {
  color: #1890ff;
}

.dark-theme .ant-breadcrumb-separator {
  color: rgba(255, 255, 255, 0.45);
}

.dark-theme .ant-breadcrumb-link {
  color: rgba(255, 255, 255, 0.85);
}

/* 全局链接颜色 */
.dark-theme a {
  color: #1890ff;
}

.dark-theme a:hover {
  color: #40a9ff;
}

/* 段落文字 */
.dark-theme p {
  color: rgba(255, 255, 255, 0.85);
}

.dark-theme span {
  color: inherit;
}

/* 标题 */
.dark-theme h1,
.dark-theme h2,
.dark-theme h3,
.dark-theme h4,
.dark-theme h5,
.dark-theme h6 {
  color: rgba(255, 255, 255, 0.85);
}

/* 标签文字 */
.dark-theme label {
  color: rgba(255, 255, 255, 0.85);
}

/* 小字和次要文字 */
.dark-theme small {
  color: rgba(255, 255, 255, 0.45);
}

/* 强调文字 */
.dark-theme strong {
  color: rgba(255, 255, 255, 0.95);
}

/* 代码文字 */
.dark-theme code {
  background: #2d2d2d;
  color: #ff6b6b;
  border-color: #434343;
}

/* 引用文字 */
.dark-theme blockquote {
  color: rgba(255, 255, 255, 0.65);
  border-color: #434343;
}

/* 滚动条 */
.dark-theme ::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.dark-theme ::-webkit-scrollbar-track {
  background: #1f1f1f;
}

.dark-theme ::-webkit-scrollbar-thumb {
  background: #434343;
  border-radius: 4px;
}

.dark-theme ::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
