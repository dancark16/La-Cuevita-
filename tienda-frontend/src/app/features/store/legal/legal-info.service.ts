import { Injectable, computed, inject } from '@angular/core';
import { StoreSettingsService } from '../../../core/settings/store-settings.service';
import { whatsappLink } from '../../../shared/util/whatsapp';

/**
 * Reglas de la tienda que aparecen en las páginas legales.
 * Si el negocio cambia una política, se ajusta aquí y se actualiza en todos los textos.
 */
export const POLICY = {
  /** Días hábiles, desde que recibes el pedido, para pedir un cambio. */
  changeDays: 5,
  /** Horas, desde que recibes el pedido, para avisar de un defecto o un error de envío. */
  defectHours: 48,
  /** Días hábiles para devolver el dinero una vez aprobada la devolución. */
  refundDays: 5,
  /** Horas que se aparta el stock de un pedido sin pagar (igual a ORDER_RESERVATION_HOURS del servidor). */
  reservationHours: 48,
  /** Fecha de la última revisión de los textos. */
  updated: new Date(2026, 10, 19),
};

/** Datos del negocio tomados de Configuración, listos para mostrar en los textos legales. */
@Injectable({ providedIn: 'root' })
export class LegalInfoService {
  private readonly settings = inject(StoreSettingsService).settings;

  readonly policy = POLICY;

  readonly name = computed(() => this.settings().tradeName || 'La Cuevita');
  readonly legalName = computed(() => this.settings().businessName);
  readonly ruc = computed(() => this.settings().ruc);
  readonly address = computed(() => this.settings().headOfficeAddress);
  readonly email = computed(() => this.settings().contactEmail);

  readonly whatsappNumber = computed(() => this.settings().whatsappNumber);
  readonly whatsappLabel = computed(() => {
    const number = this.settings().whatsappNumber ?? '';
    return number.startsWith('593') ? `+${number.slice(0, 3)} ${number.slice(3)}` : number;
  });

  readonly instagram = computed(() => this.settings().instagramUser);
  readonly instagramUrl = computed(() => (this.instagram() ? `https://www.instagram.com/${this.instagram()}` : null));

  /** Enlace de WhatsApp con un mensaje ya escrito; null si la tienda aún no tiene número. */
  whatsapp(message: string): string | null {
    return whatsappLink(this.settings().whatsappNumber, message);
  }
}
