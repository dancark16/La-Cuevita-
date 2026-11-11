import { Component, ElementRef, NgZone, OnInit, ViewChild, inject, output, signal } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/auth/auth.service';

const GSI_SRC = 'https://accounts.google.com/gsi/client';

interface GoogleIdApi {
  initialize(config: { client_id: string; callback: (r: { credential: string }) => void }): void;
  renderButton(element: HTMLElement, options: Record<string, unknown>): void;
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleIdApi } };
  }
}

@Component({
  selector: 'app-google-button',
  standalone: true,
  templateUrl: './google-button.component.html',
  styleUrl: './google-button.component.scss',
})
export class GoogleButtonComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly zone = inject(NgZone);

  @ViewChild('target', { static: true }) private target!: ElementRef<HTMLElement>;

  readonly signedIn = output<void>();
  readonly failed = output<string>();

  readonly enabled = signal(!!environment.googleClientId);

  ngOnInit(): void {
    if (!this.enabled()) {
      return;
    }
    this.loadScript().then(() => this.render());
  }

  private loadScript(): Promise<void> {
    if (window.google) {
      return Promise.resolve();
    }
    return new Promise((resolve) => {
      const script = document.createElement('script');
      script.src = GSI_SRC;
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => this.zone.run(() => this.failed.emit('No se pudo cargar Google.'));
      document.head.appendChild(script);
    });
  }

  private render(): void {
    const api = window.google?.accounts.id;
    if (!api) {
      return;
    }
    api.initialize({
      client_id: environment.googleClientId,
      callback: ({ credential }) => this.zone.run(() => this.exchange(credential)),
    });
    api.renderButton(this.target.nativeElement, {
      theme: 'outline',
      size: 'large',
      text: 'continue_with',
      shape: 'rectangular',
      locale: 'es',
      width: Math.min(this.target.nativeElement.offsetWidth || 360, 400),
    });
  }

  private exchange(credential: string): void {
    this.auth.loginWithGoogle(credential).subscribe({
      next: () => this.signedIn.emit(),
      error: () => this.failed.emit('No pudimos iniciar sesión con Google.'),
    });
  }
}
