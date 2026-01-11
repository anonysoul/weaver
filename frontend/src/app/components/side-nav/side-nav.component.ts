import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-side-nav',
  standalone: true,
  imports: [MenuModule],
  templateUrl: './side-nav.component.html',
  styleUrl: './side-nav.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SideNavComponent {
  @Input() items: MenuItem[] = [];
  @Input() brand = 'Weaver';
  @Input() tagline = 'AI Coding Orchestration';
}
