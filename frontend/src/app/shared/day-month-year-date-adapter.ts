import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

@Injectable()
export class DayMonthYearDateAdapter extends NativeDateAdapter {
  override parse(value: unknown): Date | null {
    if (typeof value !== 'string') {
      return super.parse(value);
    }
    if (!value.trim()) {
      return null;
    }
    const parts = value.trim().match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (!parts) {
      return this.invalid();
    }
    const [, day, month, year] = parts.map(Number);
    try {
      const date = this.createDate(year, month - 1, day);
      return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
        ? date : this.invalid();
    } catch {
      return this.invalid();
    }
  }
}
