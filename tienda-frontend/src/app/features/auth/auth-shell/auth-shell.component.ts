import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './auth-shell.component.html',
  styleUrl: './auth-shell.component.scss',
})
export class AuthShellComponent {
  readonly headline = input('Bienvenido a');
  readonly text = input('Aquí la moda no solo se lleva, se vive.');

  readonly image = 'assets/images/auth-bg.webp';

  readonly features = [
    { icon: 'verified_user', title: 'Compra segura', text: 'Tus datos protegidos' },
    { icon: 'local_shipping', title: 'Envíos a todo el país', text: 'Llega hasta tu ciudad' },
    { icon: 'workspace_premium', title: 'La mejor moda', text: 'Siempre contigo' },
  ];
}
