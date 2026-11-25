import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CookieNoticeComponent } from '../../../../shared/components/cookie-notice/cookie-notice.component';
import { MobileTabbarComponent } from '../mobile-tabbar/mobile-tabbar.component';
import { StoreFooterComponent } from '../store-footer/store-footer.component';
import { StoreHeaderComponent } from '../store-header/store-header.component';

@Component({
  selector: 'app-store-layout',
  standalone: true,
  imports: [RouterOutlet, StoreHeaderComponent, StoreFooterComponent, CookieNoticeComponent, MobileTabbarComponent],
  templateUrl: './store-layout.component.html',
  styleUrl: './store-layout.component.scss',
})
export class StoreLayoutComponent {}
