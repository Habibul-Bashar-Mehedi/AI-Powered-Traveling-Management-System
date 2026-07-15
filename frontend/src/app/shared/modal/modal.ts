import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

let nextId = 0;

@Component({
  selector: 'app-modal',
  imports: [],
  templateUrl: './modal.html',
  styleUrl: './modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class Modal {
  readonly open = input.required<boolean>();
  readonly titleText = input<string>('');
  readonly subtitleText = input<string | null | undefined>(null);
  readonly icon = input<string | null | undefined>(null);
  readonly size = input<'md' | 'lg'>('md');
  readonly closeDisabled = input<boolean>(false);

  readonly closed = output<void>();

  readonly titleId = `app-modal-title-${nextId++}`;

  onBackdropClick(): void {
    this.requestClose();
  }

  onEscape(): void {
    if (this.open()) {
      this.requestClose();
    }
  }

  requestClose(): void {
    if (!this.closeDisabled()) {
      this.closed.emit();
    }
  }
}
