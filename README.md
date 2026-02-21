# Vue AI Platform

一个 AI 驱动的应用开发平台，支持可视化编辑、代码生成和应用发布。

## 技术栈

| 类别 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Ant Design Vue 4 + Pinia + Monaco Editor |
| 后端 | Spring Boot 2.7 + SQLite + Hutool |
| 包管理 | pnpm |

---

## 当前版本: v0.18.0

### 核心功能

| 模块 | 功能 |
|------|------|
| 用户系统 | 注册、登录、游客模式、个人资料管理 |
| AI配置 | OpenAI/Claude/智谱/通义多提供商支持 |
| 项目管理 | 多文件、文件夹、项目CRUD、导入导出ZIP、Git版本管理 |
| AI助手 | 自然语言生成代码 |
| 应用市场 | 应用发布、浏览、安装、窗口管理、分类标签 |
| 社区功能 | 收藏、评论、关注、用户主页 |
| 版本管理 | 版本号自动管理、历史记录、回滚 |
| 应用详情 | 实时预览、评分评论、一键启动、社交分享 |
| 主题系统 | 深色模式、浅色模式、跟随系统 |
| Git管理 | 提交历史、分支管理、版本检出、提交对比、远程仓库 |
| 消息通知 | 评论回复提醒、获赞提醒、通知铃铛 |
| 顶部导航 | 统一顶部功能区组件、头像下拉菜单 |
| 智能搜索 | 全文搜索、热门标签、排序筛选 |
| 智能推荐 | 基于用户收藏的个性化推荐 |
| 模板市场 | 官方模板、用户模板、一键使用 |
| 代码片段库 | 代码片段保存、分类管理、复制复用 |
| 数据仪表盘 | 项目统计、应用数据、评分分布 |
| 活动日志 | 用户行为记录、活动时间线 |
| 评分算法 | 综合评分权重算法 |
| 国际化 | 中英文支持、语言切换 |

### v0.18.0 更新

- ✅ **用户活动日志**: 记录用户创建项目、发布应用、评论等行为，仪表盘展示活动时间线
- ✅ **应用评分权重算法**: 综合评分、点赞、浏览量、时效性计算应用综合得分
- ✅ **国际化支持**: 中英文双语支持，主题/语言切换组件集成

- ✅ **社交分享**: 应用详情页支持链接分享、嵌入代码、社交平台分享
- ✅ **消息通知系统**: 评论回复提醒、获赞提醒、通知铃铛组件
- ✅ **编辑器增强**: 
  - 代码补全（Monaco内置智能补全）
  - AI解释代码（选中代码点击解释按钮）
  - AI代码优化（选中代码点击优化按钮）
  - 代码格式化（Ctrl+Shift+F）
- ✅ **快捷键支持**:
  - Ctrl+S: 保存
  - Ctrl+Shift+F: 格式化代码
  - Ctrl+/: AI解释代码
- ✅ **后端通知服务**: 自动创建点赞/评论通知

### v0.14.0 更新

- ✅ **Git版本管理增强**: 为个人项目提供完整的Git版本控制
  - 提交历史查看（支持分页）
  - 分支管理（创建、删除、切换）
  - 版本检出（回滚到任意提交）
  - 提交详情查看
  - 初始化Git仓库
  - 远程仓库管理（添加、删除远程仓库）
- ✅ **项目管理增强**: 集成Git管理入口
- ✅ **后端API优化**: 统一响应数据格式

### v0.12.0 更新

- ✅ **用户资料编辑**: 支持修改用户名和个人简介
- ✅ **深色模式**: 支持深色/浅色主题切换，可跟随系统设置
- ✅ **项目导入**: 支持从ZIP文件导入项目

### v0.11.0 更新

- ✅ **应用分类标签**: 支持标签云、热门标签、标签筛选
- ✅ **标签管理**: 发布应用时可添加标签，便于分类检索
- ✅ **市场分类筛选**: 按分类和标签筛选应用

### v0.10.0 更新

- ✅ **应用详情页**: 实时iframe预览应用效果
- ✅ **评分评论**: 五星评分+评论系统
- ✅ **互动功能**: 点赞、收藏、浏览统计
- ✅ **一键启动**: 详情页直接启动应用到窗口系统

### v0.9.0 更新

- ✅ **客户端AI调用**: 前端直接调用AI API，降低延迟
- ✅ **智能回退**: CORS失败自动切换后端
- ✅ **项目上下文**: 客户端调用支持完整项目结构分析

---

## 快速开始

```bash
# 前端
cd vue-ai-platform
pnpm install
pnpm dev

# 后端
cd vue-ai-server
mvn spring-boot:run
```

- 前端: http://localhost:5173
- 后端: http://localhost:8081

---

## 项目结构

