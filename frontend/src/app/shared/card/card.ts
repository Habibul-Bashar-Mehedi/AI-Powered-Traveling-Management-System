import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-card',
  imports: [],
  templateUrl: './card.html',
  styleUrl: './card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Card {
  imageUrl = input<string | null | undefined>(null);
  imageAlt = input<string>('');
  badgeText = input<string | null | undefined>(null);
  title = input.required<string>();
  subtitle = input<string | null | undefined>(null);
  description = input<string | null | undefined>(null);
  metaPills = input<string[]>([]);
  showViewDetails = input<boolean>(true);
  viewDetailsLabel = input<string>('View Details');

  readonly viewDetails = output<void>();
}
