/** The business data the owner edits in Configuración (ADR 016). Both are optional. */
export interface BusinessSettings {
  notificationEmail: string | null;
  contactPhone: string | null;
}
