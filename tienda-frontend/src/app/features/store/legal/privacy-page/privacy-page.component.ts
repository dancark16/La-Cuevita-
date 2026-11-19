import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LegalInfoService } from '../legal-info.service';

@Component({
  selector: 'app-privacy-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './privacy-page.component.html',
  styleUrl: './privacy-page.component.scss',
})
export class PrivacyPageComponent {
  readonly info = inject(LegalInfoService);
}
