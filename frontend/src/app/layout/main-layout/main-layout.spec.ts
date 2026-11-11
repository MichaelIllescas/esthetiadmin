import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SessionService } from '../../core/services/session.service';
import { MainLayout } from './main-layout';

describe('MainLayout', () => {
  const session = { username: signal<string | null>('admin@esthetiadmin.test'), logout: vi.fn() };
  let fixture: ComponentFixture<MainLayout>;

  const toggle = () => fixture.nativeElement.querySelector('.menu-toggle') as HTMLButtonElement;
  const shell = () => fixture.nativeElement.querySelector('.shell') as HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [provideRouter([]), { provide: SessionService, useValue: session }],
    }).compileComponents();
    fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
  });

  it('lists every module, with only the finished ones as links', () => {
    const links = Array.from(fixture.nativeElement.querySelectorAll('nav a')).map((a) =>
      (a as HTMLElement).textContent?.trim(),
    );
    const pending = Array.from(fixture.nativeElement.querySelectorAll('.nav-disabled')).map((item) =>
      (item as HTMLElement).textContent?.replace('Próximamente', '').trim(),
    );

    expect(links).toEqual(['Inicio', 'Agenda', 'Clientes', 'Servicios', 'Tratamientos', 'Horarios', 'Gastos', 'Reportes', 'Configuración']);
    expect(pending).toEqual([]);
  });

  it('opens and closes the navigation from the menu button', () => {
    expect(toggle().getAttribute('aria-expanded')).toBe('false');

    toggle().click();
    fixture.detectChanges();
    expect(toggle().getAttribute('aria-expanded')).toBe('true');
    expect(shell().classList.contains('menu-open')).toBe(true);

    toggle().click();
    fixture.detectChanges();
    expect(shell().classList.contains('menu-open')).toBe(false);
  });

  it('closes the navigation with Escape', () => {
    toggle().click();
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(shell().classList.contains('menu-open')).toBe(false);
  });

  it('closes the navigation when the backdrop is pressed', () => {
    toggle().click();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.backdrop') as HTMLElement).click();
    fixture.detectChanges();

    expect(shell().classList.contains('menu-open')).toBe(false);
  });

  it('includes the user menu and a router outlet for the pages', () => {
    expect(fixture.nativeElement.querySelector('app-user-menu')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeTruthy();
  });
});
