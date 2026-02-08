# Vue AI Platform

一个 AI 驱动的应用开发平台，支持可视化编辑、代码生成和应用发布。

## 技术栈

### 前端
- **Vue 3** - 前端框架
- **TypeScript** - 类型安全
- **Pinia** - 状态管理
- **Vue Router** - 路由管理
- **Ant Design Vue 4** - UI 组件库
- **Monaco Editor** - 代码编辑器（VS Code 同款）
- **Vite** - 构建工具
- **Axios** - HTTP 请求

### 后端
- **Spring Boot 2.7** - Java 后端框架
- **SQLite** - 嵌入式数据库
- **JDBC** - 数据库访问
- **Hutool** - Java 工具库

---

## 当前版本: v0.7.0

### 已完成功能

#### 1. 用户认证系统 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户注册 | ✅ | 支持用户名密码注册 |
| 用户登录 | ✅ | 支持登录验证 |
| 游客模式 | ✅ | 无需登录即可体验所有功能 |
| 登录状态持久化 | ✅ | 刷新浏览器保持登录状态 |

**API 接口：**
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/user` - 获取用户信息

#### 2. AI 配置系统 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 多AI提供商支持 | ✅ | OpenAI、Claude、Azure、本地模型等 |
| 配置管理 | ✅ | 添加、编辑、删除、切换AI配置 |
| 配置测试 | ✅ | 测试AI配置是否可用 |
| 高级选项 | ✅ | 温度、最大Token等参数配置 |
| 配置持久化 | ✅ | 配置保存到数据库，支持多用户 |

**支持的AI提供商：**
- **OpenAI**: GPT-4、GPT-4 Turbo、GPT-3.5 Turbo
- **Anthropic Claude**: Claude 3 Opus、Sonnet、Haiku
- **Azure OpenAI**: 企业级OpenAI服务
- **本地模型**: Ollama、LM Studio等
- **自定义API**: 兼容OpenAI格式的任何API

**API 接口：**
- `GET /magic/ai/configs` - 获取用户AI配置列表
- `POST /magic/ai/configs` - 保存AI配置
- `DELETE /magic/ai/configs/{id}` - 删除AI配置
- `PUT /magic/ai/configs/{id}/active` - 设置活动配置
- `POST /magic/ai/configs/test` - 测试AI配置
- `GET /magic/ai/providers` - 获取支持的AI提供商
- `POST /magic/ai/generate` - AI代码生成

#### 3. 代码编辑器 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| Monaco Editor | ✅ | VS Code同款编辑器，语法高亮 |
| 文件管理 | ✅ | 文件树组件，文件类型图标 |
| 实时预览 | ✅ | 右侧实时预览 |
| 保存项目 | ✅ | 项目保存到数据库 |
| AI助手 | ✅ | 自然语言生成代码 |

**组件结构：**
- `MonacoEditor.vue` - 代码编辑器
- `FileTree.vue` - 文件树
- `Preview.vue` - 实时预览
- `AIAssistant.vue` - AI代码生成
- `AIConfigForm.vue` - AI配置表单

#### 4. 项目管理 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 创建项目 | ✅ | 新建项目 |
| 编辑项目 | ✅ | 编辑现有项目代码 |
| 保存项目 | ✅ | 保存项目内容 |
| 项目列表 | ✅ | 查看所有项目 |
| 项目删除 | ✅ | 删除项目 |
| 项目重命名 | ✅ | 重命名项目 |
| 多文件支持 | ✅ | 创建、删除，重命名多个文件 |
| 文件夹管理 | ✅ | 创建文件夹和组织结构 |

**API 接口：**
- `GET /api/project/list` - 获取项目列表
- `GET /api/project/{id}` - 获取项目详情
- `POST /api/project/save` - 保存/创建项目
- `DELETE /api/project/{id}` - 删除项目
- `PUT /api/project/{id}/rename` - 重命名项目

#### 5. 应用发布系统 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 发布到市场 | ✅ | 从编辑器发布到市场 |
| 开源设置 | ✅ | 支持设置开源或闭源（保护代码） |
| 自动保存 | ✅ | 发布前自动保存项目 |
| 管理已发布 | ✅ | 在"我的应用"中管理 |
| 下架应用 | ✅ | 从市场下架应用 |
| 版本更新 | ✅ | 已发布应用自动检测并更新 |

#### 6. 应用市场 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 应用图标网格 | ✅ | 类似Mac启动台的图标展示 |
| 点击启动 | ✅ | 点击图标直接启动应用 |
| 应用窗口管理 | ✅ | 支持多窗口同时运行 |
| 窗口拖拽 | ✅ | 自由拖拽窗口位置 |
| 窗口最小化 | ✅ | 最小化到Dock栏 |
| 窗口最大化/全屏 | ✅ | 全屏显示应用 |
| 窗口关闭 | ✅ | 关闭应用窗口 |
| Mac风格Dock栏 | ✅ | 底部显示运行中的应用 |
| ESC键支持 | ✅ | ESC键最小化当前窗口 |
| 搜索应用 | ✅ | 按名称/描述搜索 |
| 分类筛选 | ✅ | 按标签分类筛选 |

#### 7. 应用版本管理 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 版本号自动管理 | ✅ | Semantic Versioning (语义化版本) |
| 版本更新 | ✅ | 支持 Patch/Minor/Major 更新 |
| 版本历史 | ✅ | 查看完整版本更新记录 |
| 版本回滚 | ✅ | 一键回滚到任意历史版本 |
| 更新说明 | ✅ | 记录每次更新的内容 |

**版本号规则：**
- **Patch** (补丁): `1.0.0` → `1.0.1`（修复bug、小改动）
- **Minor** (次版本): `1.0.0` → `1.1.0`（新功能、向下兼容）
- **Major** (主版本): `1.0.0` → `2.0.0`（重大变更、不兼容）

**API 接口：**
- `POST /api/market/publish` - 发布/更新应用
- `GET /api/market/apps/{id}/versions` - 获取版本历史
- `POST /api/market/apps/{appId}/rollback/{versionId}` - 回滚到指定版本
- `GET /api/market/project/{projectId}/published` - 获取发布状态

**组件结构：**
- `MarketLaunchpad.vue` - 应用市场页面（图标网格 + 窗口）
- `Launchpad.vue` - 全屏启动台组件
- `MyApps.vue` - 我的应用（版本管理、版本历史、回滚）

---

## v0.7.0 更新内容

### 社区功能增强

#### 新增功能
1. **应用评论系统**
   - 用户可以对发布的应用发表评论
   - 支持星级评分（1-5星）
   - 支持回复评论（嵌套评论）
   - 评论统计和平均评分显示

2. **应用收藏**
   - 一键收藏喜欢的应用
   - 独立收藏页面（我的收藏）
   - 收藏状态实时同步
   - 快速取消收藏

3. **用户关注**
   - 关注/取消关注其他开发者
   - 查看用户粉丝列表
   - 查看用户关注列表
   - 个人主页展示关注关系

4. **应用详情页面**
   - 独立应用详情页路由
   - 应用预览窗口
   - 评论区集成
   - 收藏和点赞快捷操作

5. **个人中心**
   - 用户主页（/profile/:userId）
   - 查看用户发布的应用
   - 查看粉丝和关注列表
   - 一键关注功能

#### 新增API接口
- `GET /api/market/apps/{id}/comments` - 获取应用评论
- `POST /api/market/apps/{id}/comments` - 添加评论
- `DELETE /api/market/comments/{id}` - 删除评论
- `POST /api/market/apps/{id}/favorite` - 收藏/取消收藏
- `GET /api/market/favorites` - 获取收藏列表
- `GET /api/market/apps/{id}/favorite/check` - 检查收藏状态
- `POST /api/market/users/{id}/follow` - 关注/取消关注
- `GET /api/market/users/{id}/followers` - 获取粉丝列表
- `GET /api/market/users/{id}/following` - 获取关注列表
- `GET /api/market/users/{id}/follow/check` - 检查关注状态
- `GET /api/market/users/{id}/apps` - 获取用户发布的应用

#### 新增页面和组件
- `MyFavorites.vue` - 我的收藏页面
- `MarketAppDetail.vue` - 应用详情页面
- `UserProfile.vue` - 用户主页
- `AppDetailModal.vue` - 应用详情弹窗

#### 数据库变更
- `magic_sys_market_app_comment` - 应用评论表
- `magic_sys_market_app_favorite` - 应用收藏表
- `magic_sys_user_follow` - 用户关注表

---

## v0.6.0 更新内容（历史版本）

### 应用版本管理系统

#### 新增功能
1. **智能版本检测**
   - 编辑器中发布时自动检测是否已发布
   - 已发布应用显示"更新"按钮
   - 未发布应用显示"发布"按钮
   - 项目列表显示当前版本号

2. **版本更新界面**
   - 显示当前版本号
   - 三种版本更新类型选择
   - 更新内容说明输入
   - 版本号自动计算预览

3. **版本历史管理**
   - 时间线形式展示所有版本
   - 当前版本高亮显示
   - 显示版本更新说明
   - 一键回滚到历史版本

4. **数据库迁移**
   - 自动添加版本字段（version, version_code, update_content）
   - 自动创建版本历史表
   - 增量升级，保留历史数据

#### 界面改进
- 我的应用页面增加版本号标签
- 项目列表显示发布状态和版本
- 发布弹窗智能切换发布/更新模式
- 版本历史时间线组件

---

## v0.5.0 更新内容（历史版本）

### 应用市场全新改版

#### 新增功能
1. **启动台模式**
   - 全屏应用启动台界面
   - 搜索和分类筛选
   - 毛玻璃背景效果

2. **窗口管理系统**
   - 独立窗口运行多个应用
   - 窗口拖拽移动
   - 窗口最小化/最大化/关闭
   - 窗口层级管理（点击置顶）

3. **Mac风格Dock栏**
   - 底部停靠栏显示运行中的应用
   - 悬停放大效果
   - 活动窗口指示点
   - 一键关闭所有应用

---

## 下一步迭代规划 (v0.8.0)

### 1. 应用详情增强 (P1)
- [ ] **应用截图**: 支持上传多张截图展示
- [ ] **应用视频**: 支持上传演示视频
- [ ] **使用说明**: 详细的使用文档和帮助
- [ ] **版本对比**: 查看不同版本之间的差异

### 2. 用户体验优化 (P2)
- [ ] **消息通知**: 系统通知和消息中心
- [ ] **操作日志**: 用户操作历史记录
- [ ] **首屏优化**: 加载性能优化
- [ ] **深色模式**: 完整的深色主题支持

### 3. 编辑器增强 (P2)
- [ ] **代码补全**: 编辑器内智能代码补全
- [ ] **代码解释**: 选中代码进行AI解释
- [ ] **代码优化**: AI提供代码优化建议
- [ ] **错误检测**: 实时语法错误检测

### 4. 分享功能 (P2)
- [ ] **生成海报**: 生成应用分享海报
- [ ] **社交分享**: 一键分享到社交平台
- [ ] **嵌入代码**: 生成可嵌入网页的代码

---

## v1.0.0 规划

### 高级功能
- [ ] **部署服务**: 一键部署应用到生产环境
- [ ] **团队协作**: 多人协作编辑项目
- [ ] **模板市场**: 官方和社区模板
- [ ] **API市场**: 第三方API集成
- [ ] **数据分析**: 应用使用数据分析

### 国际化
- [ ] **多语言支持**: 英文、日文等语言支持
- [ ] **本地化**: 适应不同地区用户

### 性能与安全
- [ ] **性能优化**: 大项目加载优化
- [ ] **安全加固**: 增强安全性和权限控制
- [ ] **CDN加速**: 静态资源加速

---

## 快速开始

### 前端启动

```bash
cd vue-ai-platform
pnpm install
pnpm dev
```

### 后端启动

```bash
cd vue-ai-server
mvn spring-boot:run
```

### 访问地址

- 前端：http://localhost:5173
- 后端 API：http://localhost:8081

---

## 数据库表结构

```sql
-- 用户表
CREATE TABLE magic_sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT DEFAULT 'user',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI配置表
CREATE TABLE magic_sys_ai_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    provider TEXT NOT NULL,
    api_key TEXT,
    base_url TEXT,
    model TEXT,
    temperature REAL DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 2000,
    is_active INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 项目表
