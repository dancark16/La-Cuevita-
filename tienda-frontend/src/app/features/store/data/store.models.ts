export type StoreSort = 'NEWEST' | 'NAME';

export interface ColorSwatch {
    name: string;
    hex: string;
}

export interface StoreCategory {
    id: string;
    name: string;
    slug: string;
    parentId: string | null;
    imageUrl: string | null;
    active?: boolean;
}

export interface StoreVariantOption {
    id: string;
    sizeLabel: string;
    colorName: string;
    colorHex: string;
    price: number;
    maxQuantity: number;
}

export interface StoreProductSummary {
    id: string;
    name: string;
    categoryId: string;
    categoryName: string;
    price: number;
    compareAtPrice: number | null;
    imageUrl: string | null;
    colors: ColorSwatch[];
    inStock: boolean;
    newArrival: boolean;
    variants: StoreVariantOption[];
}

export interface StoreVariant {
    id: string;
    sku: string;
    sizeLabel: string;
    colorName: string;
    colorHex: string;
    price: number;
    compareAtPrice: number | null;
    images: string[];
    maxQuantity: number;
}

export interface StoreProductDetail {
    id: string;
    name: string;
    description: string | null;
    categoryId: string;
    categoryName: string;
    variants: StoreVariant[];
}

export interface StoreFacets {
    sizes: string[];
    colors: ColorSwatch[];
}

export interface StoreSearch {
    categoryId: string | null;
    q: string;
    sizeLabel?: string | null;
    colorName?: string | null;
    sort: StoreSort;
    page: number;
    size: number;
}