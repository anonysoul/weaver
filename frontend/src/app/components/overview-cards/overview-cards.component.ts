import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ProviderResponse } from '../../core/openapi/model/provider-response';
import { ProviderType } from '../../core/openapi/model/provider-type';
import { SessionResponse } from '../../core/openapi/model/session-response';
import { SessionStatus } from '../../core/openapi/model/session-status';

interface ProviderStat {
  type: ProviderType;
  label: string;
  icon: string;
  iconType: 'pi' | 'text';
  tone: string;
  count: number;
}

interface SessionStat {
  status: SessionStatus;
  label: string;
  tone: string;
  count: number;
}

@Component({
  selector: 'app-overview-cards',
  standalone: true,
  imports: [CommonModule, CardModule, TagModule],
  templateUrl: './overview-cards.component.html',
  styleUrl: './overview-cards.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OverviewCardsComponent {
  @Input() providers: ProviderResponse[] = [];
  @Input() sessions: SessionResponse[] = [];

  private readonly providerCatalog: Omit<ProviderStat, 'count'>[] = [
    {
      type: ProviderType.GITLAB,
      label: 'GitLab',
      icon: 'GL',
      iconType: 'text',
      tone: 'gitlab',
    },
    {
      type: ProviderType.GITHUB,
      label: 'GitHub',
      icon: 'pi pi-github',
      iconType: 'pi',
      tone: 'github',
    },
    {
      type: ProviderType.AZURE_DEVOPS,
      label: 'Azure DevOps',
      icon: 'pi pi-microsoft',
      iconType: 'pi',
      tone: 'azure',
    },
  ];

  private readonly sessionCatalog: Omit<SessionStat, 'count'>[] = [
    { status: SessionStatus.CREATING, label: '启动中', tone: 'creating' },
    { status: SessionStatus.READY, label: '已就绪', tone: 'ready' },
    { status: SessionStatus.STOPPED, label: '已停止', tone: 'stopped' },
    { status: SessionStatus.FAILED, label: '失败', tone: 'failed' },
  ];

  get providerStats(): ProviderStat[] {
    const counts = this.providers.reduce(
      (acc, provider) => {
        acc[provider.type] = (acc[provider.type] ?? 0) + 1;
        return acc;
      },
      {} as Record<ProviderType, number>,
    );

    return this.providerCatalog.map((item) => ({
      ...item,
      count: counts[item.type] ?? 0,
    }));
  }

  get sessionStats(): SessionStat[] {
    const counts = this.sessions.reduce(
      (acc, session) => {
        acc[session.status] = (acc[session.status] ?? 0) + 1;
        return acc;
      },
      {} as Record<SessionStatus, number>,
    );

    return this.sessionCatalog.map((item) => ({
      ...item,
      count: counts[item.status] ?? 0,
    }));
  }
}
