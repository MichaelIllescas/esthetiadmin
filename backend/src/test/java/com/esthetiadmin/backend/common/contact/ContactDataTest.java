package com.esthetiadmin.backend.common.contact;

import com.esthetiadmin.backend.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactDataTest {

    @Test
    void keepsOnlyTheDigitsOfAPhone() {
        assertThat(ContactData.phone(" +54 (341) 555-1234 ")).isEqualTo("543415551234");
    }

    @Test
    void rejectsAMissingPhone() {
        assertThatThrownBy(() -> ContactData.phone("  "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("El teléfono es obligatorio");
        assertThatThrownBy(() -> ContactData.phone(null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsTooFewOrTooManyDigits() {
        assertThatThrownBy(() -> ContactData.phone("555-123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("El teléfono tiene que tener entre 8 y 15 números");
        assertThatThrownBy(() -> ContactData.phone("1234567890123456")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void anOptionalPhoneCanBeMissing() {
        assertThat(ContactData.optionalPhone(null)).isNull();
        assertThat(ContactData.optionalPhone("  ")).isNull();
        assertThat(ContactData.optionalPhone("341 555-1234")).isEqualTo("3415551234");
        assertThatThrownBy(() -> ContactData.optionalPhone("123")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void trimsAndLowersTheEmail() {
        assertThat(ContactData.email("  Carina@Mail.COM ")).isEqualTo("carina@mail.com");
    }

    @Test
    void aBlankEmailIsNone() {
        assertThat(ContactData.email(null)).isNull();
        assertThat(ContactData.email("   ")).isNull();
    }
}
