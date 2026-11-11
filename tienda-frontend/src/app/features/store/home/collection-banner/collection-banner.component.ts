import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-collection-banner',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './collection-banner.component.html',
  styleUrl: './collection-banner.component.scss',
})
export class CollectionBannerComponent {
  readonly image = 'assets/images/banner-colecciones.webp';
}
