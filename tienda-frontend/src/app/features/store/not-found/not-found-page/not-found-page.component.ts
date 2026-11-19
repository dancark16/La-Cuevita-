import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { StoreApiService } from '../../data/store-api.service';
import { StoreCategory } from '../../data/store.models';

@Component({
  selector: 'app-not-found-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './not-found-page.component.html',
  styleUrl: './not-found-page.component.scss',
})
export class NotFoundPageComponent {
  private readonly router = inject(Router);
  readonly categories = signal<StoreCategory[]>([]);

  constructor() {
    inject(StoreApiService)
      .rootCategories()
      .subscribe((categories) => this.categories.set(categories));
  }

  search(event: Event, term: string): void {
    event.preventDefault();
    this.router.navigate(['/tienda'], { queryParams: { q: term.trim() || null } });
  }
}
