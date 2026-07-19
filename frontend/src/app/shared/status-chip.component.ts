import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-status-chip',
  templateUrl: './status-chip.component.html',
  styleUrl: './status-chip.component.scss',
})
export class StatusChipComponent {
  readonly value = input.required<string>();
  readonly label = computed(() => this.value().replaceAll('_', ' ').toLowerCase());
  readonly tone = computed(() => {
    switch (this.value().toUpperCase()) {
      case 'ACTIVE':
      case 'PAID':
      case 'PRESENT':
      case 'COMPLETED':
        return 'success';
      case 'INACTIVE':
      case 'DISABLED':
      case 'CANCELLED':
      case 'OVERDUE':
      case 'ABSENT':
        return 'danger';
      case 'DUE':
      case 'LATE':
      case 'PASSWORD_CHANGE_REQUIRED':
        return 'warning';
      default:
        return 'neutral';
    }
  });
}
