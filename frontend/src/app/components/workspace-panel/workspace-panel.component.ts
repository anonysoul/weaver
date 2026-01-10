import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SessionResponse } from '../../core/openapi';
import {
  GitCommandResponse,
  SessionContextResponse,
  SessionLogResponse,
  SessionRuntimeService,
} from '../../services/session-runtime.service';

@Component({
  selector: 'app-workspace-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './workspace-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspacePanelComponent implements OnChanges {
  @Input({ required: true }) session: SessionResponse | null = null;

  protected logs: SessionLogResponse[] = [];
  protected hasMoreLogs = false;
  protected loadingLogs = false;
  protected gitResult: GitCommandResponse | null = null;
  protected gitError: string | null = null;
  protected checkoutBranch = '';
  protected runningGit = false;
  protected context: SessionContextResponse | null = null;
  protected loadingContext = false;
  private readonly logPageSize = 1000;
  private logOffset = 0;

  constructor(
    private readonly runtimeApi: SessionRuntimeService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['session']) {
      this.logs = [];
      this.hasMoreLogs = false;
      this.logOffset = 0;
      this.gitResult = null;
      this.gitError = null;
      this.checkoutBranch = '';
      this.context = null;
      if (this.session) {
        this.refreshLogs();
      }
    }
  }

  refreshLogs(): void {
    if (!this.session) {
      return;
    }
    this.logs = [];
    this.hasMoreLogs = false;
    this.logOffset = 0;
    this.loadLogsPage(false);
  }

  loadMoreLogs(): void {
    this.loadLogsPage(true);
  }

  private loadLogsPage(append: boolean): void {
    if (!this.session || this.loadingLogs) {
      return;
    }
    this.loadingLogs = true;
    const offset = append ? this.logOffset : 0;
    this.runtimeApi.listLogs(this.session.id, offset, this.logPageSize).subscribe({
      next: (logs) => {
        this.logs = append ? [...this.logs, ...logs] : logs;
        this.logOffset = this.logs.length;
        this.hasMoreLogs = logs.length === this.logPageSize;
        this.loadingLogs = false;
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        this.gitError = err?.error?.error || '获取日志失败';
        this.loadingLogs = false;
        this.cdr.markForCheck();
      },
    });
  }

  runStatus(): void {
    this.runGitCommand({ command: 'STATUS' });
  }

  runPull(): void {
    this.runGitCommand({ command: 'PULL' });
  }

  runCheckout(): void {
    const branch = this.checkoutBranch.trim();
    if (!branch) {
      this.gitError = '请输入分支名称';
      return;
    }
    this.runGitCommand({ command: 'CHECKOUT', branch });
  }

  exportContext(): void {
    if (!this.session) {
      return;
    }
    this.loadingContext = true;
    this.runtimeApi.exportContext(this.session.id).subscribe({
      next: (context) => {
        this.context = context;
        this.loadingContext = false;
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        this.gitError = err?.error?.error || '导出上下文失败';
        this.loadingContext = false;
        this.cdr.markForCheck();
      },
    });
  }

  private runGitCommand(payload: {
    command: 'STATUS' | 'PULL' | 'CHECKOUT';
    branch?: string;
  }): void {
    if (!this.session || this.runningGit) {
      return;
    }
    this.runningGit = true;
    this.gitError = null;
    this.runtimeApi.runGitCommand(this.session.id, payload).subscribe({
      next: (result) => {
        this.gitResult = result;
        this.runningGit = false;
        this.refreshLogs();
        this.cdr.markForCheck();
      },
      error: (err: any) => {
        this.gitError = err?.error?.error || 'Git 操作失败';
        this.runningGit = false;
        this.cdr.markForCheck();
      },
    });
  }
}
