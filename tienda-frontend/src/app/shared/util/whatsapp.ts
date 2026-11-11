/** Convierte un teléfono ecuatoriano (0991234567, +593 99..., 593...) al formato internacional sin signos. */
export function toWhatsappNumber(phone: string | null | undefined): string | null {
  const digits = (phone ?? '').replace(/\D/g, '');
  if (digits.length < 8) {
    return null;
  }
  if (digits.startsWith('593')) {
    return digits;
  }
  return `593${digits.replace(/^0+/, '')}`;
}

export function whatsappLink(phone: string | null | undefined, message: string): string | null {
  const number = toWhatsappNumber(phone);
  return number ? `https://wa.me/${number}?text=${encodeURIComponent(message)}` : null;
}
