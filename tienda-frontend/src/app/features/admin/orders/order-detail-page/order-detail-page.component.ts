import { InvoiceFilesService } from '../../../../shared/services/invoice-files.service';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnDestroy, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { DomSanitizer, SafeResourceUrl, SafeUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { Observable, catchError, filter, of, switchMap, tap } from 'rxjs';
import { Order } from '../../../../shared/models/order.model';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { ORDER_STATUS_LABEL, ORDER_STEPS, orderStepIndex } from '../../../../shared/util/order-status';
import { whatsappLink } from '../../../../shared/util/whatsapp';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { Carrier, CarriersService } from '../../settings/carriers.service';
import { AdminOrdersService } from '../admin-orders.service';

type NoteAction = 'reject' | 'ship' | 'cancel';

const NOTE_COPY: Record<NoteAction, { title: string; label: string; hint: string; button: string; required: boolean }> = {
  reject: {
    title: 'Rechazar comprobante',
    label: 'Motivo (lo verá la clienta)',
    hint: 'Ej: el valor no coincide, la captura no se ve bien, la transferencia no aparece en la cuenta.',
    button: 'Rechazar comprobante',
    required: true,
  },
  ship: {
    title: 'Marcar como enviado',
    label: 'Nota para la clienta (opcional)',
    hint: 'Ej: Te llegará en 2 a 3 días hábiles.',
    button: 'Marcar como enviado',
    required: false,
  },
  cancel: {
    title: 'Cancelar pedido',
    label: 'Motivo (lo verá la clienta)',
    hint: 'Solo se pueden cancelar pedidos que aún no están pagados.',
    button: 'Cancelar pedido',
    required: true,
  },
};

@Component({
  selector: 'app-order-detail-page',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, FormsModule, ModalComponent, SelectComponent],
  templateUrl: './order-detail-page.component.html',
  styleUrl: './order-detail-page.component.scss',
})
export class OrderDetailPageComponent implements OnDestroy {
  private readonly api = inject(AdminOrdersService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly invoices = inject(InvoiceFilesService);
  readonly invoiceBusy = signal(false);

  /** Id del pedido, tomado de la ruta /admin/pedidos/:id. */
  readonly id = input.required<string>();

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly busy = signal(false);

  readonly receiptImage = signal<SafeUrl | null>(null);
  readonly receiptPdf = signal<SafeResourceUrl | null>(null);
  readonly receiptRaw = signal<string | null>(null);
  readonly receiptLoading = signal(false);

  private readonly carriersApi = inject(CarriersService);
  readonly carrierOptions = signal<SelectOption[]>([]);
  readonly carrierId = signal<string | null>(null);
  readonly trackingNumber = signal('');
  readonly eventText = signal('');
  readonly eventNotify = signal(true);
  readonly noteAction = signal<NoteAction | null>(null);

  addEvent(): void {
    const order = this.order();
    const message = this.eventText().trim();
    if (!order || !message) {
      return;
    }
    this.busy.set(true);
    this.api.addTrackingEvent(order.id, message, this.eventNotify()).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.apply(updated, false);
        this.eventText.set('');
        this.notify.success(this.eventNotify() ? 'Novedad agregada y avisada a la clienta' : 'Novedad agregada');
      },
      error: (error) => {
        this.busy.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }

  removeEvent(eventId: string): void {
    const order = this.order();
    if (!order) {
      return;
    }
    this.api.deleteTrackingEvent(order.id, eventId).subscribe({
      next: (updated) => this.apply(updated, false),
      error: (error) => this.notify.error(httpErrorMessage(error)),
    });
  }
  readonly noteText = signal('');

  readonly steps = ORDER_STEPS;
  readonly statusLabel = ORDER_STATUS_LABEL;
  readonly copy = NOTE_COPY;

  readonly stepIndex = computed(() => {
    const order = this.order();
    return order ? orderStepIndex(order.status) : -1;
  });

  readonly canConfirm = computed(() => this.order()?.status === 'PENDING');
  readonly canShip = computed(() => {
    const status = this.order()?.status;
    return status === 'PAID' || status === 'PROCESSING' || status === 'CONFIRMED';
  });

  readonly whatsapp = computed(() => {
    const order = this.order();
    if (!order) {
      return null;
    }
    const first = order.customerName.split(' ')[0] || 'hola';
    const total = `$${order.total.toFixed(2)}`;
    const note = order.adminNote ? ` ${order.adminNote}` : '';
    let text: string;
    switch (order.status) {
      case 'PENDING':
        text = order.adminNote
          ? `Hola ${first}, te escribimos de La Cuevita por tu pedido #${order.orderNumber} (${total}). Sobre tu comprobante:${note} ¿Puedes enviarlo de nuevo?`
          : `Hola ${first}, te escribimos de La Cuevita por tu pedido #${order.orderNumber} por ${total}. Cuando hagas la transferencia, envíanos el comprobante por aquí o súbelo en tu pedido.`;
        break;
      case 'PAID':
      case 'PROCESSING':
      case 'CONFIRMED':
        text = `¡Hola ${first}! Confirmamos tu pago del pedido #${order.orderNumber} por ${total}. Ya estamos preparando tu envío. Gracias por comprar en La Cuevita 💖`;
        break;
      case 'SHIPPED':
        text = `¡Hola ${first}! Tu pedido #${order.orderNumber} ya va en camino.${note}`;
        break;
      case 'DELIVERED':
        text = `¡Hola ${first}! Tu pedido #${order.orderNumber} fue entregado. Gracias por comprar en La Cuevita 💖`;
        break;
      default:
        text = `Hola ${first}, tu pedido #${order.orderNumber} fue cancelado.${note}`;
    }
    return whatsappLink(order.customerPhone, text);
  });

  /** La factura existe desde que se confirma el pago. */
  readonly hasInvoice = computed(() => {
    const status = this.order()?.status;
    return status === 'PAID' || status === 'PROCESSING' || status === 'SHIPPED' || status === 'DELIVERED' || status === 'CONFIRMED';
  });

  downloadInvoice(): void {
    const order = this.order();
    if (!order) {
      return;
    }
    this.invoiceBusy.set(true);
    this.invoices.download(order.id, order.orderNumber).subscribe({
      next: () => this.invoiceBusy.set(false),
      error: (error) => {
        this.invoiceBusy.set(false);
        this.notify.error(httpErrorMessage(error, 'No se pudo descargar la factura.'));
      },
    });
  }

  emailInvoice(): void {
    const order = this.order();
    if (!order) {
      return;
    }
    this.invoiceBusy.set(true);
    this.invoices.sendByEmail(order.id).subscribe({
      next: () => {
        this.invoiceBusy.set(false);
        this.notify.success(`Factura enviada a ${order.customerEmail}`);
      },
      error: (error) => {
        this.invoiceBusy.set(false);
        this.notify.error(httpErrorMessage(error, 'No se pudo enviar el correo.'));
      },
    });
  }

  private objectUrl: string | null = null;

  constructor() {
    toObservable(this.id)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.notFound.set(false);
        }),
        switchMap((id) => this.api.get(id).pipe(catchError(() => of(null))))
      )
      .subscribe((order) => this.apply(order, true));
  }

  ngOnDestroy(): void {
    this.revokeReceipt();
  }

  confirmPayment(): void {
    const order = this.order();
    if (!order) {
      return;
    }
    confirmAction(this.dialog, {
      title: 'Confirmar pago',
      message: `¿Ya viste en el banco la transferencia de ${order.total.toFixed(2)} dólares del pedido #${order.orderNumber}? Se descontará el stock y se generará la factura.`,
      confirmLabel: 'Sí, confirmar pago',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.run(this.api.confirmPayment(order.id)))
      )
      .subscribe();
  }

  deliver(): void {
    const order = this.order();
    if (!order) {
      return;
    }
    confirmAction(this.dialog, {
      title: 'Marcar como entregado',
      message: `¿El pedido #${order.orderNumber} ya fue entregado a la clienta?`,
      confirmLabel: 'Sí, entregado',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.run(this.api.deliver(order.id)))
      )
      .subscribe();
  }

  openNote(action: NoteAction): void {
    this.noteText.set('');
    if (action === 'ship') {
      this.trackingNumber.set('');
      this.carrierId.set(null);
      this.carriersApi.list().subscribe({
        next: (list: Carrier[]) =>
          this.carrierOptions.set(list.filter((c) => c.active).map((c) => ({ value: c.id, label: c.name }))),
        error: () => undefined,
      });
    }
    this.noteAction.set(action);
  }

  closeNote(): void {
    this.noteAction.set(null);
  }

  submitNote(): void {
    const action = this.noteAction();
    const order = this.order();
    if (!action || !order) {
      return;
    }
    const note = this.noteText().trim();
    if (NOTE_COPY[action].required && !note) {
      this.notify.error('Escribe el motivo.');
      return;
    }
    if (action === 'ship' && !this.carrierId()) {
      this.notify.error('Elige cómo se envía el pedido.');
      return;
    }
    const call$ =
      action === 'reject'
        ? this.api.rejectReceipt(order.id, note)
        : action === 'ship'
          ? this.api.ship(order.id, {
              carrierId: this.carrierId(),
              trackingNumber: this.trackingNumber().trim() || null,
              note: note || null,
            })
          : this.api.cancel(order.id, note);
    this.run(call$).subscribe(() => this.noteAction.set(null));
  }

  private run(call$: Observable<Order>): Observable<Order | null> {
    this.busy.set(true);
    return call$.pipe(
      tap((order) => {
        this.busy.set(false);
        this.apply(order, true);
        this.notify.success('Pedido actualizado');
      }),
      catchError((error) => {
        this.busy.set(false);
        this.notify.error(httpErrorMessage(error));
        return of(null);
      })
    );
  }

  private apply(order: Order | null, loadReceipt: boolean): void {
    this.loading.set(false);
    this.order.set(order);
    this.notFound.set(order === null);
    this.revokeReceipt();
    if (order?.hasReceipt && loadReceipt) {
      this.loadReceipt(order.id);
    }
  }

  private loadReceipt(id: string): void {
    this.receiptLoading.set(true);
    this.api.receipt(id).subscribe({
      next: (blob) => {
        this.objectUrl = URL.createObjectURL(blob);
        this.receiptRaw.set(this.objectUrl);
        if (blob.type === 'application/pdf') {
          this.receiptPdf.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl));
        } else {
          this.receiptImage.set(this.sanitizer.bypassSecurityTrustUrl(this.objectUrl));
        }
        this.receiptLoading.set(false);
      },
      error: () => this.receiptLoading.set(false),
    });
  }

  private revokeReceipt(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
    this.receiptImage.set(null);
    this.receiptPdf.set(null);
    this.receiptRaw.set(null);
  }
}
