package com.esthetiadmin.backend.expense.controller;

import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.exception.GlobalExceptionHandler;
import com.esthetiadmin.backend.expense.dto.ExpenseListResponse;
import com.esthetiadmin.backend.expense.dto.ExpenseResponse;
import com.esthetiadmin.backend.expense.dto.ExpenseTypeResponse;
import com.esthetiadmin.backend.expense.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ExpenseController.class, ExpenseTypeController.class})
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class ExpenseControllerTest {

    private static final String VALID_BODY =
            "{\"spentOn\":\"2026-09-25\",\"description\":\"Insumos\",\"amount\":1500.50,\"expenseTypeId\":3}";

    private static final ExpenseResponse EXPENSE = new ExpenseResponse(8L, LocalDate.of(2026, 9, 25),
            "Insumos", new BigDecimal("1500.50"), 3L, "Insumos");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());
    }

    @Test
    void searchReturnsExpensesAndTheTotal() throws Exception {
        when(expenseService.search(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 3L))
                .thenReturn(new ExpenseListResponse(List.of(EXPENSE), new BigDecimal("1500.50")));

        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .param("expenseTypeId", "3")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expenseTypeName").value("Insumos"))
                .andExpect(jsonPath("$.totalAmount").value(1500.50));
    }

    @Test
    void createReturns201() throws Exception {
        when(expenseService.create(any())).thenReturn(EXPENSE);

        mockMvc.perform(post("/api/expenses").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8));
    }

    @Test
    void createReportsAllRequiredFields() throws Exception {
        mockMvc.perform(post("/api/expenses").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("La fecha es obligatoria")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("El concepto es obligatorio")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("El importe es obligatorio")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("El tipo de gasto es obligatorio")));
    }

    @Test
    void createRejectsANonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/expenses").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spentOn\":\"2026-09-25\",\"description\":\"Insumos\",\"amount\":0,\"expenseTypeId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El importe debe ser mayor que cero"));
    }

    @Test
    void createRejectsAFutureDate() throws Exception {
        mockMvc.perform(post("/api/expenses").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spentOn\":\"2999-01-01\",\"description\":\"Insumos\",\"amount\":10,\"expenseTypeId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha no puede ser futura"));
    }

    @Test
    void updateReturns200() throws Exception {
        when(expenseService.update(eq(8L), any())).thenReturn(EXPENSE);

        mockMvc.perform(put("/api/expenses/8").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Insumos"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/expenses/8").with(jwt())).andExpect(status().isNoContent());

        verify(expenseService).delete(8L);
    }

    @Test
    void listsExpenseTypes() throws Exception {
        when(expenseService.listTypes()).thenReturn(List.of(new ExpenseTypeResponse(3L, "Insumos")));

        mockMvc.perform(get("/api/expense-types").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].name").value("Insumos"));
    }
}
