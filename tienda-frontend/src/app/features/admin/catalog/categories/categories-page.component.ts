import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap } from 'rxjs';
import { confirmAction } from '../../../../shared/components/confirm-dialog.component';
import { NotificationService } from '../../../../shared/services/notification.service';
import { httpErrorMessage } from '../../../../shared/util/http-error';
import { DashboardService } from '../../dashboard/dashboard.service';
import { Category } from '../catalog.models';
import { CategoriesService } from '../services/categories.service';
import { CategoryFormModalComponent } from './category-form-modal/category-form-modal.component';

@Component({
  selector: 'app-categories-page',
  standalone: true,
  imports: [CategoryFormModalComponent],
  templateUrl: './categories-page.component.html',
  styleUrl: './categories-page.component.scss',
})
export class CategoriesPageComponent implements OnInit {
  private readonly api = inject(CategoriesService);
  private readonly dashboard = inject(DashboardService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly search = signal('');
  /** '' = todas, o el id de una categoría concreta. */
  readonly categoryFilter = signal('');
  /** '' = todos, 'ROOT' = sin padre, o el id de una categoría padre. */
  readonly parentFilter = signal('');
  /** '' = todos, 'ACTIVE' o 'INACTIVE'. */
  readonly statusFilter = signal('');
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
    });
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
      next: (stats) => this.counts.set(new Map(stats.categories.map((c) => [c.id, c.products] as const))),
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
