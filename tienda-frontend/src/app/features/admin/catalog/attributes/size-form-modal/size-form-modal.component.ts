import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalComponent } from '../../../../../shared/components/modal/modal.component';
import { NotificationService } from '../../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../../shared/util/http-error';
import { Size } from '../../catalog.models';
import { AttributesService } from '../../services/attributes.service';

@Component({
  selector: 'app-size-form-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './size-form-modal.component.html',
  styleUrl: './size-form-modal.component.scss',
})
export class SizeFormModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AttributesService);
  private readonly notify = inject(NotificationService);

  readonly size = input<Size | null>(null);

  readonly saved = output<void>();
  readonly cancelled = output<void>();

  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    label: ['', [Validators.required, Validators.maxLength(10)]],
    active: [true],
  });

  ngOnInit(): void {
    const current = this.size();
    if (current) {
      this.form.setValue({ label: current.label, active: current.active });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request = { label: value.label.trim(), active: value.active };
    const current = this.size();
    const call$ = current ? this.api.updateSize(current.id, request) : this.api.createSize(request);

    this.saving.set(true);
    call$.subscribe({
      next: () => {
        this.notify.success(current ? 'Talla actualizada' : 'Talla creada');
        this.saved.emit();
      },
      error: (error) => {
        this.saving.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}
