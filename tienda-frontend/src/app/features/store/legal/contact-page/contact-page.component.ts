import { Component, inject } from '@angular/core';
import { LegalInfoService } from '../legal-info.service';

@Component({
  selector: 'app-contact-page',
  standalone: true,
  templateUrl: './contact-page.component.html',
  styleUrl: './contact-page.component.scss',
})
export class ContactPageComponent {
  readonly info = inject(LegalInfoService);
}
