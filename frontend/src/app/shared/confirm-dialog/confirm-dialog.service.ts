import { Injectable, signal } from '@angular/core';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  /** Styles the confirm button as destructive (rose) instead of the default brand color. */
  danger?: boolean;
}

interface PendingConfirm extends Required<ConfirmOptions> {
  resolve: (value: boolean) => void;
}

/**
 * App-wide replacement for native confirm()/window.confirm() — those render as
 * unstyled OS dialogs that jar against the app's own designed-modal look.
 * Rendered once from the root App component (see ConfirmDialog), so any
 * service/component can just `await this.confirmDialog.confirm({...})`.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  readonly pending = signal<PendingConfirm | null>(null);

  confirm(options: ConfirmOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.pending.set({
        confirmLabel: 'Confirm',
        cancelLabel: 'Cancel',
        danger: false,
        ...options,
        resolve
      });
    });
  }

  resolveWith(value: boolean): void {
    const current = this.pending();
    if (!current) return;
    this.pending.set(null);
    current.resolve(value);
  }
}
