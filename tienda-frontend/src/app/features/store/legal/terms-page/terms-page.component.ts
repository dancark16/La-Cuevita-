import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LegalInfoService } from '../legal-info.service';

@Component({
  selector: 'app-terms-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './terms-page.component.html',
  styleUrl: './terms-page.component.scss',
})
export class TermsPageComponent {
  readonly info = inject(LegalInfoService);
}
