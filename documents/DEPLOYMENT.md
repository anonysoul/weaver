# Weaver 运行与部署说明

## Docker 运行

示例：

```bash
docker run --rm -p 8080:8080 \
  -e WEAVER_DB_URL=jdbc:sqlite:/data/weaver.db \
  -e WEAVER_CONTAINER_IMAGE=weaver-codex \
  -e WEAVER_CONTAINER_DATA_VOLUME=weaver-data \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v weaver-data:/data \
  weaver:latest
```

说明：

- `WEAVER_CONTAINER_DATA_VOLUME` 会把名为 `weaver-data` 的 volume 挂载到会话容器内的 `/data`。
- SQLite 建议使用 `/data/weaver.db`，保证容器重启后数据可恢复。
- 使用 Docker in Docker 时，需要挂载宿主机 `docker.sock`。

## 会话容器清理

- 会话删除后会移除对应容器。
- 定时清理会移除已失效会话的容器，并清理过期日志文件。
