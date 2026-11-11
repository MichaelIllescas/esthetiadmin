import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../../../../environments/environment';
import { BookingLink } from './booking-link';

describe('BookingLink', () => {
  let fixture: ComponentFixture<BookingLink>;
  const writeText = vi.fn();

  const root = () => fixture.nativeElement as HTMLElement;
  const copy = async () => {
    (Array.from(root().querySelectorAll('button')).find((b) => b.textContent?.includes('Copiar')) as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    writeText.mockReset();
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    await TestBed.configureTestingModule({ imports: [BookingLink] }).compileComponents();
    fixture = TestBed.createComponent(BookingLink);
    fixture.detectChanges();
  });

  it('shows the public booking link from the environment, with a way to open it', () => {
    expect((root().querySelector('input') as HTMLInputElement).value).toBe(environment.publicBookingUrl);
    expect(root().querySelector('a')?.getAttribute('href')).toBe(environment.publicBookingUrl);
    expect(root().querySelector('a')?.getAttribute('target')).toBe('_blank');
  });

  it('copies the link and says so', async () => {
    writeText.mockResolvedValue(undefined);

    await copy();

    expect(writeText).toHaveBeenCalledWith(environment.publicBookingUrl);
    expect(root().textContent).toContain('¡Copiado!');
    expect(root().textContent).toContain('Link copiado.');
  });

  it('leaves the link selected to copy by hand when the browser refuses', async () => {
    writeText.mockRejectedValue(new Error('denied'));

    await copy();

    expect(root().textContent).toContain('No se pudo copiar solo');
    const input = root().querySelector('input') as HTMLInputElement;
    expect(input.selectionEnd! - input.selectionStart!).toBe(environment.publicBookingUrl.length);
  });
});
