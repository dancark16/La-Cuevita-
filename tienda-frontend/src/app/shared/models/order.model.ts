import { OrderStatus } from '../util/order-status';

export type IdentificationType = 'CEDULA' | 'RUC' | 'PASAPORTE';

export interface OrderItem {
  variantId: string;
  sku: string;
  productName: string;
  sizeLabel: string;
  colorName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface OrderAddress {
  line1: string;
  line2: string | null;
  city: string;
  province: string;
  reference: string | null;
}

export interface Order {
  id: string;
  orderNumber: number;
  status: OrderStatus;
  customerName: string;
  customerEmail: string;
  customerPhone: string | null;
  customerIdNumber: string;
  address: OrderAddress;
  items: OrderItem[];
  subtotal: number;
  discount: number;
  total: number;
  paymentMethod: string | null;
  hasReceipt: boolean;
  receiptUploadedAt: string | null;
  adminNote: string | null;
  paidAt: string | null;
  shippedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CheckoutPayload {
  customerProfile: {
    idType: IdentificationType;
    idNumber: string;
    firstName: string | null;
    lastName: string | null;
    businessName: string | null;
    phone: string;
  };
  address: {
    line1: string;
    line2: string | null;
    city: string;
    province: string;
    reference: string | null;
  };
  items: { variantId: string; quantity: number }[];
}
