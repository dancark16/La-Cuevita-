import { Pipe, PipeTransform } from '@angular/core';

const FILES = '/api/files/';

/** Dirección de la foto en el ancho pedido. Las que no son de la tienda (assets) se dejan igual. */
export function imageSrc(url: string | null | undefined, width: number): string {
  if (!url) {
    return '';
  }
  return url.startsWith(FILES) && !url.includes('?') ? `${url}?w=${width}` : url;
}

@Pipe({ name: 'imageSrc', standalone: true })
export class ImageSrcPipe implements PipeTransform {
  transform(url: string | null | undefined, width: number): string {
    return imageSrc(url, width);
  }
}

/** srcset con dos anchos, para que cada pantalla elija el suyo. */
@Pipe({ name: 'imageSrcset', standalone: true })
export class ImageSrcsetPipe implements PipeTransform {
  transform(url: string | null | undefined, small: number, large: number): string {
    return url && url.startsWith(FILES) && !url.includes('?')
      ? `${imageSrc(url, small)} ${small}w, ${imageSrc(url, large)} ${large}w`
      : '';
  }
}
