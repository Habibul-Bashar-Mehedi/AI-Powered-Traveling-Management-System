import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Generic slide-in-from-the-right panel. Content is projected via <ng-content>;
 * the host page owns the open/closed state and passes it in via `open`.
 */
@Component({
  selector: 'app-side-drawer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './side-drawer.html',
  styleUrl: './side-drawer.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SideDrawer {
  readonly open = input(false);
  readonly title = input('');

  readonly closed = output<void>();

  close(): void {
    this.closed.emit();
  }
}
