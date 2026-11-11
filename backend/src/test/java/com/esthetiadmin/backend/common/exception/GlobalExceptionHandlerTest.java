package com.esthetiadmin.backend.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundExceptionReturns404WithItsMessage() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("El cliente no existe"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unauthorizedExceptionReturns401WithItsMessage() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void badRequestExceptionReturns400WithItsMessage() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Las fechas no son válidas"));
    }

    @Test
    void conflictExceptionReturns409WithItsMessage() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("El cliente tiene tratamientos"));
    }

    @Test
    void invalidBodyReturns400WithValidationMessage() throws Exception {
        mockMvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El nombre es obligatorio"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void wrongParameterTypeReturns400() throws Exception {
        mockMvc.perform(get("/test/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El valor del parámetro 'id' no es válido"));
    }

    @Test
    void unsupportedMethodKeepsItsStatusInsteadOfBecoming500() throws Exception {
        mockMvc.perform(get("/test/validated"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void dataIntegrityViolationReturns409() throws Exception {
        mockMvc.perform(get("/test/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado. Intentá nuevamente más tarde"));
    }

    record TestRequest(@NotBlank(message = "El nombre es obligatorio") String name) {
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("El cliente no existe");
        }

        @GetMapping("/test/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException("Usuario o contraseña incorrectos");
        }

        @GetMapping("/test/bad-request")
        void badRequest() {
            throw new BadRequestException("Las fechas no son válidas");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("El cliente tiene tratamientos");
        }

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/typed/{id}")
        void typed(@PathVariable Long id) {
        }

        @GetMapping("/test/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("constraint violated: secret table detail");
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("internal detail that must not leak");
        }
    }
}
