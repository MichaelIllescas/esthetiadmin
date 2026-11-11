import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { EMAIL_PATTERN } from '../../../../shared/utils/contact';

export interface Contact {
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
}

/** 8 to 15 digits once spaces, dashes and the like are left out, as the API keeps it. */
function phoneDigits(control: AbstractControl<string>): ValidationErrors | null {
  const digits = (control.value ?? '').replace(/\D/g, '').length;
  return digits >= 8 && digits <= 15 ? null : { phone: true };
}

/** Step 3: who the appointment is for. No account: name, last name, phone and, if she wants the confirmation, email. */
@Component({
  selector: 'app-contact-form',
  imports: [ReactiveFormsModule],
  templateUrl: './contact-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContactForm {
  readonly busy = input(false);
  readonly submitted = output<Contact>();

  protected readonly form = new FormGroup({
    firstName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)] }),
    lastName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)] }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.required, phoneDigits, Validators.maxLength(30)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.pattern(EMAIL_PATTERN), Validators.maxLength(254)] }),
  });

  protected isInvalid(control: keyof Contact): boolean {
    return this.form.controls[control].invalid && this.form.controls[control].touched;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { firstName, lastName, phone, email } = this.form.getRawValue();
    this.submitted.emit({
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      phone: phone.trim(),
      email: email.trim() || null,
    });
  }
}
