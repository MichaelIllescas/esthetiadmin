import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SessionService } from '../../core/services/session.service';
import { UserMenu } from './user-menu';

describe('UserMenu', () => {
  const session = { username: signal<string | null>('admin@esthetiadmin.test'), logout: vi.fn() };
  let fixture: ComponentFixture<UserMenu>;

  const toggleButton = () => fixture.nativeElement.querySelector('.user-button') as HTMLButtonElement;
  const panel = () => fixture.nativeElement.querySelector('.panel') as HTMLElement | null;

  beforeEach(async () => {
    session.logout.mockReset();
    await TestBed.configureTestingModule({
      imports: [UserMenu],
      providers: [{ provide: SessionService, useValue: session }],
    }).compileComponents();
    fixture = TestBed.createComponent(UserMenu);
    fixture.detectChanges();
  });

  it('shows the initial of the user and keeps the panel closed at first', () => {
    expect(fixture.nativeElement.querySelector('.avatar').textContent.trim()).toBe('A');
    expect(panel()).toBeNull();
    expect(toggleButton().getAttribute('aria-expanded')).toBe('false');
  });

  it('opens the panel with the user and the logout action', () => {
    toggleButton().click();
    fixture.detectChanges();

    expect(toggleButton().getAttribute('aria-expanded')).toBe('true');
    expect(panel()?.textContent).toContain('admin@esthetiadmin.test');
    expect(panel()?.textContent).toContain('Cerrar sesión');
  });

  it('closes the session from the panel', () => {
    toggleButton().click();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.logout-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(session.logout).toHaveBeenCalledTimes(1);
    expect(panel()).toBeNull();
  });

  it('closes with Escape and gives the focus back to the button', () => {
    toggleButton().click();
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(panel()).toBeNull();
    expect(document.activeElement).toBe(toggleButton());
  });

  it('closes when clicking outside', () => {
    toggleButton().click();
    fixture.detectChanges();

    document.body.click();
    fixture.detectChanges();

    expect(panel()).toBeNull();
  });
});
