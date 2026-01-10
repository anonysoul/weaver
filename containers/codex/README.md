# Codex 容器镜像

该目录用于构建 Codex 适配镜像，内置 `git`、`curl`、`zsh` 与 `nvm`。

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

## 说明

- `NODE_VERSION` 既可在构建时设置（`--build-arg`），也可在运行时设置（`-e`）。
- 入口脚本会在容器启动时安装并激活指定版本的 Node。
