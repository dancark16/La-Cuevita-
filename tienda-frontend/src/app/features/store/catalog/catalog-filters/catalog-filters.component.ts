import { Component, EventEmitter, Input, Output } from '@angular/core';
import { StoreSort } from '../../data/store.models';

@Component({
  selector: 'app-catalog-filters',
  standalone: true,
  templateUrl: './catalog-filters.component.html',
  styleUrl: './catalog-filters.component.scss',
})
export class CatalogFiltersComponent {
  @Input() searchTerm = '';
  @Input() sort: StoreSort = 'NEWEST';

  @Output() searchSubmitted = new EventEmitter<string>();
  @Output() sortChanged = new EventEmitter<StoreSort>();

  onSearch(event: Event, value: string): void {
    event.preventDefault();
    this.searchSubmitted.emit(value);
  }

  onSortChange(value: string): void {
    this.sortChanged.emit(value === 'NAME' ? 'NAME' : 'NEWEST');
  }
}
