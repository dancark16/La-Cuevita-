import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { Carrier, CarriersService } from '../carriers.service';

@Component({
  selector: 'app-carrier-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './carrier-modal.component.html',
  styleUrl: './carrier-modal.component.scss',
})
export class CarrierModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CarriersService);
  private readonly notify = inject(NotificationService);

  readonly carrier = input<Carrier | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    trackingUrl: [''],
    active: [true],
  });

  ngOnInit(): void {
    const current = this.carrier();
    if (current) {
      this.form.setValue({ name: current.name, trackingUrl: current.trackingUrl ?? '', active: current.active });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const url = value.trackingUrl.trim();
    if (url && !/^https?:\/\//i.test(url)) {
      this.notify.error('El enlace de rastreo debe empezar con http:// o https://');
      return;
    }
    const request = { name: value.name.trim(), trackingUrl: url || null, active: value.active };
    const current = this.carrier();
    const call$ = current ? this.api.update(current.id, request) : this.api.create(request);
    this.saving.set(true);
    call$.subscribe({
      next: () => {
        this.notify.success(current ? 'Transportista actualizado' : 'Transportista agregado');
        this.saved.emit();
      },
      error: (error) => {
        this.saving.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}
