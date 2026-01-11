# Codex 容器镜像

该目录用于构建 Codex 适配镜像，内置 `git`、`curl`、`zsh`、`nvm`、`code-server`（VSCode Web）与 `@openai/codex`。

## 构建

```bash
docker build -t weaver-codex containers/codex
```

指定 Node 版本：

```bash
docker build --build-arg NODE_VERSION=18 -t weaver-codex containers/codex
```

## 运行

```bash
docker run -it weaver-codex zsh
```

运行时覆盖 Node 版本（容器启动时自动安装并切换）：

```bash
docker run -e NODE_VERSION=22 -it weaver-codex zsh
```

暴露 VSCode Web 端口（容器内默认 8080，可按需映射宿主端口）：

```bash
docker run -p 0:8080 -it weaver-codex zsh
```

指定固定宿主端口：

```bash
docker run -p 62000:8080 -it weaver-codex zsh
```

## 说明

- `NODE_VERSION` 既可在构建时设置（`--build-arg`），也可在运行时设置（`-e`）。
- 未设置 `NODE_VERSION` 时默认使用 `nvm install --lts` 安装并激活最新版 LTS。
- 入口脚本会在容器启动时安装并激活指定版本的 Node，同时确保全局安装 `codex`。
- `code-server` 作为 VSCode Web 运行在容器内 `8080` 端口，Weaver 会在创建会话时自动映射宿主端口并启动服务。
