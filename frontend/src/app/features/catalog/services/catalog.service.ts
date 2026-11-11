import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Category, CategoryRequest, Offering, OfferingRequest } from '../models/catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly categoriesUrl = `${environment.apiUrl}/categories`;
  private readonly servicesUrl = `${environment.apiUrl}/services`;

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.categoriesUrl);
  }

  createCategory(request: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.categoriesUrl, request);
  }

  updateCategory(id: number, request: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.categoriesUrl}/${id}`, request);
  }

  setCategoryActive(id: number, active: boolean): Observable<Category> {
    return this.http.patch<Category>(`${this.categoriesUrl}/${id}/active`, { active });
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.categoriesUrl}/${id}`);
  }

  createOffering(request: OfferingRequest): Observable<Offering> {
    return this.http.post<Offering>(this.servicesUrl, request);
  }

  updateOffering(id: number, request: OfferingRequest): Observable<Offering> {
    return this.http.put<Offering>(`${this.servicesUrl}/${id}`, request);
  }

  setOfferingActive(id: number, active: boolean): Observable<Offering> {
    return this.http.patch<Offering>(`${this.servicesUrl}/${id}/active`, { active });
  }

  deleteOffering(id: number): Observable<void> {
    return this.http.delete<void>(`${this.servicesUrl}/${id}`);
  }
}
