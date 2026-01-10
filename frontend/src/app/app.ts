import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ViewEncapsulation
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  ConnectionTestResponse,
  ProvidersService,
  GitLabRepoResponse,
  ProviderRequest,
  ProviderResponse,
  SessionRequest,
  SessionResponse,
  SessionsService
} from './core/openapi';
import { ProviderPanelComponent } from './components/provider-panel/provider-panel.component';
import { RepoCatalogComponent } from './components/repo-catalog/repo-catalog.component';
import { SessionPanelComponent } from './components/session-panel/session-panel.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { TopbarComponent } from './components/topbar/topbar.component';
import { WorkspacePanelComponent } from './components/workspace-panel/workspace-panel.component';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    FormsModule,
    ProviderPanelComponent,
    RepoCatalogComponent,
    SessionPanelComponent,
    SidebarComponent,
    TopbarComponent,
    WorkspacePanelComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.less',
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
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
  protected sessions: SessionResponse[] = [];
  protected loadingSessions = false;
  protected creatingSession = false;
  protected form = {
    name: '',
    baseUrl: 'https://gitlab.com',
    token: ''
  };

  constructor(
    private readonly providerApi: ProvidersService,
    private readonly sessionApi: SessionsService,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.refresh();
    this.refreshSessions();
  }

  get repoCount(): number {
    return Object.keys(this.repos).length;
  }

  get totalRepoCount(): number {
    return Object.values(this.repos).reduce((sum, repos) => sum + repos.length, 0);
  }

  refresh(): void {
    this.loadingProviders = true;
    this.providerApi.listProviders().subscribe({
      next: (providers: ProviderResponse[]) => {
        this.providers = providers;
        this.loadingProviders = false;
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        this.status = '加载 GitLab 连接列表失败';
        this.loadingProviders = false;
        this.cdr.markForCheck();
      }
    });
  }

  refreshSessions(): void {
    this.loadingSessions = true;
    this.sessionApi.listSessions().subscribe({
      next: (sessions: SessionResponse[]) => {
        this.sessions = [...sessions].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.loadingSessions = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.status = '加载 Session 列表失败';
        this.loadingSessions = false;
        this.cdr.markForCheck();
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
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        const message = err?.error?.error || '保存失败，请检查 GitLab 连接信息';
        this.status = message;
        this.cdr.markForCheck();
      }
    });
  }

  remove(provider: ProviderResponse): void {
    this.providerApi.deleteProvider(provider.id).subscribe({
      next: () => {
        this.status = 'GitLab 连接已删除';
        this.refresh();
        this.cdr.markForCheck();
      },
      error: () => {
        this.status = '删除失败，请稍后重试';
        this.cdr.markForCheck();
      }
    });
  }

  test(provider: ProviderResponse): void {
    this.providerApi.testProviderConnection(provider.id).subscribe({
      next: (result: ConnectionTestResponse) => {
        this.status = result.ok ? `连接成功：${result.message}` : `连接失败：${result.message}`;
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        const message = err?.error?.error || '连接测试失败';
        this.status = message;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * 拉取单个 GitLab 连接的仓库清单
   */
  loadRepos(provider: ProviderResponse): void {
    this.loadingRepos = {
      ...this.loadingRepos,
      [provider.id]: true
    };
    this.cdr.markForCheck();
    this.providerApi.listProviderRepos(provider.id).subscribe({
      next: (repos: GitLabRepoResponse[]) => {
        this.repos = {
          ...this.repos,
          [provider.id]: repos
        };
        this.loadingRepos = {
          ...this.loadingRepos,
          [provider.id]: false
        };
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        const message = err?.error?.error || '加载仓库失败';
        this.status = message;
        this.loadingRepos = {
          ...this.loadingRepos,
          [provider.id]: false
        };
        this.cdr.markForCheck();
      }
    });
  }

  createSession(request: SessionRequest): void {
    if (this.creatingSession) {
      return;
    }
    this.creatingSession = true;
    this.sessionApi.createSession(request).subscribe({
      next: () => {
        this.status = 'Session 初始化中';
        this.creatingSession = false;
        this.refreshSessions();
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        const message = err?.error?.error || '创建 Session 失败';
        this.status = message;
        this.creatingSession = false;
        this.cdr.markForCheck();
      }
    });
  }

  deleteSession(session: SessionResponse): void {
    this.sessionApi.deleteSession(session.id).subscribe({
      next: () => {
        this.status = 'Session 已删除';
        this.refreshSessions();
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        const message = err?.error?.error || '删除 Session 失败';
        this.status = message;
        this.cdr.markForCheck();
      }
    });
  }
}