```
vue-ai/
├── vue-ai-platform/        # 前端
│   ├── src/
│   │   ├── api/            # API定义
│   │   ├── components/      # 组件
│   │   ├── services/       # 客户端服务
│   │   ├── stores/         # Pinia状态
│   │   ├── views/          # 页面
│   │   └── utils/          # 工具
│   └── vite.config.ts
│
└── vue-ai-server/          # 后端
    └── src/main/java/
        └── com.vueai/server/
            ├── config/     # 配置
            ├── controller/  # 控制器
            ├── dto/        # DTO
            ├── model/      # 数据模型
            └── service/     # 服务层
```

---

## API 接口

| 模块 | 接口 | 说明 |
|------|------|------|
| 认证 | POST /api/auth/register | 用户注册 |
| 认证 | POST /api/auth/login | 用户登录 |
| 项目 | GET /api/project/list | 项目列表 |
| 项目 | POST /api/project/save | 保存项目 |
| 市场 | GET /api/market/apps | 应用列表 |
| 市场 | POST /api/market/publish | 发布应用 |
| AI | POST /magic/ai/generate | 代码生成 |
| AI | POST /magic/ai/configs | AI配置 |
| Git | GET /api/git/commits/{id} | 获取提交历史 |
| Git | POST /api/git/commit | 创建提交 |
| Git | GET /api/git/branches/{id} | 获取分支列表 |
| Git | POST /api/git/branch | 创建分支 |
| Git | POST /api/git/checkout | 检出提交 |
| Git | GET /api/git/remotes/{id} | 获取远程仓库列表 |
| Git | POST /api/git/remote | 添加远程仓库 |
| Git | DELETE /api/git/remote/{id}/{name} | 删除远程仓库 |
| Git | POST /api/git/pull | 拉取远程更新 |
| Git | POST /api/git/push | 推送到远程仓库 |
| 分享 | POST /api/share/record/{appId} | 记录分享 |
| 分享 | GET /api/share/link/{appId} | 获取分享链接 |
| 分享 | GET /api/share/stats/{appId} | 获取分享统计 |
| 通知 | GET /api/notification/list | 获取通知列表 |
| 通知 | PUT /api/notification/read/{id} | 标记通知已读 |
| 通知 | PUT /api/notification/read-all | 全部标记已读 |
| 通知 | GET /api/notification/unread-count | 获取未读数量 |

---

## 数据库

```
magic_sys_user         # 用户
magic_sys_ai_config    # AI配置
magic_sys_project      # 项目
magic_sys_project_commit  # 项目提交记录
magic_sys_project_branch # 项目分支
magic_sys_project_remote  # 远程仓库
magic_sys_market_app   # 市场应用
magic_sys_market_app_version  # 版本历史
magic_sys_market_app_comment   # 评论
magic_sys_market_app_like     # 点赞
magic_sys_market_app_favorite  # 收藏
magic_sys_market_app_share    # 分享记录
magic_sys_notification # 通知消息
magic_sys_user_follow  # 用户关注
```

---

## 版本历史

### v0.18.0 (2026-02) ⭐
用户活动日志、应用评分权重算法、国际化支持

### v0.17.0 (2026-02)
智能搜索增强、智能推荐、模板市场、代码片段库、数据仪表盘

### v0.16.0 (2026-02)
顶部功能区组件化、导航菜单优化、AI配置入口优化

### v0.15.0 (2026-02)
社交分享、消息通知系统、编辑器增强（AI解释代码、代码优化、格式化）、快捷键支持

### v0.14.0 (2026-02)
Git版本管理增强（远程仓库管理、API优化）

### v0.13.0 (2026-02)
Git版本管理功能

### v0.12.0 (2026-02)
用户资料、深色模式与项目导入

### v0.11.0 (2026-02)
应用分类与标签系统

### v0.10.0 (2026-02)
应用详情与社区互动

### v0.9.0 (2026-02)
客户端AI调用重构

### v0.8.0 (2026-01)
用户功能增强（修改密码、用户组件统一）

### v0.7.0 (2026-01)
社区功能（评论、收藏、关注）

### v0.6.0 (2026-01)
版本管理系统（自动版本号、历史记录、回滚）

### v0.5.0 (2026-01)
应用市场改版（窗口管理、Dock栏）

---

## 迭代规划

### v0.19.0（开发中）

| 优先级 | 功能 | 状态 |
|--------|------|------|
| P1 | 项目模板化保存 | ⏳ |
| P2 | 团队协作功能 | ⏳ |
| P3 | 性能优化 | ⏳ |

### v0.20.0 规划

| 优先级 | 功能 |
|--------|------|
| P1 | 移动端适配 |
| P2 | 主题商店 |
| P3 | 更多AI提供商 |

### v1.0.0 里程碑

- **部署服务**: 一键部署应用到云端
- **团队协作**: 多人在线协作开发
- **企业功能**: 团队管理、权限控制、SSO集成
- **插件系统**: 支持第三方插件扩展
- **多语言**: 国际化支持

---

## License

MIT
