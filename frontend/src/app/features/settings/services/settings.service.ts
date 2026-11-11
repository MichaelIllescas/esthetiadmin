import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { BusinessSettings } from '../models/settings.models';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/settings`;

  get(): Observable<BusinessSettings> {
    return this.http.get<BusinessSettings>(this.url);
  }

  /** null or empty clears a value. */
  save(settings: BusinessSettings): Observable<BusinessSettings> {
    return this.http.put<BusinessSettings>(this.url, settings);
  }
}
