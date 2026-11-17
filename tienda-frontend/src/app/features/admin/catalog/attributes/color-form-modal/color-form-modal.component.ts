import { colorTones } from '../../../../../shared/util/color';
import { ColorBackgroundPipe } from '../../../../../shared/util/color-background.pipe';
import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalComponent } from '../../../../../shared/components/modal/modal.component';
import { NotificationService } from '../../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../../shared/util/http-error';
import { Color } from '../../catalog.models';
import { AttributesService } from '../../services/attributes.service';

const HEX_PATTERN = /^#[0-9A-Fa-f]{6}$/;

@Component({
  selector: 'app-color-form-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent, ColorBackgroundPipe],
  templateUrl: './color-form-modal.component.html',
  styleUrl: './color-form-modal.component.scss',
})
export class ColorFormModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AttributesService);
  private readonly notify = inject(NotificationService);

  readonly color = input<Color | null>(null);

  readonly saved = output<void>();
  readonly cancelled = output<void>();

  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(50)]],
    hex: ['#000000', [Validators.required, Validators.pattern(HEX_PATTERN)]],
    combine: [false],
    hex2: ['#FFFFFF', [Validators.pattern(HEX_PATTERN)]],
    active: [true],
  });

  ngOnInit(): void {
    const current = this.color();
    if (current) {
      const [first, second] = colorTones(current.hex);
      this.form.setValue({
        name: current.name,
        hex: first ?? '#000000',
        combine: !!second,
        hex2: second ?? '#FFFFFF',
        active: current.active,
      });
    }
  }

  setHex(value: string): void {
    this.form.controls.hex.setValue(value.toUpperCase());
  }

  setHex2(value: string): void {
    this.form.controls.hex2.setValue(value.toUpperCase());
  }

  validPreview2(): string {
    const hex = this.form.controls.hex2.value;
    return HEX_PATTERN.test(hex) ? hex : '#FFFFFF';
  }

  /** Valor que se guarda: un tono, o dos separados por «/». */
  get combined(): string {
    const { hex, hex2, combine } = this.form.getRawValue();
    return combine && HEX_PATTERN.test(hex2) ? `${hex.toUpperCase()}/${hex2.toUpperCase()}` : hex.toUpperCase();
  }

  validPreview(): string {
    const hex = this.form.controls.hex.value;
    return HEX_PATTERN.test(hex) ? hex : '#000000';
  }

  save(): void {
    if (this.form.invalid || (this.form.controls.combine.value && this.form.controls.hex2.invalid)) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request = { name: value.name.trim(), hex: this.combined, active: value.active };
    const current = this.color();
    const call$ = current ? this.api.updateColor(current.id, request) : this.api.createColor(request);

    this.saving.set(true);
    call$.subscribe({
      next: () => {
        this.notify.success(current ? 'Color actualizado' : 'Color creado');
        this.saved.emit();
      },
      error: (error) => {
        this.saving.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }
}
