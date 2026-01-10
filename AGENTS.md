# Repository Guidelines

## 项目结构与模块组织
- `backend/`: Kotlin + Spring Boot 服务（Gradle）。主代码在 `backend/src/main/kotlin`，资源在 `backend/src/main/resources`。
- `frontend/`: Angular 应用（TypeScript + Less）。应用代码在 `frontend/src/app`，全局样式在 `frontend/src/styles.less`。
- `documents/`: 项目文档与路线图（见 `documents/ROADMAP.md`）。
- OpenAPI 规范位于 `backend/src/main/resources/openapi/openapi.yaml`，生成代码路径：
  - 后端：`backend/build/generated/openapi`
  - 前端：`frontend/src/app/core/openapi`

## 构建、测试与开发命令
后端（在 `backend/` 目录执行）：
- `./gradlew bootRun`: 启动 API 服务。
- `./gradlew test`: 运行后端测试（JUnit Platform）。
- `./gradlew openApiGenerate`: 重新生成后端 OpenAPI 接口代码。

前端（在 `frontend/` 目录执行）：
- `npm run start`: 启动开发服务器，地址 `http://localhost:8765`（会同步生成 OpenAPI 客户端）。
- `npm run build`: 生产构建。
- `npm run test`: 单元测试（`ng test`，Vitest）。
- `npm run openapi-generate`: 仅重新生成 Angular OpenAPI 客户端。

## 编码风格与命名约定
- Kotlin：4 空格缩进，类用 `PascalCase`，函数/变量用 `camelCase`，文件路径与包名对齐。
- TypeScript/Angular：遵循 Angular CLI 默认规范，生成文件使用 `kebab-case`，组件类使用 `PascalCase`。
- 格式化：前端使用 `frontend/package.json` 中的 Prettier（单引号，行宽 100）。

## 测试指南
- 后端：Spring Boot + JUnit；测试放在 `backend/src/test/kotlin`，命名为 `*Test.kt`。
- 前端：Vitest via `ng test`；测试与组件同目录，命名 `*.spec.ts`（如 `app.spec.ts`）。
- 当前未设置覆盖率阈值，新增功能应配套测试。

## 提交与合并请求规范
- 提交信息沿用 `type: message` 简洁格式（如 `init: ...`、`doc: ...`），type 使用小写。
- PR 需包含：简要说明、关联问题（如有）、测试说明（命令或备注）、UI 变更截图。

## 安全与配置提示
- 后端本地使用 SQLite（`backend/weaver.db`），避免提交敏感数据或本地数据库文件。
- 修改 `openapi.yaml` 后需重新生成前后端代码。
