import {
  Component,
  ElementRef,
  HostListener,
  computed,
  forwardRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SelectOption {
  value: unknown;
  label: string;
}

let nextId = 0;

/**
 * Lista desplegable con el estilo del sistema. Reemplaza al <select> nativo, cuya lista
 * no se puede diseñar. Sirve con formularios (formControl / formControlName) o con
 * [value] y (changed). Si hay muchas opciones muestra un buscador.
 */
@Component({
  selector: 'app-select',
  standalone: true,
  templateUrl: './select.component.html',
  styleUrl: './select.component.scss',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectComponent), multi: true }],
})
export class SelectComponent implements ControlValueAccessor {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly options = input<SelectOption[]>([]);
  readonly placeholder = input('Seleccionar');
  readonly ariaLabel = input<string | null>(null);
  readonly inputId = input<string | null>(null);
  readonly invalid = input(false);
  /** 'filter' = compacto para barras de filtros; 'field' = ancho completo en formularios. */
  readonly variant = input<'filter' | 'field'>('field');
  /** Muestra buscador cuando hay al menos tantas opciones. */
  readonly searchFrom = input(9);

  readonly value = input<unknown>(undefined, { alias: 'value' });
  readonly changed = output<unknown>();

  readonly uid = `app-select-${nextId++}`;
  readonly open = signal(false);
  readonly query = signal('');
  readonly active = signal(0);
  readonly panelStyle = signal<Record<string, string>>({});

  private readonly internal = signal<unknown>(undefined);
  private readonly fromForm = signal(false);
  readonly disabled = signal(false);

  private readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  private readonly trigger = viewChild<ElementRef<HTMLButtonElement>>('trigger');

  private onChange: (value: unknown) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  readonly current = computed(() => (this.fromForm() ? this.internal() : (this.value() ?? this.internal())));

  readonly selected = computed(() => this.options().find((option) => this.same(option.value, this.current())) ?? null);

  readonly searchable = computed(() => this.options().length >= this.searchFrom());

  readonly filtered = computed(() => {
    const term = this.normalize(this.query());
    return term ? this.options().filter((option) => this.normalize(option.label).includes(term)) : this.options();
  });

  writeValue(value: unknown): void {
    this.fromForm.set(true);
    this.internal.set(value);
  }

  registerOnChange(fn: (value: unknown) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled.set(disabled);
  }

  toggle(): void {
    if (this.disabled()) {
      return;
    }
    this.open() ? this.close() : this.openPanel();
  }

  choose(option: SelectOption): void {
    this.internal.set(option.value);
    this.onChange(option.value);
    this.changed.emit(option.value);
    this.close();
    this.trigger()?.nativeElement.focus();
  }

  isSelected(option: SelectOption): boolean {
    return this.same(option.value, this.current());
  }

  onSearch(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
    this.active.set(0);
  }

  onKeydown(event: KeyboardEvent): void {
    const list = this.filtered();
    if (!this.open()) {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp' || event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        this.openPanel();
      }
      return;
    }
    switch (event.key) {
      case 'Escape':
        event.preventDefault();
        this.close();
        this.trigger()?.nativeElement.focus();
        break;
      case 'ArrowDown':
        event.preventDefault();
        this.active.update((index) => Math.min(index + 1, list.length - 1));
        this.scrollToActive();
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.active.update((index) => Math.max(index - 1, 0));
        this.scrollToActive();
        break;
      case 'Enter':
        event.preventDefault();
        if (list[this.active()]) {
          this.choose(list[this.active()]);
        }
        break;
      case 'Tab':
        this.close();
        break;
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  @HostListener('window:resize')
  onResize(): void {
    this.close();
  }

  @HostListener('window:scroll', ['$event'])
  onScroll(event: Event): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  private openPanel(): void {
    const button = this.trigger()?.nativeElement;
    if (button) {
      const rect = button.getBoundingClientRect();
      const width = Math.max(rect.width, 224);
      const spaceBelow = window.innerHeight - rect.bottom;
      const openUp = spaceBelow < 300 && rect.top > spaceBelow;
      const left = Math.min(rect.left, window.innerWidth - width - 8);
      this.panelStyle.set({
        left: `${Math.max(8, left)}px`,
        width: `${width}px`,
        ...(openUp
          ? { bottom: `${window.innerHeight - rect.top + 6}px`, 'max-height': `${Math.min(340, rect.top - 16)}px` }
          : { top: `${rect.bottom + 6}px`, 'max-height': `${Math.min(340, spaceBelow - 16)}px` }),
      });
    }
    this.query.set('');
    const index = this.options().findIndex((option) => this.same(option.value, this.current()));
    this.active.set(Math.max(index, 0));
    this.open.set(true);
    setTimeout(() => {
      this.searchInput()?.nativeElement.focus();
      this.scrollToActive();
    });
  }

  private close(): void {
    if (this.open()) {
      this.open.set(false);
      this.onTouched();
    }
  }

  private scrollToActive(): void {
    setTimeout(() =>
      this.host.nativeElement.querySelector('.option--active')?.scrollIntoView({ block: 'nearest' })
    );
  }

  private same(a: unknown, b: unknown): boolean {
    return (a ?? '') === (b ?? '');
  }

  private normalize(text: string): string {
    return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
  }
}
