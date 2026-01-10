import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { GitLabRepoResponse, ProviderResponse } from '../../core/openapi';

@Component({
  selector: 'app-repo-catalog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './repo-catalog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RepoCatalogComponent {
  @Input({ required: true }) providers!: ProviderResponse[];
  @Input({ required: true }) repos!: Record<number, GitLabRepoResponse[]>;
  @Input({ required: true }) loadingRepos!: Record<number, boolean>;

  @Output() loadRepos = new EventEmitter<ProviderResponse>();
}
