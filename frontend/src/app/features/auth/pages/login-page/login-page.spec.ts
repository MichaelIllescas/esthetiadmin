import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  const authService = { login: vi.fn() };
  const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };

  beforeEach(async () => {
    authService.login.mockReset();
    router.navigateByUrl.mockClear();
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
  });

  it('shows validation messages after an empty submit', () => {
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ingresá tu correo electrónico.');
    expect(fixture.nativeElement.textContent).toContain('Ingresá tu contraseña.');
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('submits valid credentials and goes to the home page', () => {
    authService.login.mockReturnValue(of({ token: 'token', expiresAt: '2026-09-26T00:00:00Z' }));
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    const inputs = fixture.nativeElement.querySelectorAll('input');
    inputs[0].value = 'admin';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = 'secret';
    inputs[1].dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.login).toHaveBeenCalledWith({ username: 'admin', password: 'secret' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('shows a safe message for invalid credentials', () => {
    authService.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    const inputs = fixture.nativeElement.querySelectorAll('input');
    inputs[0].value = 'admin';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = 'wrong';
    inputs[1].dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'El usuario o la contraseña no son correctos.',
    );
  });
});
