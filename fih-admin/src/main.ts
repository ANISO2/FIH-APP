import { bootstrapApplication } from '@angular/platform-browser';
import * as echarts from 'echarts';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { registerFihTheme } from './app/shared/echarts-theme';

// Register the centralized ECharts brand theme once, before bootstrap.
registerFihTheme(echarts);

bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));
