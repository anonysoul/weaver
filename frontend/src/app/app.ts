import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { GitLabRepoResponse, ProviderApiService, ProviderRequest, ProviderResponse } from './core/openapi';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.less'
})
export class App {
  protected providers: ProviderResponse[] = [];
  /**
   * 按连接 ID 缓存 GitLab 仓库列表
   */
  protected repos: Record<number, GitLabRepoResponse[]> = {};
  protected loadingProviders = false;
  /**
   * 按连接 ID 记录仓库列表加载状态
   */
  protected loadingRepos: Record<number, boolean> = {};
  protected status: string | null = null;
  protected editingId: number | null = null;
  protected showForm = false;
  protected form = {
    name: '',
    baseUrl: 'https://gitlab.com',
    token: ''
  };

  constructor(private readonly providerApi: ProviderApiService) {
    this.refresh();
  }

  refresh(): void {
    this.loadingProviders = true;
    this.providerApi.listProviders().subscribe({
      next: (providers) => {
        this.providers = providers;
        this.loadingProviders = false;
      },
      error: () => {
        this.status = '加载 GitLab 连接列表失败';
        this.loadingProviders = false;
      }
    });
  }

  startCreate(): void {
    this.editingId = null;
    this.showForm = true;
    this.form = {
      name: '',
      baseUrl: 'https://gitlab.com',
      token: ''
    };
    this.status = null;
  }

  startEdit(provider: ProviderResponse): void {
    this.editingId = provider.id;
    this.showForm = true;
    this.form = {
      name: provider.name,
      baseUrl: provider.baseUrl,
      token: ''
    };
    this.status = null;
  }

  save(): void {
    const formValue = this.form;
    const payload: ProviderRequest = {
      name: formValue.name.trim(),
      baseUrl: formValue.baseUrl.trim(),
      token: formValue.token.trim()
    };

    if (!payload.name || !payload.baseUrl || !payload.token) {
      this.status = '请填写完整的 GitLab 连接信息';
      return;
    }

    const editingId = this.editingId;
    const request$ = editingId
      ? this.providerApi.updateProvider(editingId, payload)
      : this.providerApi.createProvider(payload);

    request$.subscribe({
      next: () => {
        this.status = editingId ? 'GitLab 连接已更新' : 'GitLab 连接已创建';
        this.refresh();
        if (!editingId) {
          this.form = {
            name: '',
            baseUrl: 'https://gitlab.com',
            token: ''
          };
          this.showForm = false;
          this.status = null;
        }
      },
      error: (err) => {
        const message = err?.error?.error || '保存失败，请检查 GitLab 连接信息';
        this.status = message;
      }
    });
  }

  remove(provider: ProviderResponse): void {
    this.providerApi.deleteProvider(provider.id).subscribe({
      next: () => {
        this.status = 'GitLab 连接已删除';
        this.refresh();
      },
      error: () => {
        this.status = '删除失败，请稍后重试';
      }
    });
  }

  test(provider: ProviderResponse): void {
    this.providerApi.testProviderConnection(provider.id).subscribe({
      next: (result) => {
        this.status = result.ok ? `连接成功：${result.message}` : `连接失败：${result.message}`;
      },
      error: (err) => {
        const message = err?.error?.error || '连接测试失败';
        this.status = message;
      }
    });
  }

  loadRepos(provider: ProviderResponse): void {
    /**
     * 拉取单个 GitLab 连接的仓库清单
     */
    this.loadingRepos[provider.id] = true;
    this.providerApi.listProviderRepos(provider.id).subscribe({
      next: (repos) => {
        this.repos[provider.id] = repos;
        this.loadingRepos[provider.id] = false;
      },
      error: (err) => {
        const message = err?.error?.error || '加载仓库失败';
        this.status = message;
        this.loadingRepos[provider.id] = false;
      }
    });
  }
}
