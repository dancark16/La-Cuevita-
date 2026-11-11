import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, shareReplay } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/page.model';
import {
    StoreCategory,
    StoreFacets,
    StoreProductDetail,
    StoreProductSummary,
    StoreSearch,
} from './store.models';

@Injectable({ providedIn: 'root' })
export class StoreApiService {
    private readonly http = inject(HttpClient);

    private readonly allCategories$ = this.http
        .get<StoreCategory[]>(`${environment.apiUrl}/categories`)
        .pipe(
            map((all) => all.filter((category) => category.active !== false)),
            catchError(() => of([] as StoreCategory[])),
            shareReplay({ bufferSize: 1, refCount: false })
        );

    /** Todas las categorías activas (principales y subcategorías). */
    allCategories(): Observable<StoreCategory[]> {
        return this.allCategories$;
    }

    /** Solo las categorías principales (Mujer, Hombre, Accesorios). */
    rootCategories(): Observable<StoreCategory[]> {
        return this.allCategories$.pipe(map((all) => all.filter((category) => category.parentId === null)));
    }

    searchProducts(search: StoreSearch): Observable<Page<StoreProductSummary>> {
        let params = new HttpParams()
            .set('page', search.page)
            .set('size', search.size)
            .set('sort', search.sort);

        if (search.categoryId) {
            params = params.set('categoryId', search.categoryId);
        }
        if (search.q.trim()) {
            params = params.set('q', search.q.trim());
        }
        if (search.sizeLabel) {
            params = params.set('sizeLabel', search.sizeLabel);
        }
        if (search.colorName) {
            params = params.set('colorName', search.colorName);
        }
        return this.http.get<Page<StoreProductSummary>>(`${environment.apiUrl}/store/products`, {
            params,
        });
    }

    facets(): Observable<StoreFacets> {
        return this.http
            .get<StoreFacets>(`${environment.apiUrl}/store/facets`)
            .pipe(catchError(() => of({ sizes: [], colors: [] } as StoreFacets)));
    }

    getProduct(id: string): Observable<StoreProductDetail> {
        return this.http.get<StoreProductDetail>(`${environment.apiUrl}/store/products/${id}`);
    }
}