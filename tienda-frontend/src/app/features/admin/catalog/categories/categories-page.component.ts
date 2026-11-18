import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap } from 'rxjs';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { DashboardService } from '../../dashboard/dashboard.service';
import { Category } from '../catalog.models';
import { CategoriesService } from '../services/categories.service';
import { CategoryFormModalComponent } from './category-form-modal/category-form-modal.component';

const FILTERS_KEY = 'admin.categories.filters';

function readSavedFilters(): { category: string; parent: string; status: string; saved: boolean } {
  try {
    const raw = sessionStorage.getItem(FILTERS_KEY);
    if (raw) {
      const value = JSON.parse(raw);
      return { category: value.category ?? '', parent: value.parent ?? '', status: value.status ?? '', saved: true };
    }
  } catch {
    // se ignora
  }
  return { category: '', parent: '', status: '', saved: false };
}

@Component({
  selector: 'app-categories-page',
  standalone: true,
  imports: [SelectComponent, CategoryFormModalComponent],
  templateUrl: './categories-page.component.html',
  styleUrl: './categories-page.component.scss',
})
export class CategoriesPageComponent implements OnInit {
  private readonly api = inject(CategoriesService);
  private readonly dashboard = inject(DashboardService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  private readonly savedFilters = readSavedFilters();
  private readonly firstVisit = !this.savedFilters.saved;
  private started = false;

  constructor() {
    // Recuerda los filtros para volver exactamente donde estabas.
    effect(() => {
      try {
        sessionStorage.setItem(
          FILTERS_KEY,
          JSON.stringify({ category: this.categoryFilter(), parent: this.parentFilter(), status: this.statusFilter() })
        );
      } catch {
        // sin almacenamiento: queda solo en memoria
      }
    });
  }

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly search = signal('');
  /** '' = todas, o el id de una categoría concreta. */
  readonly categoryFilter = signal(this.savedFilters.category);
  /** '' = todos, 'ROOT' = sin padre, o el id de una categoría padre. */
  readonly parentFilter = signal(this.savedFilters.parent);
  /** '' = todos, 'ACTIVE' o 'INACTIVE'. */
  readonly statusFilter = signal(this.savedFilters.status);
  readonly counts = signal<Map<string, number>>(new Map());
  readonly editing = signal<Category | null>(null);
  readonly formOpen = signal(false);

  private readonly namesById = computed(
    () => new Map(this.categories().map((category) => [category.id, category.name] as const))
  );

  readonly parents = computed(() => {
    const parentIds = new Set(this.categories().map((c) => c.parentId).filter((id): id is string => !!id));
    return this.categories().filter((c) => parentIds.has(c.id));
  });

  readonly categoryOptions = computed<SelectOption[]>(() => [
    { value: '', label: 'Todas las categorías' },
    ...this.categories().map((c) => ({ value: c.id, label: c.name })),
  ]);

  readonly parentOptions = computed<SelectOption[]>(() => [
    { value: '', label: 'Todos los padres' },
    { value: 'ROOT', label: 'Sin padre (principales)' },
    ...this.parents().map((c) => ({ value: c.id, label: c.name })),
  ]);

  readonly statusOptions: SelectOption[] = [
    { value: '', label: 'Todos los estados' },
    { value: 'ACTIVE', label: 'Activas' },
    { value: 'INACTIVE', label: 'Inactivas' },
  ];

  readonly visible = computed(() => {
    const term = this.search().trim().toLowerCase();
    const categoryId = this.categoryFilter();
    const parent = this.parentFilter();
    const status = this.statusFilter();
    return this.categories().filter((c) => {
      if (term && !c.name.toLowerCase().includes(term) && !c.slug.includes(term)) {
        return false;
      }
      if (categoryId && c.id !== categoryId) {
        return false;
      }
      if (parent === 'ROOT' && c.parentId !== null) {
        return false;
      }
      if (parent && parent !== 'ROOT' && c.parentId !== parent) {
        return false;
      }
      if (status === 'ACTIVE' && !c.active) {
        return false;
      }
      if (status === 'INACTIVE' && c.active) {
        return false;
      }
      return true;
    }).sort((a, b) => a.name.localeCompare(b.name, 'es'));
  });

  ngOnInit(): void {
    this.load();
  }

  parentName(parentId: string | null): string | null {
    return parentId ? (this.namesById().get(parentId) ?? null) : null;
  }

  productCount(id: string): number {
    return this.counts().get(id) ?? 0;
  }

  onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  onCategory(event: Event): void {
    this.categoryFilter.set((event.target as HTMLSelectElement).value);
  }

  onParent(event: Event): void {
    this.parentFilter.set((event.target as HTMLSelectElement).value);
  }

  onStatus(event: Event): void {
    this.statusFilter.set((event.target as HTMLSelectElement).value);
  }

  load(): void {
    this.loading.set(true);
    this.dashboard.catalog().subscribe({
      next: (stats) => {
        this.counts.set(new Map(stats.categories.map((c) => [c.id, c.products] as const)));
        // Primera vez: arranca en la primera categoría (A-Z) que tenga productos, no en «Todas».
        if (this.firstVisit && !this.started) {
          const first = [...stats.categories]
            .filter((category) => category.products > 0)
            .sort((a, b) => a.name.localeCompare(b.name, 'es'))[0];
          if (first) {
            this.categoryFilter.set(first.id);
          }
        }
        this.started = true;
      },
      error: () => undefined,
    });
    this.api.list().subscribe({
      next: (categories) => {
        this.categories.set([...categories].sort((a, b) => a.name.localeCompare(b.name, 'es')));
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.notify.error(httpErrorMessage(error));
      },
    });
  }

  openForm(category: Category | null): void {
    this.editing.set(category);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
  }

  onSaved(): void {
    this.formOpen.set(false);
    this.load();
  }

  remove(category: Category): void {
    confirmAction(this.dialog, {
      title: 'Eliminar categoría',
      message: `¿Eliminar "${category.name}"? Solo se puede si no tiene productos. Esta acción no se puede deshacer.`,
      confirmLabel: 'Eliminar',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.delete(category.id))
      )
      .subscribe({
        next: () => {
          this.notify.success('Categoría eliminada');
          this.load();
        },
        error: (error) => this.notify.error(httpErrorMessage(error)),
      });
  }
}
