import { NgOptimizedImage } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiErrorResponse } from '../../models/auth.models';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, NgOptimizedImage],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isPasswordVisible = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly feedback = signal<{ type: 'error'; message: string } | null>(null);

  protected readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected togglePasswordVisibility(): void {
    this.isPasswordVisible.update((visible) => !visible);
  }

  protected submit(): void {
    this.feedback.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.authService
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/');
        },
        error: (error: HttpErrorResponse) => {
          const apiError = error.error as ApiErrorResponse | null;
          this.feedback.set({
            type: 'error',
            message:
              error.status === 401
                ? 'El usuario o la contraseña no son correctos.'
                : (apiError?.message ?? 'No pudimos iniciar sesión. Intentá nuevamente.'),
          });
        },
      });
  }
}
