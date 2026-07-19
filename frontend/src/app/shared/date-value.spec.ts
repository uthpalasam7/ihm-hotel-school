import { describe, expect, it } from 'vitest';
import { toIsoDate, toLocalDate } from './date-value';

describe('date value helpers', () => {
  it('serializes a selected local date without shifting its calendar day', () => {
    expect(toIsoDate(new Date(2026, 6, 1))).toBe('2026-07-01');
  });

  it('deserializes an API date into the same local calendar day', () => {
    const date = toLocalDate('2026-07-01');

    expect(date?.getFullYear()).toBe(2026);
    expect(date?.getMonth()).toBe(6);
    expect(date?.getDate()).toBe(1);
  });
});
