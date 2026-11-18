import { Component, inject } from '@angular/core';
import { NotificationService, ToastTone } from '../../services/notification.service';

const ICONS: Record<ToastTone, string> = {
  success: 'check_circle',
  error: 'error_outline',
  info: 'info',
  warning: 'warning_amber',
};

const TITLES: Record<ToastTone, string> = {
  success: 'Listo',
  error: 'No se pudo',
  info: 'Aviso',
  warning: 'Atención',
};

@Component({
  selector: 'app-toast-host',
  standalone: true,
  templateUrl: './toast-host.component.html',
  styleUrl: './toast-host.component.scss',
})
export class ToastHostComponent {
  readonly notifications = inject(NotificationService);

  iconOf(tone: ToastTone): string {
    return ICONS[tone];
  }

  titleOf(tone: ToastTone): string {
    return TITLES[tone];
  }
}
