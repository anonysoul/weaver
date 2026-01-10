import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { RepoApiService, RepoRequest, RepoResponse } from './core/openapi';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.less'
})
export class App {
  protected repos: RepoResponse[] = [];
  protected loading = false;
  protected status: string | null = null;
  protected editingId: number | null = null;
  protected showForm = false;
  protected form = {
    name: '',
    url: '',
    defaultBranch: 'main',
    username: '',
    token: ''
  };

  constructor(private readonly repoApi: RepoApiService) {
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.repoApi.listRepos().subscribe({
      next: (repos) => {
        this.repos = repos;
        this.loading = false;
      },
      error: () => {
        this.status = '加载仓库列表失败';
        this.loading = false;
      }
    });
  }

  startCreate(): void {
    this.editingId = null;
    this.showForm = true;
    this.form = {
      name: '',
      url: '',
      defaultBranch: 'main',
      username: '',
      token: ''
    };
    this.status = null;
  }

  startEdit(repo: RepoResponse): void {
    this.editingId = repo.id;
    this.showForm = true;
    this.form = {
      name: repo.name,
      url: repo.url,
      defaultBranch: repo.defaultBranch,
      username: '',
      token: ''
    };
    this.status = null;
  }

  save(): void {
    const formValue = this.form;
    const payload: RepoRequest = {
      name: formValue.name.trim(),
      url: formValue.url.trim(),
      defaultBranch: formValue.defaultBranch.trim(),
      credential: formValue.token.trim()
        ? {
            type: 'HTTPS_PAT',
            username: formValue.username.trim() || undefined,
            token: formValue.token.trim()
          }
        : undefined
    };

    if (!payload.name || !payload.url || !payload.defaultBranch) {
      this.status = '请填写完整的仓库信息';
      return;
    }

    const editingId = this.editingId;
    const request$ = editingId
      ? this.repoApi.updateRepo(editingId, payload)
      : this.repoApi.createRepo(payload);

    request$.subscribe({
      next: () => {
        this.status = editingId ? '仓库已更新' : '仓库已创建';
        this.refresh();
        if (!editingId) {
          this.form = {
            name: '',
            url: '',
            defaultBranch: 'main',
            username: '',
            token: ''
          };
          this.showForm = false;
          this.status = null;
        }
      },
      error: (err) => {
        const message = err?.error?.error || '保存失败，请检查仓库信息';
        this.status = message;
      }
    });
  }

  remove(repo: RepoResponse): void {
    this.repoApi.deleteRepo(repo.id).subscribe({
      next: () => {
        this.status = '仓库已删除';
        this.refresh();
      },
      error: () => {
        this.status = '删除失败，请稍后重试';
      }
    });
  }

  test(repo: RepoResponse): void {
    this.repoApi.testRepoConnection(repo.id).subscribe({
      next: (result) => {
        this.status = result.ok ? `连接成功：${result.message}` : `连接失败：${result.message}`;
      },
      error: (err) => {
        const message = err?.error?.error || '连接测试失败';
        this.status = message;
      }
    });
  }
}
