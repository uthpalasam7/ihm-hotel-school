import { AbstractControl, ValidationErrors } from '@angular/forms';
import { toIsoDate } from '../shared/date-value';

export const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d(?::[0-5]\d(?:\.\d{1,9})?)?$/;
export function forwardTimes(control: AbstractControl): ValidationErrors | null {
  const { startTime, endTime } = control.value;
  if (!TIME_PATTERN.test(startTime) || !TIME_PATTERN.test(endTime)) return null;
  const seconds = (value: string) => { const [h, m, s = 0] = value.split(':').map(Number); return h * 3600 + m * 60 + s; };
  return seconds(startTime) < seconds(endTime) ? null : { timeOrder: true };
}
export function generationRange(control: AbstractControl): ValidationErrors | null {
  const from = toIsoDate(control.value.fromDate), to = toIsoDate(control.value.toDate);
  if (!from || !to) return null;
  const days = (Date.parse(to) - Date.parse(from)) / 86400000 + 1;
  return days < 1 ? { dateOrder: true } : days > 366 ? { rangeTooLong: true } : null;
}
