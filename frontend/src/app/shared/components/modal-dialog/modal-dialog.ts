import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, input, output, viewChild } from '@angular/core';

let nextId = 0;

/**
 * A modal window built on the native dialog element, which already traps the focus and closes with
 * Escape. It is created when needed and removed by its parent when it emits `closed`.
 */
@Component({
  selector: 'app-modal-dialog',
  templateUrl: './modal-dialog.html',
  styleUrl: './modal-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalDialog implements AfterViewInit {
  readonly heading = input.required<string>();
  readonly closed = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly headingId = `dialog-heading-${nextId++}`;

  ngAfterViewInit(): void {
    const element = this.dialog().nativeElement;
    if (typeof element.showModal === 'function') {
      element.showModal();
    } else {
      element.setAttribute('open', '');
    }
  }

  protected onCancel(event: Event): void {
    event.preventDefault();
    this.closed.emit();
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.closed.emit();
    }
  }
}
