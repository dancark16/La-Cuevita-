import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthShellComponent } from '../auth-shell/auth-shell.component';
import { GoogleButtonComponent } from '../google-button/google-button.component';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  return group.get('password')?.value === group.get('confirm')?.value ? null : { mismatch: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent, GoogleButtonComponent],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly showPassword = signal(false);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', [Validators.required]],
    },
    { validators: passwordsMatch }
  );

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

    const { email, password } = this.form.getRawValue();
    this.auth.register({ email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/']);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(this.messageFor(error));
      },
    });
  }

  private messageFor(error: HttpErrorResponse): string {
    if (error.status === 409) {
      return 'Ya existe una cuenta con ese correo.';
    }
    if (error.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    return 'No pudimos crear la cuenta. Intenta de nuevo.';
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
