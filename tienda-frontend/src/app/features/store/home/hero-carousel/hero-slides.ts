export interface HeroSlide {
  eyebrow: string;
  titleLine1: string;
  titleLine2: string;
  text: string;
  ctaLabel: string;
  ctaLink: string;
  image: string;
}

export const HERO_SLIDES: HeroSlide[] = [
  {
    eyebrow: 'Ropa con actitud',
    titleLine1: 'Tu estilo,',
    titleLine2: 'en un solo lugar',
    text: 'Las mejores prendas, las últimas tendencias y la calidad que te acompaña en cada momento.',
    ctaLabel: 'Ver catálogo',
    ctaLink: '/tienda',
    image: 'assets/images/hero-1.webp',
  },
];
