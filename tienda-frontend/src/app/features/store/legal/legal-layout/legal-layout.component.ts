import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { LegalInfoService } from '../legal-info.service';

interface LegalLink {
  path: string;
  label: string;
  icon: string;
}

const LINKS: LegalLink[] = [
  { path: 'terminos', label: 'Términos y condiciones', icon: 'gavel' },
  { path: 'privacidad', label: 'Política de privacidad', icon: 'shield' },
  { path: 'devoluciones', label: 'Cambios y devoluciones', icon: 'sync_alt' },
  { path: 'contacto', label: 'Contacto', icon: 'support_agent' },
];

@Component({
  selector: 'app-legal-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe],
  templateUrl: './legal-layout.component.html',
  styleUrl: './legal-layout.component.scss',
})
export class LegalLayoutComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly info = inject(LegalInfoService);

  readonly links = LINKS;

  private readonly data = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.route.firstChild?.snapshot.data ?? {})
    ),
    { initialValue: this.route.firstChild?.snapshot.data ?? {} }
  );

  readonly title = computed(() => (this.data()['title'] as string) ?? 'Información legal');
  readonly subtitle = computed(() => (this.data()['subtitle'] as string) ?? '');
  readonly icon = computed(() => (this.data()['icon'] as string) ?? 'gavel');
  readonly showUpdated = computed(() => this.data()['path'] !== 'contacto');
}
