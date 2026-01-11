# Weaver

Weaver 是一个面向 AI 编码场景的 Git 仓库与会话管理平台，提供仓库配置、会话隔离与工作空间管理能力。

Codex 登录支持 `codex login --device-auth`，并且需要在网页上（https://auth.openai.com/codex/device）开启“为 Codex 启用设备代码授权”。

## 镜像交付

- Weaver 镜像：`anonysoul/weaver:latest`
- Codex 镜像：`anonysoul/weaver-workspace-codex:latest`

构建：

```bash
docker build -t anonysoul/weaver:latest .
docker build -t anonysoul/weaver-workspace-codex:latest containers/codex
```

运行与部署说明见 `documents/DEPLOYMENT.md`。

## 路线图

详见 `documents/ROADMAP.md`。
