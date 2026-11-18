import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ModalComponent } from './modal/modal.component';
import { Observable, map } from 'rxjs';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  /** 'danger' para acciones destructivas; si no se indica se deduce del texto del botón. */
  tone?: 'danger' | 'default';
}

const DESTRUCTIVE = /^(eliminar|quitar|borrar|desactivar|cancelar pedido|rechazar)/i;

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, ModalComponent],
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss',
})
export class ConfirmDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<ConfirmDialogComponent, boolean>);
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);

  readonly danger = (this.data.tone ?? (DESTRUCTIVE.test(this.data.confirmLabel ?? '') ? 'danger' : 'default')) === 'danger';

  close(result: boolean): void {
    this.dialogRef.close(result);
  }
}

export function confirmAction(dialog: MatDialog, data: ConfirmDialogData): Observable<boolean> {
  return dialog
    .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data,
      width: '440px',
      maxWidth: '92vw',
    })
    .afterClosed()
    .pipe(map((result) => result === true));
}
