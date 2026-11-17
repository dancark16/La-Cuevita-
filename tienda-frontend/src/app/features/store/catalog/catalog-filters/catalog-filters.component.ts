import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { StoreSort } from '../../data/store.models';

@Component({
  selector: 'app-catalog-filters',
  standalone: true,
  imports: [SelectComponent],
  templateUrl: './catalog-filters.component.html',
  styleUrl: './catalog-filters.component.scss',
})
export class CatalogFiltersComponent {
  @Input() searchTerm = '';
  @Input() sort: StoreSort = 'NEWEST';

  @Output() searchSubmitted = new EventEmitter<string>();
  readonly sortOptions: SelectOption[] = [
    { value: 'NEWEST', label: 'Más nuevos' },
    { value: 'NAME', label: 'Nombre (A-Z)' },
  ];

  @Output() sortChanged = new EventEmitter<StoreSort>();

  onSearch(event: Event, value: string): void {
    event.preventDefault();
    this.searchSubmitted.emit(value);
  }

  onSortChange(value: string): void {
    this.sortChanged.emit(value === 'NAME' ? 'NAME' : 'NEWEST');
  }
}
