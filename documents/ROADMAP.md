# Weaver 项目路线图

## 1. 项目目标

Weaver 是一个面向 AI 编码场景的 Git 仓库与会话管理平台，目标是：

- 以 **Docker 镜像** 形式交付和运行
- 启动容器后即可通过 **Web 浏览器访问**
- 在 Web UI 中配置和管理多个 **代码托管平台连接** 与其下的仓库
- 支持为同一个 Git 远程仓库创建 **多个独立的 Codex / AI 编码会话**
- 每个会话拥有独立的 workspace，用于 AI 编码与上下文隔离
- 仅提供 **仓库与会话环境管理**，不包含模型调用或 AI 代理执行逻辑

技术栈：
- 后端：Kotlin（Spring Boot）
- 前端：Angular
- 数据库：SQLite
- 接口与规范：OpenAPI（后端提供 spec，前端基于 spec 生成类型与客户端）

---

## 2. 核心概念（Domain Model）

### 2.1 SCM Provider
- 代码托管平台的配置实体（GitLab / GitHub / Azure DevOps）
- 通过平台授权或访问令牌建立连接

### 2.2 Repository
- 平台下的远程 Git 仓库
- 由平台连接自动拉取或选择

### 2.3 Session（Codex Session）
- 一次 AI 编码会话
- 仅绑定一个 Git 仓库
- 每个 Session 拥有独立的 Workspace

### 2.4 Workspace
- Session 对应的本地工作目录
- 用于 clone / checkout Git 仓库
- 容器内路径示例：`/data/weaver/workspaces/{sessionId}`

# 项目里程碑（Milestones）

## Milestone 0：项目骨架（可运行基础）

### 目标
- 项目可以以 **单一 Docker 镜像** 的形式构建和运行
- 启动容器后，可通过浏览器访问 Web 页面
- 前后端基础通信正常

### 验收标准
- `docker build` 成功
- `docker run` 后访问 `http://localhost:8080` 有响应
- 基础日志、配置加载正常

### 主要任务
- 技术栈落地（Kotlin / Angular / SQLite）
- 初始化项目目录结构
- 编写 Dockerfile（单镜像）
- 基础 Web 页面与 API 服务
- 基础配置与环境变量管理


---

## Milestone 1：代码托管平台对接（Provider 管理）

### 目标
- 在 Web 上配置和管理 GitLab 平台连接
- 支持 GitLab 的基础接入
- 可拉取平台下的仓库列表并验证连接

### 验收标准
- Web UI 支持平台连接的新增 / 编辑 / 删除
- 至少支持一种平台授权方式（推荐 OAuth 或 PAT）
- 后端可调用平台 API 拉取仓库列表并验证连接
- 认证信息加密存储，不在接口和日志中明文暴露

### 主要任务
- Provider 数据模型设计
- Provider CRUD API
- GitLab API 封装（仓库列表、连接校验）
- 认证信息加密与解密逻辑
- GitLab 连接与仓库选择 UI 页面

---

## Milestone 2：Codex 会话管理（Session + Workspace）

### 目标
- 支持创建多个 Codex 会话
- 会话可选择一个 Git 仓库作为上下文
- 同一 Git 仓库可被多个会话同时使用，彼此隔离

### 验收标准
- Web UI 可创建 / 删除会话
- 创建会话时可选择一个仓库
- 后端为每个会话创建独立 workspace 目录
- 选定仓库会被 clone 到会话 workspace 中
- 会话状态可见（creating / ready / failed）

### 主要任务
- Session 数据模型
- Session CRUD API
- Workspace 目录结构设计
- 会话初始化流程（clone 仓库）
- 初始化过程日志记录与状态更新


---

## Milestone 3：会话运行能力（最小可用）

### 目标
- 会话具备最小可操作能力，支撑 AI 编码使用
- 可查看会话执行日志
- 可导出会话上下文信息供 AI 使用

### 验收标准
- Web 页面可查看会话日志
- 提供受控 Git 操作（如 pull / checkout / status）
- 可生成会话上下文摘要（仓库列表、分支、目录结构等）
- 会话失败时可返回明确错误信息

### 主要任务
- 会话日志存储与查询接口
- Git 操作 API（白名单命令）
- 会话上下文导出接口（JSON）
- 会话详情 UI 页面


---

## Milestone 4：并发、隔离与稳定性增强

### 目标
- 提升系统在多会话并发下的稳定性和安全性
- 完善资源管理与清理机制
- 为后续 AI 深度集成打好基础

### 验收标准
- 多会话并行初始化互不影响
- 会话删除后 workspace 可被正确清理
- 凭据不泄露，日志与接口返回均脱敏
- 支持通过 volume 持久化 `/data` 目录

### 主要任务
- 会话初始化与 Git 操作加锁
- 并发与任务队列控制
- Workspace 回收与 GC 策略
- 基础安全加固（权限、限流、校验）
- Docker 运行与部署文档完善

---

## Milestone 5：多平台接入扩展（GitHub / Azure DevOps）

### 目标
- 在现有 GitLab 接入基础上扩展更多平台
- 支持 GitHub / Azure DevOps 的平台连接
- 统一平台抽象与仓库选择体验

### 验收标准
- Web UI 支持多平台连接的新增 / 编辑 / 删除
- 支持 GitHub / Azure DevOps 的认证与仓库列表拉取
- 不同平台的仓库可被统一绑定到会话

### 主要任务
- Provider 抽象层扩展与适配
- GitHub API 封装与认证流程
- Azure DevOps API 封装与认证流程
- 多平台连接与仓库选择 UI 优化
