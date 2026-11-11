import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { ImportAction, ImportResult } from '../import.models';
import { ImportService } from '../import.service';

type Stage = 'idle' | 'loading' | 'preview' | 'importing' | 'done';
type ItemFilter = 'ALL' | ImportAction;

const ACCEPTED = /\.(xlsx|xls)$/i;
const MAX_BYTES = 10 * 1024 * 1024;

@Component({
  selector: 'app-import-page',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './import-page.component.html',
  styleUrl: './import-page.component.scss',
})
export class ImportPageComponent {
  private readonly service = inject(ImportService);

  readonly stage = signal<Stage>('idle');
  readonly file = signal<File | null>(null);
  readonly result = signal<ImportResult | null>(null);
  readonly error = signal<string | null>(null);
  readonly dragging = signal(false);
  readonly filter = signal<ItemFilter>('ALL');

  readonly errors = computed(() => this.result()?.issues.filter((i) => i.level === 'ERROR') ?? []);
  readonly warnings = computed(() => this.result()?.issues.filter((i) => i.level === 'WARNING') ?? []);
  readonly visibleItems = computed(() => {
    const items = this.result()?.items ?? [];
    const filter = this.filter();
    return filter === 'ALL' ? items : items.filter((item) => item.action === filter);
  });
  readonly importable = computed(() => (this.result()?.items.length ?? 0) > 0);
  readonly busy = computed(() => this.stage() === 'loading' || this.stage() === 'importing');

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.select(file);
    }
  }

  onFileChosen(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      this.select(file);
    }
  }

  confirm(): void {
    const file = this.file();
    if (!file || !this.importable()) {
      return;
    }
    this.stage.set('importing');
    this.error.set(null);
    this.service.apply(file).subscribe({
      next: (result) => {
        this.result.set(result);
        this.stage.set('done');
      },
      error: (error) => {
        this.error.set(httpErrorMessage(error, 'No se pudo importar el archivo.'));
        this.stage.set('preview');
      },
    });
  }

  reset(): void {
    this.stage.set('idle');
    this.file.set(null);
    this.result.set(null);
    this.error.set(null);
    this.filter.set('ALL');
  }

  setFilter(filter: ItemFilter): void {
    this.filter.set(filter);
  }

  private select(file: File): void {
    if (!ACCEPTED.test(file.name)) {
      this.error.set('Solo se aceptan archivos Excel (.xlsx o .xls).');
      return;
    }
    if (file.size > MAX_BYTES) {
      this.error.set('El archivo supera los 10 MB.');
      return;
    }
    this.file.set(file);
    this.result.set(null);
    this.error.set(null);
    this.filter.set('ALL');
    this.stage.set('loading');
    this.service.preview(file).subscribe({
      next: (result) => {
        this.result.set(result);
        this.stage.set('preview');
      },
      error: (error) => {
        this.error.set(httpErrorMessage(error, 'No se pudo leer el archivo.'));
        this.file.set(null);
        this.stage.set('idle');
      },
    });
  }
}
