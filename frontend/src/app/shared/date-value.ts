export type DateValue = Date | string | null;

export function toIsoDate(value: DateValue): string {
  if (!value) {
    return '';
  }
  if (typeof value === 'string') {
    const isoDate = value.match(/^\d{4}-\d{2}-\d{2}/)?.[0];
    if (isoDate) {
      return isoDate;
    }
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? '' : formatLocalDate(parsed);
  }
  return Number.isNaN(value.getTime()) ? '' : formatLocalDate(value);
}

export function toLocalDate(value: DateValue): Date | null {
  if (!value) {
    return null;
  }
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }
  const isoDate = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (isoDate) {
    return new Date(Number(isoDate[1]), Number(isoDate[2]) - 1, Number(isoDate[3]));
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function formatLocalDate(value: Date): string {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
