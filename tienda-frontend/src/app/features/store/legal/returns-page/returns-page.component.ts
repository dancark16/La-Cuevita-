import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LegalInfoService } from '../legal-info.service';

@Component({
  selector: 'app-returns-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './returns-page.component.html',
  styleUrl: './returns-page.component.scss',
})
export class ReturnsPageComponent {
  readonly info = inject(LegalInfoService);
}
