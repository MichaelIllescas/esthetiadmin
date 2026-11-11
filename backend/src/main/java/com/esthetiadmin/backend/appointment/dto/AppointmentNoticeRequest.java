package com.esthetiadmin.backend.appointment.dto;

import com.esthetiadmin.backend.notification.dto.NoticeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Aviso por email que se manda a la clienta")
public record AppointmentNoticeRequest(
        @Schema(description = "BOOKED (confirmado), RESCHEDULED (reprogramado) o CANCELLED (cancelado)",
                example = "BOOKED")
        @NotNull(message = "El tipo de aviso es obligatorio") NoticeType type) {
}