CREATE TABLE magic_sys_project (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    owner_id INTEGER,
    content TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 市场应用表
CREATE TABLE magic_sys_market_app (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER,
    name TEXT NOT NULL,
    description TEXT,
    tags TEXT,
    thumbnail TEXT,
    content TEXT,
    author_id INTEGER,
    author_name TEXT,
    author_avatar TEXT,
    likes INTEGER DEFAULT 0,
    views INTEGER DEFAULT 0,
    publish_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status INTEGER DEFAULT 1,
    is_open_source INTEGER DEFAULT 1,
    version VARCHAR(20) DEFAULT '1.0.0',
    version_code INTEGER DEFAULT 1,
    update_content TEXT
);

-- 应用点赞表
CREATE TABLE magic_sys_market_app_like (
    app_id INTEGER,
    user_id INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (app_id, user_id)
);

-- 应用收藏表
CREATE TABLE magic_sys_market_app_favorite (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(app_id, user_id)
);

-- 应用评论表
CREATE TABLE magic_sys_market_app_comment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    user_name VARCHAR(100),
    user_avatar TEXT,
    content TEXT NOT NULL,
    rating INTEGER DEFAULT 5,
    parent_id INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户关注表
CREATE TABLE magic_sys_user_follow (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    follower_id INTEGER NOT NULL,
    followee_id INTEGER NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(follower_id, followee_id)
);

-- 应用版本历史表
CREATE TABLE magic_sys_market_app_version (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id INTEGER NOT NULL,
    version VARCHAR(20) NOT NULL,
    version_code INTEGER NOT NULL,
    content TEXT NOT NULL,
    description TEXT,
    author_id INTEGER,
    author_name TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (app_id) REFERENCES magic_sys_market_app(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_app_version ON magic_sys_market_app_version(app_id);
```

---

## 项目结构

```
vue-ai/
├── vue-ai-platform/        # 前端应用
│   ├── src/
│   │   ├── api/            # API接口定义
│   │   ├── components/     # 公共组件
│   │   │   ├── editor/     # 编辑器组件
│   │   │   └── preview/    # 预览组件
│   │   ├── router/         # 路由配置
│   │   ├── stores/         # Pinia状态管理
│   │   ├── utils/          # 工具函数
│   │   ├── views/          # 页面组件
│   │   │   ├── EditorLayout.vue
│   │   │   ├── MarketLaunchpad.vue
│   │   │   ├── Launchpad.vue
│   │   │   ├── MyApps.vue
│   │   │   ├── MyFavorites.vue
│   │   │   ├── MarketAppDetail.vue
│   │   │   ├── UserProfile.vue
│   │   │   ├── AIConfig.vue
│   │   │   └── Login.vue
│   │   ├── components/     # 公共组件
│   │   │   ├── editor/     # 编辑器组件
│   │   │   ├── preview/    # 预览组件
│   │   │   └── community/  # 社区组件
│   │   │       └── AppDetailModal.vue
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
└── vue-ai-server/           # 后端应用
    └── src/main/java/
        └── com.vueai/server/
            ├── config/        # 配置类
            │   └── DatabaseMigration.java  # 数据库迁移
            ├── controller/   # 控制器
            │   └── MarketController.java  # 市场API（含版本管理）
            ├── dto/         # 数据传输对象
            ├── model/       # 数据模型
            └── service/     # 服务层
```

---

## 版本历史

### v0.7.0 - 社区功能增强
**重大更新**
- ✅ 应用评论系统（支持星级评分和回复）
- ✅ 应用收藏功能
- ✅ 用户关注系统
- ✅ 独立应用详情页面
- ✅ 用户个人主页
- ✅ API接口和数据库表

### v0.6.0 - 应用版本管理系统
**重大更新**
- ✅ 应用版本号自动管理（Semantic Versioning）
- ✅ 版本更新（Patch/Minor/Major）
- ✅ 版本历史记录和查看
- ✅ 一键回滚到历史版本
- ✅ 编辑器智能检测发布状态
- ✅ 我的应用页面版本管理
- ✅ 数据库自动迁移

### v0.5.0 - 应用市场全新改版
- ✅ 全新应用市场界面，类似 macOS 启动台
- ✅ 应用图标网格展示，点击即可启动
- ✅ 完整的窗口管理系统（拖拽、最小化、最大化、关闭）
- ✅ Mac 风格 Dock 栏，支持窗口快速切换
- ✅ ESC 键快捷操作
- ✅ 改进的组件加载逻辑和错误处理

### v0.4.0 - 用户体验优化版本
- 新增用户头像上传功能（支持URL自定义头像）
- 实现实时自动保存（3秒无操作自动保存）
- 添加应用分享功能（一键复制分享链接）
- 浏览量统计（实时更新应用浏览次数）

### v0.3.0 - 智能代码生成与项目导出版本
- 新增保存项目弹窗并优化保存逻辑
- 添加项目导出功能（支持ZIP下载）
- 实现智能模块代码生成功能
- 为应用市场添加开源/闭源设置功能
- 添加通义千问AI支持
- 添加AI配置管理系统（多提供商支持）
- 完善项目管理和多文件编辑功能

### v0.2.0 - AI能力增强版本
- 用户认证系统（注册、登录、游客模式）
- AI配置系统（多AI提供商支持）
- 代码编辑器（Monaco Editor集成）
- 项目管理（多文件、文件夹管理）
- 应用发布系统
- 应用市场（浏览、搜索、使用模板）

### v0.1.0 - 核心编辑体验完善版本
- 项目删除功能
- 项目重命名功能
- 多文件支持（创建、删除、重命名）
- 文件夹管理（创建、重命名、删除）
- 文件树右键菜单优化

---

## License

MIT
