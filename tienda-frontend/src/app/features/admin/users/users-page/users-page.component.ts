import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { AdminUser, AdminUsersService } from '../admin-users.service';
import { UserModalComponent } from '../user-modal/user-modal.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, SelectComponent, UserModalComponent],
  templateUrl: './users-page.component.html',
  styleUrl: './users-page.component.scss',
})
export class UsersPageComponent implements OnInit {
  private readonly api = inject(AdminUsersService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly rows = signal<AdminUser[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly loading = signal(true);
  readonly modalOpen = signal(false);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly roleControl = new FormControl<string | null>(null);

  readonly roleOptions: SelectOption[] = [
    { value: null, label: 'Todos los tipos' },
    { value: 'ADMIN', label: 'Administradores' },
    { value: 'CUSTOMER', label: 'Clientes' },
  ];

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / PAGE_SIZE)));
  readonly from = computed(() => (this.total() === 0 ? 0 : this.pageIndex() * PAGE_SIZE + 1));
  readonly to = computed(() => Math.min(this.total(), (this.pageIndex() + 1) * PAGE_SIZE));

  constructor() {
    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.resetAndLoad());
    this.roleControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.resetAndLoad());
  }

  ngOnInit(): void {
    this.load();
  }

  isMe(user: AdminUser): boolean {
    return user.email.toLowerCase() === (this.auth.user()?.email ?? '').toLowerCase();
  }

  goTo(page: number): void {
    this.pageIndex.set(page);
    this.load();
  }

  onSaved(): void {
    this.modalOpen.set(false);
    this.load();
  }

  toggleRole(user: AdminUser): void {
    const makeAdmin = user.role !== 'ADMIN';
    confirmAction(this.dialog, {
      title: makeAdmin ? 'Dar acceso al panel' : 'Quitar acceso al panel',
      message: makeAdmin
        ? `${user.email} podrá ver y cambiar productos, pedidos, configuración y cuentas.`
        : `${user.email} dejará de poder entrar al panel de administración. Podrá seguir comprando en la tienda.`,
      confirmLabel: makeAdmin ? 'Sí, dar acceso' : 'Quitar acceso',
      tone: makeAdmin ? 'default' : 'danger',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.update(user.id, { role: makeAdmin ? 'ADMIN' : 'CUSTOMER' }))
      )
      .subscribe({
        next: () => {
          this.notify.success('Cuenta actualizada');
          this.load();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }

  toggleEnabled(user: AdminUser): void {
    const disable = user.enabled;
    confirmAction(this.dialog, {
      title: disable ? 'Desactivar cuenta' : 'Activar cuenta',
      message: disable
        ? `${user.email} no podrá ingresar y se cerrarán sus sesiones. Sus pedidos se conservan.`
        : `${user.email} podrá volver a ingresar.`,
      confirmLabel: disable ? 'Desactivar' : 'Activar',
      tone: disable ? 'danger' : 'default',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.update(user.id, { enabled: !disable }))
      )
      .subscribe({
        next: () => {
          this.notify.success(disable ? 'Cuenta desactivada' : 'Cuenta activada');
          this.load();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }

  sendLink(user: AdminUser): void {
    this.api.sendResetLink(user.id).subscribe({
      next: () => this.notify.success(`Enlace enviado a ${user.email}`),
      error: (error) => this.notify.error(httpErrorMessage(error)),
    });
  }

  private resetAndLoad(): void {
    this.pageIndex.set(0);
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api.search(this.searchControl.value, this.roleControl.value, this.pageIndex(), PAGE_SIZE).subscribe({
      next: (page) => {
        this.rows.set(page.content);
        this.total.set(page.totalElements);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}
