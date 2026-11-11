import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { CatalogService } from './catalog.service';

describe('CatalogService', () => {
  let service: CatalogService;
  let http: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CatalogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function expectRequest(method: string, url: string, body?: unknown) {
    const request = http.expectOne(url);
    expect(request.request.method).toBe(method);
    if (body !== undefined) {
      expect(request.request.body).toEqual(body);
    }
    request.flush(null);
  }

  it('lists the categories', () => {
    service.getCategories().subscribe();
    expectRequest('GET', `${api}/categories`);
  });

  it('creates, updates, toggles and deletes categories', () => {
    service.createCategory({ name: 'Masajes', appointmentMinutes: 60 }).subscribe();
    expectRequest('POST', `${api}/categories`, { name: 'Masajes', appointmentMinutes: 60 });

    service.updateCategory(1, { name: 'Depilación', appointmentMinutes: 30 }).subscribe();
    expectRequest('PUT', `${api}/categories/1`, { name: 'Depilación', appointmentMinutes: 30 });

    service.setCategoryActive(1, false).subscribe();
    expectRequest('PATCH', `${api}/categories/1/active`, { active: false });

    service.deleteCategory(1).subscribe();
    expectRequest('DELETE', `${api}/categories/1`);
  });

  it('creates, updates, toggles and deletes services', () => {
    const request = { categoryId: 1, name: 'Bozo', description: null, price: 15000, bookable: true };

    service.createOffering(request).subscribe();
    expectRequest('POST', `${api}/services`, request);

    service.updateOffering(10, request).subscribe();
    expectRequest('PUT', `${api}/services/10`, request);

    service.setOfferingActive(10, true).subscribe();
    expectRequest('PATCH', `${api}/services/10/active`, { active: true });

    service.deleteOffering(10).subscribe();
    expectRequest('DELETE', `${api}/services/10`);
  });
});
