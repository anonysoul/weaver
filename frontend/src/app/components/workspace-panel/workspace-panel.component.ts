import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-workspace-panel',
  standalone: true,
  templateUrl: './workspace-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WorkspacePanelComponent {}
