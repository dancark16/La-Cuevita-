import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { LEGAL_ROUTES } from './legal.routes';

// La app registra el español al arrancar; aquí hay que hacerlo a mano (las fechas salen en español)
registerLocaleData(localeEs, 'es');

@Component({ standalone: true, template: '<p>Inicio</p>' })
class HomeStubComponent {}

/**
 * Las páginas legales se abren desde el pie de página (navegación dentro de la app) y también
 * escribiendo la dirección. Un error ya dejó el centro en blanco solo en el primer caso:
 * estas pruebas cubren ambos recorridos.
 */
describe('Páginas legales: navegación', () => {
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: '', component: HomeStubComponent },
          { path: 'legal', children: LEGAL_ROUTES },
        ]),
      ],
    });
    harness = await RouterTestingHarness.create();
  });

  const pages: [string, string][] = [
    ['/legal/terminos', 'Términos y condiciones'],
    ['/legal/privacidad', 'Política de privacidad'],
    ['/legal/devoluciones', 'Cambios y devoluciones'],
    ['/legal/contacto', 'Contacto'],
  ];

  const title = () => harness.routeNativeElement?.querySelector('h1')?.textContent?.trim();

  for (const [url, expected] of pages) {
    it(`desde el inicio se abre ${url} con su título`, async () => {
      await harness.navigateByUrl('/');
      await harness.navigateByUrl(url);
      harness.detectChanges();

      expect(title()).toBe(expected);
      expect(harness.routeNativeElement?.querySelector('.paper')?.textContent?.trim().length).toBeGreaterThan(50);
    });

    it(`al escribir ${url} directamente se abre con su título`, async () => {
      await harness.navigateByUrl(url);
      harness.detectChanges();

      expect(title()).toBe(expected);
    });
  }

  it('al pasar de una página legal a otra cambia el título', async () => {
    await harness.navigateByUrl('/legal/terminos');
    await harness.navigateByUrl('/legal/privacidad');
    harness.detectChanges();

    expect(title()).toBe('Política de privacidad');
  });

  it('/legal abre los términos y condiciones', async () => {
    await harness.navigateByUrl('/');
    await harness.navigateByUrl('/legal');
    harness.detectChanges();

    expect(title()).toBe('Términos y condiciones');
  });
});
