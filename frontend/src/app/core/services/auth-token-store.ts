import { Injectable, signal } from '@angular/core';
import { readJwtPayload } from './jwt';

const STORAGE_KEY = 'esthetiadmin.auth.token';

/**
 * Keeps the session token. It survives a page reload (sessionStorage) and disappears when the
 * tab is closed. An expired or unreadable token is discarded on startup.
 */
@Injectable({ providedIn: 'root' })
export class AuthTokenStore {
  private readonly tokenState = signal<string | null>(this.restore());
  readonly token = this.tokenState.asReadonly();

  set(token: string): void {
    this.tokenState.set(token);
    this.persist(token);
  }

  clear(): void {
    this.tokenState.set(null);
    this.persist(null);
  }

  private restore(): string | null {
    try {
      const token = sessionStorage.getItem(STORAGE_KEY);
      if (token && !this.isExpired(token)) {
        return token;
      }
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Storage can be unavailable (private mode, blocked site data): the session lives in memory only.
    }
    return null;
  }

  private persist(token: string | null): void {
    try {
      if (token) {
        sessionStorage.setItem(STORAGE_KEY, token);
      } else {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      // See restore(): memory-only session.
    }
  }

  private isExpired(token: string): boolean {
    const exp = readJwtPayload(token)?.exp;
    return typeof exp !== 'number' || exp * 1000 <= Date.now();
  }
}
