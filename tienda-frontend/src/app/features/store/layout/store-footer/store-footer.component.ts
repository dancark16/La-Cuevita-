import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StoreSettingsService } from '../../../../core/settings/store-settings.service';
import { bankLogo } from '../../../../shared/util/bank-logo';
import { StoreApiService } from '../../data/store-api.service';
import { StoreCategory } from '../../data/store.models';
import { BrandLogoComponent } from '../brand-logo/brand-logo.component';

interface FeatureItem {
  icon: string;
  title: string;
  subtitle: string;
  href: string | null;
}

@Component({
  selector: 'app-store-footer',
  standalone: true,
  imports: [NgTemplateOutlet, RouterLink, BrandLogoComponent],
  templateUrl: './store-footer.component.html',
  styleUrl: './store-footer.component.scss',
})
export class StoreFooterComponent {
  private readonly settings = inject(StoreSettingsService).settings;

  readonly year = new Date().getFullYear();
  readonly categories = signal<StoreCategory[]>([]);

  constructor() {
    inject(StoreApiService)
      .rootCategories()
      .subscribe((categories) => this.categories.set(categories));
  }

  readonly whatsappUrl = computed(() => {
    const number = this.settings().whatsappNumber;
    return number ? `https://wa.me/${number}` : null;
  });

  readonly whatsappLabel = computed(() => {
    const number = this.settings().whatsappNumber ?? '';
    return number.startsWith('593') ? `+${number.slice(0, 3)} ${number.slice(3)}` : number;
  });

  readonly instagram = computed(() => this.settings().instagramUser);

  readonly banks = computed(() =>
    this.settings().bankNames.map((name) => ({ name, logo: bankLogo(name) }))
  );

  readonly features = computed<FeatureItem[]>(() => [
    { icon: 'local_shipping', title: 'Envíos a todo el país', subtitle: 'Consulta condiciones', href: null },
    { icon: 'verified_user', title: 'Compra segura', subtitle: 'Tus datos protegidos', href: null },
    { icon: 'account_balance', title: 'Paga por transferencia', subtitle: 'Sube tu comprobante', href: null },
    { icon: 'chat', title: 'Atención personalizada', subtitle: 'Por WhatsApp', href: this.whatsappUrl() },
  ]);
}
