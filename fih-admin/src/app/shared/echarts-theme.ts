// Centralized ECharts brand theme — defined ONCE and reused by every chart.
// Series color order follows the project palette.

export const BRAND = {
  primary: '#1f5f8b',
  accent: '#0f9d9d',
  warn: '#b54708',
  purple: '#5b3aa6',
  success: '#0a7c4a',
  ink: '#1a2230',
  muted: '#5b6470',
  line: '#e2e6ec'
};

export const SERIES_COLORS = ['#1f5f8b', '#0f9d9d', '#b54708', '#5b3aa6', '#0a7c4a'];

/** Register a named theme 'fih' on the shared echarts instance. */
export function registerFihTheme(echarts: { registerTheme: (name: string, theme: object) => void }): void {
  echarts.registerTheme('fih', {
    color: SERIES_COLORS,
    textStyle: { fontFamily: 'Inter, system-ui, sans-serif', color: BRAND.ink },
    title: { textStyle: { color: BRAND.ink, fontWeight: 600 } },
    legend: { textStyle: { color: BRAND.muted } },
    grid: { left: 12, right: 16, top: 28, bottom: 8, containLabel: true },
    categoryAxis: {
      axisLine: { lineStyle: { color: BRAND.line } },
      axisTick: { show: false },
      axisLabel: { color: BRAND.muted },
      splitLine: { show: false }
    },
    valueAxis: {
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: BRAND.muted },
      splitLine: { lineStyle: { color: BRAND.line } }
    },
    tooltip: {
      backgroundColor: '#ffffff',
      borderColor: BRAND.line,
      borderWidth: 1,
      textStyle: { color: BRAND.ink },
      extraCssText: 'box-shadow:0 6px 20px rgba(16,30,54,.12); border-radius:10px;'
    }
  });
}
