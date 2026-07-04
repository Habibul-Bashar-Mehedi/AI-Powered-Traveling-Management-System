import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

type InfoPageKind = 'privacy-policy' | 'terms-of-service' | 'help-center';

@Component({
  selector: 'app-info-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './info-page.html',
  styleUrls: ['./info-page.css'],
})
export class InfoPage {
  kind: InfoPageKind;

  constructor(private route: ActivatedRoute) {
    this.kind = (this.route.snapshot.routeConfig?.path as InfoPageKind) || 'help-center';
  }
}
