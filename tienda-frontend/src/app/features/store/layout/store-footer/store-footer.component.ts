import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { StoreSettingsService } from '../../../../core/settings/store-settings.service';

interface FeatureItem {
  icon: string;
  title: string;
  subtitle: string;
  href: string | null;
}

@Component({
  selector: 'app-store-footer',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './store-footer.component.html',
  styleUrl: './store-footer.component.scss',
})
export class StoreFooterComponent {
  private readonly settings = inject(StoreSettingsService).settings;

  readonly year = new Date().getFullYear();

  readonly features = computed<FeatureItem[]>(() => {
    const { whatsappNumber, instagramUser } = this.settings();
    return [
    {
      icon: 'local_shipping',
      title: 'Envíos a todo el país',
      subtitle: '(Consulta condiciones)',
      href: null,
    },
    {
      icon: 'verified_user',
      title: 'Compra segura',
      subtitle: 'Tus datos protegidos',
      href: null,
    },
    {
      icon: 'chat',
      title: 'Pedidos por WhatsApp',
      subtitle: 'Atención personalizada',
      href: whatsappNumber ? `https://wa.me/${whatsappNumber}` : null,
    },
    {
      icon: 'photo_camera',
      title: 'Síguenos en Instagram',
      subtitle: instagramUser ? `@${instagramUser}` : 'Nuestras novedades',
      href: instagramUser ? `https://www.instagram.com/${instagramUser}` : null,
    },
    ];
  });
}
