import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { Layout } from './app/layout/layout/layout/layout';

bootstrapApplication(Layout, appConfig)
  .catch((err) => console.error(err));
