export type OrderStatus =
  | 'PENDING'
  | 'PAID'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'CONFIRMED';

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: 'Pendiente de pago',
  PAID: 'Pago confirmado',
  PROCESSING: 'Preparando',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
  CONFIRMED: 'Confirmado',
};

/** Pasos que ve la clienta, en orden. */
export const ORDER_STEPS: { status: OrderStatus; label: string }[] = [
  { status: 'PENDING', label: 'Pedido recibido' },
  { status: 'PAID', label: 'Pago confirmado' },
  { status: 'SHIPPED', label: 'Enviado' },
  { status: 'DELIVERED', label: 'Entregado' },
];

export function orderStepIndex(status: OrderStatus): number {
  switch (status) {
    case 'PENDING':
      return 0;
    case 'PAID':
    case 'PROCESSING':
    case 'CONFIRMED':
      return 1;
    case 'SHIPPED':
      return 2;
    case 'DELIVERED':
      return 3;
    default:
      return -1;
  }
}
