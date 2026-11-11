import { formatCompactPrice, formatDate, formatDayAndMonth, formatMonth, formatPercent, formatPrice, formatShortDate, todayIso } from './format';

describe('formatPrice', () => {
  const normalize = (text: string) => text.replace(/\s/g, ' ');

  it('formats whole prices without decimals, with the Argentine separators', () => {
    expect(normalize(formatPrice(15000))).toBe('$ 15.000');
  });

  it('keeps the cents when there are any', () => {
    expect(normalize(formatPrice(1500.5))).toBe('$ 1.500,50');
  });
});

describe('dates', () => {
  it('writes a full date in Spanish, without shifting the day', () => {
    expect(formatDate('1985-11-03')).toBe('3 de noviembre de 1985');
    expect(formatDate('2000-01-01')).toBe('1 de enero de 2000');
  });

  it('writes a compact date with two digits for the day and the month', () => {
    expect(formatShortDate('1985-11-03')).toBe('03/11/1985');
    expect(formatShortDate('2026-01-09')).toBe('09/01/2026');
  });

  it('writes only the day and month', () => {
    expect(formatDayAndMonth('1985-11-03')).toBe('3 de noviembre');
  });

  it('gives today in the format the API and the date inputs use', () => {
    expect(todayIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});

describe('reports formats', () => {
  it('writes a month as a short name and the year', () => {
    expect(formatMonth('2026-09')).toMatch(/^sep/);
    expect(formatMonth('2026-09')).toContain('2026');
    expect(formatMonth('2026-01')).toMatch(/^ene/);
  });

  it('writes a percentage with two decimals and the Argentine comma', () => {
    expect(formatPercent(54.545).replace(/\s/g, ' ')).toBe('54,55 %');
    expect(formatPercent(0).replace(/\s/g, ' ')).toBe('0,00 %');
  });

  it('shortens the amounts of the chart axes', () => {
    expect(formatCompactPrice(1500).replace(/\s/g, ' ')).toMatch(/^\$ 1,5/);
    expect(formatCompactPrice(2500000)).toContain('$');
  });

  it('writes a negative amount of an axis with the minus sign first, like the rest of the app', () => {
    expect(formatCompactPrice(-1000000).replace(/\s/g, ' ')).toMatch(/^-\$ 1/);
  });
});
