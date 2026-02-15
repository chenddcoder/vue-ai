# Vue AI Platform

一个 AI 驱动的应用开发平台，支持可视化编辑、代码生成和应用发布。

## 技术栈

| 类别 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Ant Design Vue 4 + Pinia + Monaco Editor |
| 后端 | Spring Boot 2.7 + SQLite + Hutool |
| 包管理 | pnpm |

---

## 当前版本: v0.12.0

### 核心功能

| 模块 | 功能 |
|------|------|
| 用户系统 | 注册、登录、游客模式、个人资料管理 |
| AI配置 | OpenAI/Claude/智谱/通义多提供商支持 |
| 代码编辑 | Monaco Editor + 文件管理 + 实时预览 |
| 项目管理 | 多文件、文件夹、项目CRUD、导入导出ZIP |
| AI助手 | 自然语言生成代码 |
| 应用市场 | 应用发布、浏览、安装、窗口管理、分类标签 |
| 社区功能 | 收藏、评论、关注、用户主页 |
| 版本管理 | 版本号自动管理、历史记录、回滚 |
| 应用详情 | 实时预览、评分评论、一键启动 |
| 主题系统 | 深色模式、浅色模式、跟随系统 |

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

---

## 数据库

```
magic_sys_user         # 用户
magic_sys_ai_config    # AI配置
magic_sys_project      # 项目
magic_sys_market_app   # 市场应用
magic_sys_market_app_version  # 版本历史
magic_sys_market_app_comment   # 评论
magic_sys_market_app_like     # 点赞
magic_sys_market_app_favorite  # 收藏
magic_sys_user_follow  # 用户关注
```

---

## 版本历史

### v0.12.0 (2026-02) ⭐
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

### v0.13.0（开发中）

| 优先级 | 功能 | 状态 |
|--------|------|------|
| P1 | 社交分享（链接分享、嵌入代码） | 🔄 |
| P2 | 消息通知系统（评论回复、获赞提醒） | ⏳ |
| P2 | 编辑器增强（代码补全、AI解释代码、代码格式化） | ⏳ |
| P3 | 快捷键支持（编辑器快捷键、自定义快捷键） | ⏳ |

### v0.14.0 规划

| 优先级 | 功能 |
|--------|------|
| P1 | 应用搜索增强（全文搜索、智能推荐） |
| P2 | 模板市场（官方模板、用户模板） |
| P2 | 代码片段库（保存复用、分类管理） |
| P3 | 仪表盘（数据可视化、项目统计） |

### v1.0.0 里程碑

- **部署服务**: 一键部署应用到云端
- **团队协作**: 多人在线协作开发
- **企业功能**: 团队管理、权限控制、SSO集成
- **插件系统**: 支持第三方插件扩展
- **多语言**: 国际化支持

---

## License

MIT
