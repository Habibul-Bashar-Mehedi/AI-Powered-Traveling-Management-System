import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Shared loading placeholder — replaces the plain "Loading…" text that used to
 * be scattered across the app with something that actually reads as "working",
 * not "stalled". The shimmer gradient/keyframes are the same pattern already
 * proven in vendor-dashboard.css, just promoted so every page can reuse it
 * instead of each inventing its own.
 *
 *   spinner       - small spinner + label, for single summaries/panels
 *   shimmer-cards - a row of card-shaped placeholders, for grid sections
 *   shimmer-rows  - a stack of list-row placeholders, for tables/lists
 */
@Component({
  selector: 'app-loading-state',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './loading-state.html',
  styleUrl: './loading-state.css'
})
export class LoadingState {
  readonly variant = input<'spinner' | 'shimmer-cards' | 'shimmer-rows'>('spinner');
  readonly label = input<string>('Loading…');
  readonly count = input<number>(3);

  get items(): number[] {
    return Array.from({ length: this.count() });
  }
}
