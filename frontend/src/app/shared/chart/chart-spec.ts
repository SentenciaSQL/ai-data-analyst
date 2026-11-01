export type ChartType = 'bar' | 'line' | 'area' | 'pie';

export interface ChartSeries {
  name: string;
  data: number[];
}

export interface ChartSpec {
  type: ChartType;
  title?: string;
  categories: string[];
  series: ChartSeries[];
}

const TYPES = new Set<ChartType>(['bar', 'line', 'area', 'pie']);

export function parseChartSpec(value: unknown): ChartSpec | null {
  if (!value || typeof value !== 'object') {
    return null;
  }
  const record = value as Record<string, unknown>;
  if (typeof record['type'] !== 'string' || !TYPES.has(record['type'] as ChartType)) {
    return null;
  }
  if (!Array.isArray(record['categories']) || record['categories'].length === 0 || record['categories'].length > 50) {
    return null;
  }
  if (!record['categories'].every((item) => typeof item === 'string' && item.length > 0 && item.length <= 80)) {
    return null;
  }
  if (!Array.isArray(record['series']) || record['series'].length === 0 || record['series'].length > 8) {
    return null;
  }
  const categories = record['categories'] as string[];
  const series: ChartSeries[] = [];
  for (const item of record['series']) {
    if (!item || typeof item !== 'object') return null;
    const entry = item as Record<string, unknown>;
    if (typeof entry['name'] !== 'string' || !Array.isArray(entry['data'])) return null;
    if (entry['data'].length !== categories.length) return null;
    if (!entry['data'].every((point) => typeof point === 'number' && Number.isFinite(point))) return null;
    series.push({ name: entry['name'], data: entry['data'] as number[] });
  }
  const title = typeof record['title'] === 'string' ? record['title'].slice(0, 120) : undefined;
  return { type: record['type'] as ChartType, title, categories, series };
}
