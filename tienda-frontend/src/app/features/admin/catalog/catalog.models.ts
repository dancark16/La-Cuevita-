export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'DRAFT';
export type VariantStatus = 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED';

export interface Category {
  id: string;
  name: string;
  slug: string;
  parentId: string | null;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
}

export interface CategoryRequest {
  name: string;
  slug: string;
  parentId: string | null;
  imageUrl: string | null;
  active: boolean;
}

export interface Product {
  id: string;
  categoryId: string;
  categoryName: string;
  name: string;
  description: string | null;
  baseStatus: ProductStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  categoryId: string;
  name: string;
  description: string | null;
  baseStatus: ProductStatus;
}

export interface Size {
  id: string;
  label: string;
  active: boolean;
}

export interface SizeRequest {
  label: string;
  active?: boolean;
}

export interface Color {
  id: string;
  name: string;
  hex: string;
  active: boolean;
}

export interface ColorRequest {
  name: string;
  hex: string;
  active?: boolean;
}

export interface VariantImage {
  id: string;
  url: string;
  sortOrder: number;
}

export interface ImageRequest {
  url: string;
  sortOrder: number;
}

export interface Variant {
  id: string;
  productId: string;
  productName: string;
  sizeId: string;
  sizeLabel: string;
  colorId: string;
  colorName: string;
  colorHex: string;
  sku: string;
  barcode: string | null;
  price: number;
  promoPrice: number | null;
  status: VariantStatus;
  images: VariantImage[];
  createdAt: string;
  updatedAt: string;
}

export interface VariantRequest {
  productId: string;
  sizeId: string;
  colorId: string;
  sku: string;
  barcode: string | null;
  price: number;
  promoPrice: number | null;
  status: VariantStatus;
}

export interface CatalogItem {
  variantId: string;
  thumbnailUrl: string | null;
  productId: string;
  sku: string;
  productName: string;
  categoryId: string;
  categoryName: string;
  sizeLabel: string;
  colorName: string;
  colorHex: string;
  price: number;
  promoPrice: number | null;
  stock: number;
  variantStatus: VariantStatus;
  productStatus: ProductStatus;
}

export interface CatalogItemUpdate {
  name?: string;
  categoryId?: string;
  price?: number;
  promoPrice?: number;
}

export interface ProductRowVariant {
  variantId: string;
  sku: string;
  sizeLabel: string;
  colorName: string;
  colorHex: string;
  price: number;
  promoPrice: number | null;
  stock: number;
}

/** Un producto del listado de administración, con todos sus colores y tallas. */
export interface ProductRow {
  productId: string;
  name: string;
  categoryId: string;
  categoryName: string;
  thumbnailUrl: string | null;
  status: ProductStatus;
  minPrice: number;
  maxPrice: number;
  onPromo: boolean;
  totalStock: number;
  variants: ProductRowVariant[];
}
