import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ModalDialog } from '../modal-dialog/modal-dialog';

/** Asks for confirmation. It can show why the action was refused and offer a milder alternative. */
@Component({
  selector: 'app-confirm-dialog',
  imports: [ModalDialog],
  templateUrl: './confirm-dialog.html',
  styles: `
    p { margin: 0; line-height: 1.5; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  readonly heading = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input('Eliminar');
  readonly busy = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly alternativeLabel = input<string | null>(null);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
  readonly alternative = output<void>();
}
