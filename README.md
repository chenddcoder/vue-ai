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

## 当前版本: v0.0.3

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
| 多文件支持 | ✅ | 创建、删除、重命名多个文件 |
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
| 自动保存 | ✅ | 发布前自动保存项目 |
| 管理已发布 | ✅ | 在"我的应用"中管理 |
| 下架应用 | ✅ | 从市场下架应用 |

#### 6. 应用市场 ✅

| 功能 | 状态 | 说明 |
|------|------|------|
| 浏览应用 | ✅ | 查看所有已发布应用 |
| 搜索应用 | ✅ | 按名称/描述搜索 |
| 分类筛选 | ✅ | 按标签分类筛选 |
| 应用详情 | ✅ | 查看应用详细信息 |
| 应用预览 | ✅ | 在详情页预览应用 |
| 点赞应用 | ✅ | 点赞/取消点赞 |
| 使用模板 | ✅ | 将应用导入为新项目 |

**API 接口：**
- `GET /api/market/apps` - 获取应用列表
- `GET /api/market/apps/{id}` - 获取应用详情
- `POST /api/market/apps/{id}/like` - 点赞
- `GET /api/market/my-apps` - 获取我发布的应用
- `POST /api/market/publish` - 发布应用
- `DELETE /api/market/apps/{id}` - 下架应用

---

## 当前版本: v0.1.0

### 已完成功能

#### P0 - 核心功能 ✅

| 功能 | 描述 | 复杂度 | 预估工时 | 状态 |
|------|------|--------|----------|------|
| 多文件支持 | 创建、删除、重命名多个文件 | 中 | 3天 | ✅ 已完成 |
| 文件夹管理 | 支持创建文件夹和组织结构 | 中 | 2天 | ✅ 已完成 |
| 项目删除 | 在"我的项目"中删除项目 | 低 | 0.5天 | ✅ 已完成 |
| 项目重命名 | 支持修改项目名称 | 低 | 0.5天 | ✅ 已完成 |

#### P1 - 用户体验优化

| 功能 | 描述 | 复杂度 | 预估工时 |
|------|------|--------|----------|
| 实时保存 | 自动保存，无需手动点击 | 低 | 1天 |
| 用户头像 | 上传和显示头像 | 低 | 1天 |
| 分享功能 | 生成分享链接 | 低 | 1天 |
| 浏览量统计 | 实时更新浏览次数 | 低 | 0.5天 |

#### P2 - 社区功能

| 功能 | 描述 | 复杂度 | 预估工时 |
|------|------|--------|----------|
| 应用评论 | 对应用进行评论 | 中 | 2天 |
| 应用评分 | 对应用进行星级评分 | 低 | 1天 |
| 应用收藏 | 收藏喜欢的应用 | 低 | 1天 |

---

## 后续迭代规划

### v0.2.0 - 编辑器增强

- **模板市场**: 预设应用模板一键使用
- **代码片段**: 保存和复用代码片段
- **版本历史**: 项目版本管理，回滚功能
- **多人协作**: 实时协作编辑

### v0.3.0 - AI能力增强

- **智能补全**: 代码智能补全
- **代码解释**: AI解释代码功能
- **代码优化**: AI优化代码建议
- **单元测试生成**: AI自动生成测试用例

### v1.0.0 - 平台完善

- **社交登录**: 微信/GitHub登录
- **消息通知**: 系统通知和消息
- **用户关注**: 关注其他开发者
- **活动日志**: 用户操作历史记录
- **性能优化**: 首屏加载优化

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
    status INTEGER DEFAULT 1
);

-- 应用点赞表
CREATE TABLE magic_sys_market_app_like (
    app_id INTEGER,
    user_id INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (app_id, user_id)
);
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
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
└── vue-ai-server/           # 后端应用
    └── src/main/java/
        └── com/vueai/server/
            ├── config/      # 配置类
            ├── controller/ # 控制器
            ├── dto/        # 数据传输对象
            ├── model/      # 数据模型
            └── service/    # 服务层
```

---

## 版本历史

- **v0.1.0** - 核心编辑体验完善版本
  - 项目删除功能
  - 项目重命名功能
  - 多文件支持（创建、删除、重命名）
  - 文件夹管理（创建、重命名、删除）
  - 文件树右键菜单优化

- **v0.0.3** - AI配置功能完善版本
  - 支持多种AI提供商配置
  - AI配置管理页面
  - 配置持久化存储

- **v0.0.2** - 编辑器增强版本
  - 文件类型图标显示
  - 项目名称显示

- **v0.0.1** - 初始版本
  - 用户认证
  - 代码编辑器
  - 项目管理
  - 应用发布
  - 应用市场

---

## License

MIT
