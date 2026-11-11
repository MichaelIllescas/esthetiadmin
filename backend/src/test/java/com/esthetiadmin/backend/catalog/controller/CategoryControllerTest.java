package com.esthetiadmin.backend.catalog.controller;

import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.catalog.dto.CategoryResponse;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.catalog.service.CategoryService;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsCategoriesWithTheirServices() throws Exception {
        OfferingResponse bozo = new OfferingResponse(10L, 1L, "Bozo", null, new BigDecimal("15000.00"), true, true);
        when(categoryService.findAll()).thenReturn(List.of(new CategoryResponse(1L, "Depilación láser", true, 60, List.of(bozo))));

        mockMvc.perform(get("/api/categories").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Depilación láser"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].services[0].name").value("Bozo"))
                .andExpect(jsonPath("$[0].services[0].price").value(15000.00));
    }

    @Test
    void createReturns201() throws Exception {
        when(categoryService.create(any())).thenReturn(new CategoryResponse(2L, "Masajes", true, 60, List.of()));

        mockMvc.perform(post("/api/categories").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Masajes\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void createWithABlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/categories").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El nombre de la categoría es obligatorio"));
    }

    @Test
    void createWithARepeatedNameReturns409() throws Exception {
        when(categoryService.create(any())).thenThrow(new ConflictException("Ya existe una categoría con ese nombre"));

        mockMvc.perform(post("/api/categories").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Masajes\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void updateReturns200() throws Exception {
        when(categoryService.update(any(), any())).thenReturn(new CategoryResponse(1L, "Faciales", true, 60, List.of()));

        mockMvc.perform(put("/api/categories/1").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Faciales\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Faciales"));
    }

    @Test
    void setActiveReturns200() throws Exception {
        when(categoryService.setActive(1L, false)).thenReturn(new CategoryResponse(1L, "Faciales", false, 60, List.of()));

        mockMvc.perform(patch("/api/categories/1/active").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void setActiveWithoutTheFlagReturns400() throws Exception {
        mockMvc.perform(patch("/api/categories/1/active").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1").with(jwt())).andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    void deleteOfACategoryWithServicesReturns409() throws Exception {
        doThrow(new ConflictException("La categoría tiene servicios. Trasladalos a otra categoría o desactivala"))
                .when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La categoría tiene servicios. Trasladalos a otra categoría o desactivala"));
    }
}
