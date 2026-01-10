import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  GitLabRepoResponse,
  ProviderResponse,
  SessionRequest,
  SessionResponse,
  SessionStatus
} from '../../core/openapi';

@Component({
  selector: 'app-session-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './session-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SessionPanelComponent {
  @Input({ required: true }) providers!: ProviderResponse[];
  @Input({ required: true }) repos!: Record<number, GitLabRepoResponse[]>;
  @Input({ required: true }) sessions!: SessionResponse[];
  @Input({ required: true }) loadingSessions!: boolean;
  @Input({ required: true }) creatingSession!: boolean;

  @Output() refreshSessions = new EventEmitter<void>();
  @Output() createSession = new EventEmitter<SessionRequest>();
  @Output() deleteSession = new EventEmitter<SessionResponse>();

  protected selectedProviderId: number | null = null;
  protected selectedRepoId: number | null = null;

  get availableRepos(): GitLabRepoResponse[] {
    if (this.selectedProviderId === null) {
      return [];
    }
    return this.repos[this.selectedProviderId] ?? [];
  }

  get selectedRepo(): GitLabRepoResponse | null {
    if (this.selectedRepoId === null) {
      return null;
    }
    return this.availableRepos.find((repo) => repo.id === this.selectedRepoId) ?? null;
  }

  onProviderChange(): void {
    this.selectedRepoId = null;
  }

  requestCreate(): void {
    if (this.selectedProviderId === null || !this.selectedRepo) {
      return;
    }
    const repo = this.selectedRepo;
    this.createSession.emit({
      providerId: this.selectedProviderId,
      repoId: repo.id,
      repoName: repo.name,
      repoPathWithNamespace: repo.pathWithNamespace,
      repoHttpUrl: repo.httpUrlToRepo,
      defaultBranch: repo.defaultBranch ?? null
    });
  }

  statusLabel(status: SessionStatus): string {
    switch (status) {
      case SessionStatus.READY:
        return '就绪';
      case SessionStatus.FAILED:
        return '失败';
      case SessionStatus.CREATING:
      default:
        return '初始化中';
    }
  }

  statusClass(status: SessionStatus): string {
    switch (status) {
      case SessionStatus.READY:
        return 'status-ready';
      case SessionStatus.FAILED:
        return 'status-failed';
      case SessionStatus.CREATING:
      default:
        return 'status-creating';
    }
  }

  providerName(providerId: number): string {
    return this.providers.find((provider) => provider.id === providerId)?.name ?? `#${providerId}`;
  }
}
