import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import { ProvidersService } from '../../core/openapi/api/providers.service';
import { ProviderResponse } from '../../core/openapi/model/provider-response';
import { GitLabRepoResponse } from '../../core/openapi/model/git-lab-repo-response';

@Component({
  selector: 'app-repos-panel',
  standalone: true,
  imports: [CommonModule, TableModule, CardModule, TagModule, ToolbarModule],
  templateUrl: './repos-panel.component.html',
  styleUrl: './repos-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReposPanelComponent implements OnChanges {
  @Input() provider: ProviderResponse | null = null;
  @Output() repoSelected = new EventEmitter<GitLabRepoResponse | null>();

  repos = signal<GitLabRepoResponse[]>([]);
  loading = signal(false);

  selectedRepo: GitLabRepoResponse | null = null;

  private readonly providersService = inject(ProvidersService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['provider']) {
      if (this.provider) {
        this.loadRepos();
      } else {
        this.repos.set([]);
        this.selectedRepo = null;
        this.repoSelected.emit(null);
      }
    }
  }

  loadRepos(): void {
    if (!this.provider) {
      return;
    }
    this.loading.set(true);
    this.providersService
      .listProviderRepos(this.provider.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (repos) => {
          const sorted = [...repos].sort((left, right) => {
            const leftKey = left.pathWithNamespace ?? left.name;
            const rightKey = right.pathWithNamespace ?? right.name;
            return leftKey.localeCompare(rightKey);
          });
          this.repos.set(sorted);
          const nextSelection =
            sorted.find((repo) => repo.id === this.selectedRepo?.id) || sorted[0] || null;
          this.selectedRepo = nextSelection;
          this.repoSelected.emit(nextSelection);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '加载失败',
            detail: '无法获取仓库列表。',
          });
        },
      });
  }

  selectRepo(repo: GitLabRepoResponse | GitLabRepoResponse[] | undefined): void {
    const resolved = Array.isArray(repo) ? repo[0] : repo;
    if (!resolved) {
      return;
    }
    this.selectedRepo = resolved;
    this.repoSelected.emit(resolved);
  }
}
