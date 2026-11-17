import { Pipe, PipeTransform } from '@angular/core';
import { colorBackground } from './color';

@Pipe({ name: 'colorBackground', standalone: true })
export class ColorBackgroundPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return colorBackground(value);
  }
}
