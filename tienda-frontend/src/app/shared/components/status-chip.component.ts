import { Component, Input } from '@angular/core';

interface ChipStyle {
  label: string;
  classes: string;
}

const STATUS_STYLES: Record<string, ChipStyle> = {
  ACTIVE: { label: 'Activo', classes: 'bg-green-100 text-green-800' },
  INACTIVE: { label: 'Inactivo', classes: 'bg-gray-200 text-gray-700' },
  DRAFT: { label: 'Borrador', classes: 'bg-amber-100 text-amber-800' },
  DISCONTINUED: { label: 'Descontinuado', classes: 'bg-red-100 text-red-800' },
};

const FALLBACK_STYLE: ChipStyle = { label: '', classes: 'bg-gray-200 text-gray-700' };

@Component({
  selector: 'app-status-chip',
  standalone: true,
  template: `
    <span
      class="inline-block rounded-full px-2.5 py-0.5 text-xs font-medium"
      [class]="style.classes"
      >{{ style.label || status }}</span
    >
  `,
})
export class StatusChipComponent {
  @Input({ required: true }) status!: string;

  get style(): ChipStyle {
    return STATUS_STYLES[this.status] ?? FALLBACK_STYLE;
  }
}
