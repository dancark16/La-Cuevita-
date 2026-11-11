import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap } from 'rxjs';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { Color, Size } from '../catalog.models';
import { AttributesService } from '../services/attributes.service';
import { ColorFormModalComponent } from './color-form-modal/color-form-modal.component';
import { SizeFormModalComponent } from './size-form-modal/size-form-modal.component';

@Component({
  selector: 'app-attributes-page',
  standalone: true,
  imports: [SizeFormModalComponent, ColorFormModalComponent],
  templateUrl: './attributes-page.component.html',
  styleUrl: './attributes-page.component.scss',
})
export class AttributesPageComponent implements OnInit {
  private readonly api = inject(AttributesService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly sizes = signal<Size[]>([]);
  readonly colors = signal<Color[]>([]);

  readonly sizeSearch = signal('');
  readonly sizeStatus = signal('');
  readonly colorSearch = signal('');

  readonly sizeFormOpen = signal(false);
  readonly editingSize = signal<Size | null>(null);
  readonly colorFormOpen = signal(false);
  readonly editingColor = signal<Color | null>(null);

  readonly visibleSizes = computed(() => {
    const term = this.sizeSearch().trim().toLowerCase();
    const status = this.sizeStatus();
    return this.sizes().filter(
      (size) =>
        (!term || size.label.toLowerCase().includes(term)) &&
        (!status || (status === 'ACTIVE' ? size.active : !size.active))
    );
  });

  readonly visibleColors = computed(() => {
    const term = this.colorSearch().trim().toLowerCase();
    return this.colors().filter(
      (color) => !term || color.name.toLowerCase().includes(term) || color.hex.toLowerCase().includes(term)
    );
  });

  ngOnInit(): void {
    this.loadSizes();
    this.loadColors();
  }

  loadSizes(): void {
    this.api.listSizes().subscribe({
      next: (sizes) => this.sizes.set(sizes),
      error: (error) => this.notify.error(httpErrorMessage(error)),
    });
  }

  loadColors(): void {
    this.api.listColors().subscribe({
      next: (colors) => this.colors.set(colors),
      error: (error) => this.notify.error(httpErrorMessage(error)),
    });
  }

  onSizeSearch(event: Event): void {
    this.sizeSearch.set((event.target as HTMLInputElement).value);
  }

  onSizeStatus(event: Event): void {
    this.sizeStatus.set((event.target as HTMLSelectElement).value);
  }

  onColorSearch(event: Event): void {
    this.colorSearch.set((event.target as HTMLInputElement).value);
  }

  openSizeForm(size: Size | null): void {
    this.editingSize.set(size);
    this.sizeFormOpen.set(true);
  }

  openColorForm(color: Color | null): void {
    this.editingColor.set(color);
    this.colorFormOpen.set(true);
  }

  onSizeSaved(): void {
    this.sizeFormOpen.set(false);
    this.loadSizes();
  }

  onColorSaved(): void {
    this.colorFormOpen.set(false);
    this.loadColors();
  }

  removeSize(size: Size): void {
    confirmAction(this.dialog, {
      title: 'Eliminar talla',
      message: `¿Eliminar la talla "${size.label}"? Solo se puede si ninguna variante la usa. Si ya no la necesitas, mejor desactívala.`,
      confirmLabel: 'Eliminar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deleteSize(size.id))
      )
      .subscribe({
        next: () => {
          this.notify.success('Talla eliminada');
          this.loadSizes();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }

  removeColor(color: Color): void {
    confirmAction(this.dialog, {
      title: 'Eliminar color',
      message: `¿Eliminar el color "${color.name}"? Solo se puede si ninguna variante lo usa. Si ya no lo necesitas, mejor desactívalo.`,
      confirmLabel: 'Eliminar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deleteColor(color.id))
      )
      .subscribe({
        next: () => {
          this.notify.success('Color eliminado');
          this.loadColors();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }
}
