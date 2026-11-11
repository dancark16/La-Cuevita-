import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalComponent } from '../../../../../shared/components/modal/modal.component';
import { NotificationService } from '../../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../../shared/util/http-error';
import { SLUG_PATTERN, slugify } from '../../../../../shared/util/slug';
import { Category, CategoryRequest } from '../../catalog.models';
import { CategoriesService } from '../../services/categories.service';

const URL_PATTERN = /^https?:\/\/\S+$/i;

@Component({
  selector: 'app-category-form-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './category-form-modal.component.html',
  styleUrl: './category-form-modal.component.scss',
})
export class CategoryFormModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CategoriesService);
  private readonly notify = inject(NotificationService);

  readonly category = input<Category | null>(null);
  readonly categories = input<Category[]>([]);

  readonly saved = output<void>();
  readonly cancelled = output<void>();

  readonly saving = signal(false);
  parentOptions: Category[] = [];

  readonly form = this.fb.group({
    name: this.fb.nonNullable.control('', Validators.required),
    slug: this.fb.nonNullable.control('', [Validators.required, Validators.pattern(SLUG_PATTERN)]),
    parentId: this.fb.control<string | null>(null),
    imageUrl: this.fb.nonNullable.control('', Validators.pattern(URL_PATTERN)),
    active: this.fb.nonNullable.control(true),
  });

  ngOnInit(): void {
    const current = this.category();
    this.parentOptions = this.buildParentOptions(current);

    if (current) {
      this.form.setValue({
        name: current.name,
        slug: current.slug,
        parentId: current.parentId,
        imageUrl: current.imageUrl ?? '',
        active: current.active,
      });
    } else {
      this.form.controls.name.valueChanges.subscribe((name) => {
        if (!this.form.controls.slug.dirty) {
          this.form.controls.slug.setValue(slugify(name));
        }
      });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request: CategoryRequest = {
      name: value.name.trim(),
      slug: value.slug,
      parentId: value.parentId,
      imageUrl: value.imageUrl.trim() || null,
      active: value.active,
    };
    const current = this.category();
    const call$ = current ? this.api.update(current.id, request) : this.api.create(request);

    this.saving.set(true);
    call$.subscribe({
      next: () => {
        this.notify.success(current ? 'Categoría actualizada' : 'Categoría creada');
        this.saved.emit();
      },
      error: (error) => {
        this.saving.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }

  invalid(name: 'name' | 'slug' | 'imageUrl'): boolean {
    const control = this.form.controls[name];
    return control.touched && control.invalid;
  }

  private buildParentOptions(current: Category | null): Category[] {
    const all = this.categories();
    if (!current) {
      return all;
    }
    const excluded = new Set<string>([current.id]);
    const pending = [current.id];
    while (pending.length > 0) {
      const parentId = pending.pop() as string;
      for (const category of all) {
        if (category.parentId === parentId && !excluded.has(category.id)) {
          excluded.add(category.id);
          pending.push(category.id);
        }
      }
    }
    return all.filter((category) => !excluded.has(category.id));
  }
}
