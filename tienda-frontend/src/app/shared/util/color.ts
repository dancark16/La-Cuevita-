/** Un color puede ser de un tono ('#FFD400') o de dos ('#FFD400/#FFFFFF'). */
export function colorTones(value: string | null | undefined): string[] {
  return (value ?? '').split('/').map((tone) => tone.trim()).filter(Boolean);
}

/** Valor de CSS `background` para mostrar el color: liso, o mitad y mitad si tiene dos tonos. */
export function colorBackground(value: string | null | undefined): string {
  const [first, second] = colorTones(value);
  if (!first) {
    return '#cccccc';
  }
  return second ? `linear-gradient(135deg, ${first} 50%, ${second} 50%)` : first;
}
