import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HERO_SLIDES } from './hero-slides';

const AUTOPLAY_MS = 6000;

@Component({
  selector: 'app-hero-carousel',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './hero-carousel.component.html',
  styleUrl: './hero-carousel.component.scss',
})
export class HeroCarouselComponent implements OnInit, OnDestroy {
  readonly slides = HERO_SLIDES;
  readonly current = signal(0);

  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.startAutoplay();
  }

  ngOnDestroy(): void {
    this.stopAutoplay();
  }

  next(): void {
    this.go(this.current() + 1);
  }

  previous(): void {
    this.go(this.current() - 1);
  }

  go(index: number): void {
    const total = this.slides.length;
    this.current.set(((index % total) + total) % total);
  }

  startAutoplay(): void {
    if (this.slides.length < 2 || this.prefersReducedMotion()) {
      return;
    }
    this.stopAutoplay();
    this.timer = setInterval(() => this.next(), AUTOPLAY_MS);
  }

  stopAutoplay(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  private prefersReducedMotion(): boolean {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  }
}
