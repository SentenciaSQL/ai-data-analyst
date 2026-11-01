import { Component, computed, input } from '@angular/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';
import { ChartSpec } from './chart-spec';

echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TitleComponent, TooltipComponent, CanvasRenderer]);

@Component({
  selector: 'app-chart',
  imports: [NgxEchartsDirective],
  providers: [provideEchartsCore({ echarts })],
  template: '<div echarts [options]="options()" class="chart" role="img" [attr.aria-label]="spec().title || \'Chart\'"></div>',
})
export class ChartView {
  readonly spec = input.required<ChartSpec>();
  readonly options = computed(() => toOptions(this.spec()));
}

function toOptions(spec: ChartSpec): Record<string, unknown> {
  const title = spec.title ? { text: spec.title, left: 'left', textStyle: { fontSize: 14, fontWeight: 600 } } : undefined;
  if (spec.type === 'pie') {
    return {
      title,
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: '68%',
        data: spec.categories.map((name, index) => ({ name, value: spec.series[0]?.data[index] ?? 0 })),
      }],
    };
  }
  return {
    title,
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 40, right: 16, top: spec.title ? 48 : 24, bottom: 48 },
    xAxis: { type: 'category', data: spec.categories },
    yAxis: { type: 'value' },
    series: spec.series.map((series) => ({
      name: series.name,
      type: spec.type === 'bar' ? 'bar' : 'line',
      data: series.data,
      areaStyle: spec.type === 'area' ? {} : undefined,
      smooth: spec.type !== 'bar',
    })),
  };
}
