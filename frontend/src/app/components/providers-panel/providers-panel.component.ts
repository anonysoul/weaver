import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Output,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { ConfirmationService, MessageService } from 'primeng/api';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { ProvidersService } from '../../core/openapi/api/providers.service';
import { ProviderResponse } from '../../core/openapi/model/provider-response';
import { ProviderRequest } from '../../core/openapi/model/provider-request';
import { ProviderType } from '../../core/openapi/model/provider-type';

import loader from '@monaco-editor/loader';
import type * as Monaco from 'monaco-editor';

const DEFAULT_GIT_CONFIG = '[credential]\n        helper = store\n';

interface ProviderFormState {
  name: string;
  baseUrl: string;
  token: string;
  type: ProviderType;
  gitConfig: string;
}

@Component({
  selector: 'app-providers-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    CardModule,
    DialogModule,
    SelectModule,
    InputTextModule,
    PasswordModule,
    TagModule,
    ToolbarModule,
  ],
  templateUrl: './providers-panel.component.html',
  styleUrl: './providers-panel.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProvidersPanelComponent implements AfterViewInit, OnDestroy {
  @Output() providersUpdated = new EventEmitter<ProviderResponse[]>();
  @ViewChild('gitConfigEditor') gitConfigEditor?: ElementRef<HTMLDivElement>;

  providers = signal<ProviderResponse[]>([]);
  loading = signal(false);
  dialogVisible = signal(false);

  editingProvider: ProviderResponse | null = null;

  formState: ProviderFormState = this.createEmptyForm();

  providerOptions = [
    { label: 'GitLab', value: ProviderType.GITLAB },
    { label: 'GitHub', value: ProviderType.GITHUB },
    { label: 'Azure DevOps', value: ProviderType.AZURE_DEVOPS },
  ];

  baseUrlPlaceholder = '';
  accountPlaceholder = '';
  tokenPlaceholder = '';

  private readonly providersService = inject(ProvidersService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);
  private editor: Monaco.editor.IStandaloneCodeEditor | null = null;
  private editorModel: Monaco.editor.ITextModel | null = null;

  constructor() {
    this.loadProviders();
    this.updatePlaceholders(this.formState.type);
  }

  ngAfterViewInit(): void {
    loader.config({ paths: { vs: 'assets/monaco/vs' } });
  }

  ngOnDestroy(): void {
    this.disposeEditor();
  }

  loadProviders(): void {
    this.loading.set(true);
    this.providersService
      .listProviders()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (providers) => {
          this.providers.set(providers);
          this.providersUpdated.emit(providers);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '加载失败',
            detail: '无法获取平台接入列表。',
          });
        },
      });
  }

  openCreate(): void {
    this.editingProvider = null;
    this.formState = this.createEmptyForm();
    this.updatePlaceholders(this.formState.type);
    this.dialogVisible.set(true);
    this.queueEditorInit();
  }

  openEdit(provider: ProviderResponse): void {
    this.editingProvider = provider;
    this.formState = {
      name: provider.name,
      baseUrl: provider.baseUrl,
      token: '',
      type: provider.type,
      gitConfig: provider.gitConfig.trimEnd() || DEFAULT_GIT_CONFIG,
    };
    this.updatePlaceholders(this.formState.type);
    this.dialogVisible.set(true);
    this.queueEditorInit();
  }

  saveProvider(): void {
    const gitConfig = this.formState.gitConfig.trimEnd() || DEFAULT_GIT_CONFIG;
    const payload: ProviderRequest = {
      name: this.formState.name.trim(),
      baseUrl: this.formState.baseUrl.trim(),
      token: this.formState.token.trim(),
      type: this.formState.type,
      gitConfig,
    };

    if (!payload.name || !payload.baseUrl || !payload.token) {
      this.messageService.add({
        severity: 'warn',
        summary: '信息不完整',
        detail: '请填写名称、地址与 Token。',
      });
      return;
    }

    const request$ = this.editingProvider
      ? this.providersService.updateProvider(this.editingProvider.id, payload)
      : this.providersService.createProvider(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: '保存成功',
          detail: this.editingProvider ? '平台接入已更新。' : '平台接入已创建。',
        });
        this.onDialogVisibleChange(false);
        this.loadProviders();
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: '保存失败',
          detail: '请检查配置或后端服务状态。',
        });
      },
    });
  }

  confirmDelete(provider: ProviderResponse): void {
    this.confirmationService.confirm({
      message: `确认删除 ${provider.name} 吗？`,
      header: '删除平台接入',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.deleteProvider(provider),
    });
  }

  deleteProvider(provider: ProviderResponse): void {
    this.providersService
      .deleteProvider(provider.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: '删除成功',
            detail: '平台接入已移除。',
          });
          this.loadProviders();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '删除失败',
            detail: '无法删除该平台接入。',
          });
        },
      });
  }

  testConnection(provider: ProviderResponse): void {
    this.providersService
      .testProviderConnection(provider.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.messageService.add({
            severity: result.ok ? 'success' : 'warn',
            summary: '连接测试',
            detail: result.message,
          });
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: '连接失败',
            detail: '无法完成连接测试。',
          });
        },
      });
  }

  private createEmptyForm(): ProviderFormState {
    const defaultType = ProviderType.GITLAB;
    return {
      name: '',
      baseUrl: this.getBaseUrlForType(defaultType),
      token: '',
      type: defaultType,
      gitConfig: DEFAULT_GIT_CONFIG,
    };
  }

  onTypeChange(type: ProviderType): void {
    this.formState.type = type;
    this.formState.baseUrl = this.getBaseUrlForType(type);
    this.updatePlaceholders(type);
  }

  private updatePlaceholders(type: ProviderType): void {
    if (type === ProviderType.GITHUB) {
      this.baseUrlPlaceholder = 'https://api.github.com';
      this.accountPlaceholder = 'GitHub 用户名';
      this.tokenPlaceholder = 'GitHub Personal Access Token';
      return;
    }
    if (type === ProviderType.AZURE_DEVOPS) {
      this.baseUrlPlaceholder = 'https://dev.azure.com/your-org';
      this.accountPlaceholder = 'Azure DevOps 用户名';
      this.tokenPlaceholder = 'Azure DevOps PAT';
      return;
    }
    this.baseUrlPlaceholder = 'https://gitlab.com';
    this.accountPlaceholder = 'GitLab 用户名';
    this.tokenPlaceholder = 'GitLab Access Token';
  }

  private getBaseUrlForType(type: ProviderType): string {
    if (type === ProviderType.GITHUB) {
      return 'https://api.github.com';
    }
    if (type === ProviderType.AZURE_DEVOPS) {
      return 'https://dev.azure.com/your-org';
    }
    return 'https://gitlab.com';
  }

  getAccessTokenUrl(): string {
    if (this.formState.type === ProviderType.GITHUB) {
      return 'https://github.com/settings/tokens';
    }
    if (this.formState.type === ProviderType.AZURE_DEVOPS) {
      return 'https://learn.microsoft.com/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=azure-devops';
    }
    return 'https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html';
  }

  onDialogVisibleChange(visible: boolean): void {
    this.dialogVisible.set(visible);
    if (!visible) {
      this.disposeEditor();
      return;
    }
    this.queueEditorInit();
  }

  private queueEditorInit(): void {
    setTimeout(() => {
      void this.initEditor();
    }, 0);
  }

  private async initEditor(): Promise<void> {
    const host = this.gitConfigEditor?.nativeElement;
    if (!host) {
      return;
    }
    const monaco = await loader.init();
    if (!this.editorModel) {
      this.editorModel = monaco.editor.createModel(this.formState.gitConfig, 'ini');
    } else {
      this.editorModel.setValue(this.formState.gitConfig);
    }
    if (this.editor) {
      this.editor.dispose();
    }
    this.editor = monaco.editor.create(host, {
      model: this.editorModel,
      theme: 'vs-dark',
      automaticLayout: true,
      scrollBeyondLastLine: false,
      minimap: { enabled: false },
      fontSize: 13,
      wordWrap: 'on',
    });
    this.editor.onDidChangeModelContent(() => {
      this.formState.gitConfig = this.editorModel?.getValue() ?? '';
    });
  }

  private disposeEditor(): void {
    this.editor?.dispose();
    this.editor = null;
    this.editorModel?.dispose();
    this.editorModel = null;
  }
}
