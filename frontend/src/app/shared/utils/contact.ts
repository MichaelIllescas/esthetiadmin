/** Local Argentine mobiles are kept with 10 digits; WhatsApp needs them as 54 9 + those digits (as the API does). */
export function whatsappUrl(phone: string): string {
  const digits = phone.replace(/\D/g, '');
  return `https://wa.me/${digits.length === 10 ? `549${digits}` : digits}`;
}

/** Same rule as the API: an optional email, so empty is fine. */
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
