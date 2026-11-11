import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { SettingsService } from './settings.service';

describe('SettingsService', () => {
  let service: SettingsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SettingsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads and saves the settings', () => {
    service.get().subscribe();
    expect(http.expectOne(`${environment.apiUrl}/settings`).request.method).toBe('GET');

    const settings = { notificationEmail: 'esthetiadmin@mail.com', contactPhone: null };
    service.save(settings).subscribe();
    const put = http.expectOne(`${environment.apiUrl}/settings`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual(settings);
  });
});
