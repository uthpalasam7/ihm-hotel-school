import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-page-state',
  imports: [MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './page-state.component.html',
  styleUrl: './page-state.component.scss',
})
export class PageStateComponent {
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly empty = input(false);
  readonly loadingLabel = input('Loading…');
  readonly emptyTitle = input('Nothing to show');
  readonly emptyMessage = input('');
  readonly retry = output<void>();
}
