import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthTokenStore } from '../../../core/services/auth-token-store';
import { LoginCredentials, LoginResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStore = inject(AuthTokenStore);

  login(credentials: LoginCredentials) {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, credentials)
      .pipe(tap((response) => this.tokenStore.set(response.token)));
  }
}
