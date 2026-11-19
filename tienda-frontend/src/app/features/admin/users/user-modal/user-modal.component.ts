import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalComponent } from '../../../../shared/components/modal/modal.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { AdminUsersService } from '../admin-users.service';

@Component({
  selector: 'app-user-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent, SelectComponent],
  templateUrl: './user-modal.component.html',
  styleUrl: './user-modal.component.scss',
})
export class UserModalComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AdminUsersService);
  private readonly notify = inject(NotificationService);

  readonly saved = output<void>();
  readonly cancelled = output<void>();

  readonly saving = signal(false);
  readonly showPassword = signal(false);

  readonly roleOptions: SelectOption[] = [
    { value: 'ADMIN', label: 'Administrador: accede al panel' },
    { value: 'CUSTOMER', label: 'Cliente: solo compra en la tienda' },
  ];

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(100)]],
    role: ['ADMIN' as 'ADMIN' | 'CUSTOMER', Validators.required],
  });

  generate(): void {
    const chars = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789';
    const bytes = crypto.getRandomValues(new Uint32Array(14));
    this.form.controls.password.setValue(Array.from(bytes, (n) => chars[n % chars.length]).join(''));
    this.showPassword.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.api.create({ email: value.email.trim(), password: value.password, role: value.role }).subscribe({
      next: () => {
        this.notify.success('Cuenta creada. Compártele su contraseña por un medio seguro.');
        this.saved.emit();
      },
      error: (error) => {
        this.saving.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}
