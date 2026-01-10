import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ProviderResponse } from '../../core/openapi';

export type ProviderForm = {
  name: string;
  baseUrl: string;
  token: string;
};

@Component({
  selector: 'app-provider-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './provider-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderPanelComponent {
  @Input({ required: true }) providers!: ProviderResponse[];
  @Input({ required: true }) loadingProviders!: boolean;
  @Input({ required: true }) showForm!: boolean;
  @Input({ required: true }) editingId!: number | null;
  @Input({ required: true }) form!: ProviderForm;

  @Output() startCreate = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() startEdit = new EventEmitter<ProviderResponse>();
  @Output() test = new EventEmitter<ProviderResponse>();
  @Output() remove = new EventEmitter<ProviderResponse>();
}
