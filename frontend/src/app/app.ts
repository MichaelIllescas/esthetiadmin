import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PwaUpdateBanner } from './shared/components/pwa-update-banner/pwa-update-banner';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, PwaUpdateBanner],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
