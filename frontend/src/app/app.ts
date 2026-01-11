import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MenuItem } from 'primeng/api';

import { SideNavComponent } from './components/side-nav/side-nav.component';
import { OverviewCardsComponent } from './components/overview-cards/overview-cards.component';
import { ProvidersPanelComponent } from './components/providers-panel/providers-panel.component';
import { SessionsPanelComponent } from './components/sessions-panel/sessions-panel.component';
import { ProviderResponse } from './core/openapi/model/provider-response';
import { SessionResponse } from './core/openapi/model/session-response';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    ToastModule,
    ConfirmDialogModule,
    SideNavComponent,
    OverviewCardsComponent,
    ProvidersPanelComponent,
    SessionsPanelComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.less',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  providers = signal<ProviderResponse[]>([]);
  sessions = signal<SessionResponse[]>([]);

  private readonly document = inject(DOCUMENT);

  menuItems: MenuItem[] = [
    {
      label: '概览',
      icon: 'pi pi-compass',
      command: () => this.scrollTo('section-overview'),
    },
    {
      label: '平台接入',
      icon: 'pi pi-sliders-h',
      command: () => this.scrollTo('section-providers'),
    },
    {
      label: '会话管理',
      icon: 'pi pi-comments',
      command: () => this.scrollTo('section-sessions'),
    },
  ];

  onProvidersUpdated(providers: ProviderResponse[]): void {
    this.providers.set(providers);
  }

  onSessionsUpdated(sessions: SessionResponse[]): void {
    this.sessions.set(sessions);
  }

  private scrollTo(sectionId: string): void {
    const target = this.document.getElementById(sectionId);
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
