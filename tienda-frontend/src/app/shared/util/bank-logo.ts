export interface BankLogo {
  src: string;
  background: string;
}

const LOGOS: { match: RegExp; logo: BankLogo }[] = [
  { match: /pichincha/i, logo: { src: 'assets/images/bancos/pichincha.png', background: '#ffdd00' } },
  { match: /guayaquil/i, logo: { src: 'assets/images/bancos/guayaquil.png', background: '#cf006c' } },
];

/** Logo del banco según su nombre, o null si no tenemos uno. */
export function bankLogo(bankName: string | null | undefined): BankLogo | null {
  const name = bankName ?? '';
  return LOGOS.find((item) => item.match.test(name))?.logo ?? null;
}
