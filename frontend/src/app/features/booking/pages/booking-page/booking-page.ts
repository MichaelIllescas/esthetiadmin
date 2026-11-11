import { NgOptimizedImage } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Injector,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { whatsappUrl } from '../../../../shared/utils/contact';
import { formatWeekday } from '../../../appointments/utils/agenda-dates';
import { ContactForm, Contact } from '../../components/contact-form/contact-form';
import { ServicePicker } from '../../components/service-picker/service-picker';
import { PickedSlot, SlotPicker } from '../../components/slot-picker/slot-picker';
import { BookableCategory, BookingConfirmation, ChosenService, durationLabel } from '../../models/booking.models';
import { BookingService } from '../../services/booking.service';

type Step = 'service' | 'slot' | 'contact' | 'done';

/**
 * The public booking page (ADR 014): service → day and time → name and phone → confirmation, without an account.
 * Outside the panel's layout and login; its only way into the panel is a discreet link in the footer.
 */
@Component({
  selector: 'app-booking-page',
  imports: [NgOptimizedImage, RouterLink, ServicePicker, SlotPicker, ContactForm],
  templateUrl: './booking-page.html',
  styleUrl: './booking-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingPage implements OnInit {
  private readonly booking = inject(BookingService);
  private readonly injector = inject(Injector);
  private readonly stepHeading = viewChild<ElementRef<HTMLElement>>('stepHeading');

  protected readonly step = signal<Step>('service');
  protected readonly categories = signal<BookableCategory[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly service = signal<ChosenService | null>(null);
  protected readonly slot = signal<PickedSlot | null>(null);
  /** Why she is back on the times: the one she chose was taken while she filled in her data. */
  protected readonly slotNotice = signal<string | null>(null);
  protected readonly isBooking = signal(false);
  protected readonly bookingError = signal<string | null>(null);
  protected readonly confirmation = signal<BookingConfirmation | null>(null);
  /** The center's phone to cancel or change; null hides those lines (ADR 016). */
  protected readonly contactPhone = signal<string | null>(null);

  protected readonly durationLabel = durationLabel;
  protected readonly formatWeekday = formatWeekday;
  protected readonly whatsappUrl = whatsappUrl;

  ngOnInit(): void {
    this.loadServices();
    // Without it the page still works: it only hides the contact lines.
    this.booking.contact().subscribe({ next: (contact) => this.contactPhone.set(contact.phone), error: () => undefined });
  }

  protected loadServices(): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.booking
      .services()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (categories) => this.categories.set(categories),
        error: (error: unknown) =>
          this.loadError.set(apiErrorMessage(error, 'No pudimos cargar los servicios. Intentá nuevamente.')),
      });
  }

  protected onServiceChosen(service: ChosenService): void {
    this.service.set(service);
    this.slot.set(null);
    this.slotNotice.set(null);
    this.goTo('slot');
  }

  protected onSlotPicked(slot: PickedSlot): void {
    this.slot.set(slot);
    this.slotNotice.set(null);
    this.bookingError.set(null);
    this.goTo('contact');
  }

  protected goTo(step: Step): void {
    this.step.set(step);
    // Moves the focus to the new step's title, so screen readers and keyboards start there.
    afterNextRender(() => this.stepHeading()?.nativeElement.focus(), { injector: this.injector });
  }

  protected book(contact: Contact): void {
    const service = this.service();
    const slot = this.slot();
    if (!service || !slot) {
      return;
    }
    this.isBooking.set(true);
    this.bookingError.set(null);
    this.booking
      .book({ serviceId: service.id, startsAt: `${slot.date}T${slot.time}:00`, ...contact })
      .pipe(finalize(() => this.isBooking.set(false)))
      .subscribe({
        next: (confirmation) => {
          this.confirmation.set(confirmation);
          this.goTo('done');
        },
        error: (error: unknown) => this.onBookingError(error),
      });
  }

  protected startOver(): void {
    this.service.set(null);
    this.slot.set(null);
    this.confirmation.set(null);
    this.goTo('service');
  }

  protected timeOf(dateTime: string): string {
    return dateTime.slice(11, 16);
  }

  protected dateOf(dateTime: string): string {
    return dateTime.slice(0, 10);
  }

  /** A time taken meanwhile sends her back to pick another one on the same day; anything else stays here. */
  private onBookingError(error: unknown): void {
    const message = apiErrorMessage(error, 'No pudimos reservar el turno. Intentá nuevamente.');
    if (error instanceof HttpErrorResponse && error.status === 409 && message.includes('ocuparse')) {
      this.slotNotice.set(message);
      this.goTo('slot');
      return;
    }
    this.bookingError.set(message);
  }
}
