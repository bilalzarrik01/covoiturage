package ma.youcode.covoiturage.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import ma.youcode.covoiturage.dto.trajet.TrajetResponse;
import ma.youcode.covoiturage.entity.StatutTrajet;
import ma.youcode.covoiturage.exception.GlobalExceptionHandler;
import ma.youcode.covoiturage.service.TrajetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TrajetStatusControllerTest {
    private final TrajetService service = mock(TrajetService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TrajetController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"terminer", "annuler"})
    void returnsUpdatedTripWith200(String action) throws Exception {
        StatutTrajet status = action.equals("terminer") ? StatutTrajet.TERMINE : StatutTrajet.ANNULE;
        TrajetResponse response = new TrajetResponse(1L, LocalDateTime.now(), 4, status, 2L, 3L, List.of());
        when(service.complete(1L)).thenReturn(response);
        when(service.cancel(1L)).thenReturn(response);

        mvc.perform(patch("/api/trajets/1/" + action)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.statut").value(status.name()));
        if (action.equals("terminer")) {
            verify(service).complete(1L);
            verify(service, never()).cancel(anyLong());
        } else {
            verify(service).cancel(1L);
            verify(service, never()).complete(anyLong());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"terminer", "annuler"})
    void invalidTransitionReturns400(String action) throws Exception {
        when(service.complete(1L)).thenThrow(new IllegalStateException("Transition interdite."));
        when(service.cancel(1L)).thenThrow(new IllegalStateException("Transition interdite."));
        mvc.perform(patch("/api/trajets/1/" + action)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Transition interdite."));
    }
}
