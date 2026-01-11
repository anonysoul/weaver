import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  Output,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { TextareaModule } from 'primeng/textarea';
import { ConfirmationService, MessageService } from 'primeng/api';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { SessionsService } from '../../core/openapi/api/sessions.service';
import { SessionResponse } from '../../core/openapi/model/session-response';
import { SessionRequest } from '../../core/openapi/model/session-request';
import { SessionStatus } from '../../core/openapi/model/session-status';
import { SessionLogResponse } from '../../core/openapi/model/session-log-response';
import { SessionContextResponse } from '../../core/openapi/model/session-context-response';
import { ProviderResponse } from '../../core/openapi/model/provider-response';
import { ProviderType } from '../../core/openapi/model/provider-type';
import { GitLabRepoResponse } from '../../core/openapi/model/git-lab-repo-response';
import { BASE_PATH } from '../../core/openapi/variables';
import { ReposPanelComponent } from '../repos-panel/repos-panel.component';

@Component({
  selector: 'app-sessions-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    CardModule,
    DialogModule,
    TableModule,
    SelectModule,
    TagModule,
    ToolbarModule,
    TextareaModule,
    ReposPanelComponent,
  ],
  templateUrl: './sessions-panel.component.html',
  styleUrl: './sessions-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionsPanelComponent {
  @Input() providers: ProviderResponse[] = [];
  @Output() sessionsUpdated = new EventEmitter<SessionResponse[]>();

  sessions = signal<SessionResponse[]>([]);
  loading = signal(false);
  startingSessionId = signal<number | null>(null);
  logs = signal<SessionLogResponse[]>([]);
  contextPayload = signal('');

  logsVisible = signal(false);
  contextVisible = signal(false);
  createVisible = signal(false);

  selectedProvider: ProviderResponse | null = null;
  selectedRepo: GitLabRepoResponse | null = null;

  private readonly sessionsService = inject(SessionsService);
  private readonly httpClient = inject(HttpClient);
  private readonly basePath = inject(BASE_PATH);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly SessionStatus = SessionStatus;

  constructor() {
    this.loadSessions();
  }

  loadSessions(): void {
    this.loading.set(true);
    this.sessionsService
      .listSessions()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (sessions) => {
          this.sessions.set(sessions);
          this.sessionsUpdated.emit(sessions);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '加载失败',
            detail: '无法获取会话列表。',
          });
        },
      });
  }

  openCreateDialog(): void {
    if (!this.selectedProvider && this.providers.length > 0) {
      this.selectedProvider = this.providers[0];
    }
    this.createVisible.set(true);
  }

  createSession(): void {
    if (!this.selectedProvider || !this.selectedRepo) {
      this.messageService.add({
        severity: 'warn',
        summary: '缺少上下文',
        detail: '请先选择平台接入与仓库。',
      });
      return;
    }

    const payload: SessionRequest = {
      providerId: this.selectedProvider.id,
      repoId: this.selectedRepo.id,
      repoName: this.selectedRepo.name,
      repoPathWithNamespace: this.selectedRepo.pathWithNamespace,
      repoHttpUrl: this.selectedRepo.httpUrlToRepo,
      defaultBranch: this.selectedRepo.defaultBranch ?? null,
    };

    this.sessionsService
      .createSession(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: '会话创建中',
            detail: '容器启动流程已触发。',
          });
          this.createVisible.set(false);
          this.loadSessions();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '创建失败',
            detail: '无法创建会话，请检查后端服务。',
          });
        },
      });
  }

  onRepoSelected(repo: GitLabRepoResponse | null): void {
    this.selectedRepo = repo;
  }

  onProviderSelected(provider: ProviderResponse | null): void {
    this.selectedProvider = provider;
    this.selectedRepo = null;
  }

  getProviderTypeLabel(type: ProviderType): string {
    switch (type) {
      case ProviderType.GITHUB:
        return 'GitHub';
      case ProviderType.GITLAB:
        return 'GitLab';
      case ProviderType.AZURE_DEVOPS:
        return 'Azure DevOps';
      default:
        return '未知平台';
    }
  }

  confirmDelete(session: SessionResponse): void {
    this.confirmationService.confirm({
      message: `确认删除会话 ${session.repoName} 吗？`,
      header: '删除会话',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.deleteSession(session),
    });
  }

  deleteSession(session: SessionResponse): void {
    this.sessionsService
      .deleteSession(session.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: '删除成功',
            detail: '会话已移除。',
          });
          this.loadSessions();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '删除失败',
            detail: '无法删除该会话。',
          });
        },
      });
  }

  viewLogs(session: SessionResponse): void {
    this.sessionsService
      .listSessionLogs(session.id, 0, 200)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (logs) => {
          this.logs.set(logs);
          this.logsVisible.set(true);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '日志获取失败',
            detail: '无法获取会话日志。',
          });
        },
      });
  }

  viewContext(session: SessionResponse): void {
    this.sessionsService
      .exportSessionContext(session.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (context: SessionContextResponse) => {
          this.contextPayload.set(JSON.stringify(context, null, 2));
          this.contextVisible.set(true);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '上下文导出失败',
            detail: '无法导出会话上下文。',
          });
        },
      });
  }

  openVscode(session: SessionResponse): void {
    if (session.status !== SessionStatus.READY) {
      this.messageService.add({
        severity: 'warn',
        summary: '会话未就绪',
        detail: '当前会话未处于可用状态。',
      });
      return;
    }
    if (!session.vscodeUrl) {
      this.messageService.add({
        severity: 'warn',
        summary: 'VSCode 未就绪',
        detail: '该会话暂未提供 VSCode Web 地址。',
      });
      return;
    }
    window.open(session.vscodeUrl, '_blank', 'noopener');
  }

  startContainer(session: SessionResponse): void {
    this.startingSessionId.set(session.id);
    this.httpClient
      .post<SessionResponse>(`${this.basePath}/sessions/${session.id}/container/start`, null)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.startingSessionId.set(null)),
      )
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: '容器已启动',
            detail: '会话容器已重新启动。',
          });
          this.loadSessions();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '启动失败',
            detail: '无法启动该会话容器。',
          });
        },
      });
  }

  canStartContainer(session: SessionResponse): boolean {
    return session.status === SessionStatus.STOPPED;
  }

  sessionSeverity(status: SessionStatus): 'success' | 'info' | 'danger' | 'warn' {
    if (status === SessionStatus.READY) {
      return 'success';
    }
    if (status === SessionStatus.STOPPED) {
      return 'warn';
    }
    if (status === SessionStatus.FAILED) {
      return 'danger';
    }
    return 'info';
  }

  sessionStatusLabel(status: SessionStatus): string {
    switch (status) {
      case SessionStatus.READY:
        return '可用';
      case SessionStatus.STOPPED:
        return '已停止';
      case SessionStatus.FAILED:
        return '失败';
      case SessionStatus.CREATING:
      default:
        return '创建中';
    }
  }
}
