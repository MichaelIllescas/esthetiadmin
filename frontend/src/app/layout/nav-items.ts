export interface NavItem {
  label: string;
  path: string;
  /** Modules that are not built yet are shown but cannot be opened. */
  enabled: boolean;
}

export const NAV_ITEMS: readonly NavItem[] = [
  { label: 'Inicio', path: '/', enabled: true },
  { label: 'Agenda', path: '/agenda', enabled: true },
  { label: 'Clientes', path: '/clientes', enabled: true },
  { label: 'Servicios', path: '/servicios', enabled: true },
  { label: 'Tratamientos', path: '/tratamientos', enabled: true },
  { label: 'Horarios', path: '/horarios', enabled: true },
  { label: 'Gastos', path: '/gastos', enabled: true },
  { label: 'Reportes', path: '/reportes', enabled: true },
  { label: 'Configuración', path: '/configuracion', enabled: true },
];
