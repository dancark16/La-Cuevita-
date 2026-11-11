import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthShellComponent } from '../auth-shell/auth-shell.component';
import { GoogleButtonComponent } from '../google-button/google-button.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent, GoogleButtonComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly showPassword = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  onGoogleSignedIn(): void {
    this.goAfterLogin();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.goAfterLogin();
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(this.messageFor(error));
      },
    });
  }

  private messageFor(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return 'Correo o contraseña incorrectos.';
    }
    if (error.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica que el backend esté corriendo.';
    }
    return 'Ocurrió un error inesperado. Intenta de nuevo.';
  }

  private goAfterLogin(): void {
    if (this.auth.isAdmin()) {
      this.router.navigate(['/admin']);
      return;
    }
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    const safe = returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//');
    this.router.navigateByUrl(safe ? returnUrl : '/');
  }
}
