package ma.youcode.covoiturage.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.youcode.covoiturage.service.TrajetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TrajetSearchControllerTest {
    private final TrajetService service = mock(TrajetService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TrajetController(service)).build();
    }

    @Test
    void searchRouteUsesDefaultsAndDoesNotResolveAsAnId() throws Exception {
        when(service.search(anyString(), anyString(), any(), anyInt(), any(), any()))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(5)));

        mvc.perform(get("/api/trajets/search").param("depart", "Rabat")
                .param("destination", "Kenitra").param("date", "2030-06-15"))
                .andExpect(status().isOk());

        verify(service).search("Rabat", "Kenitra", LocalDate.of(2030, 6, 15), 1, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "dateDepart")));
        verify(service, never()).findById(anyLong());
    }

    @Test
    void bindsOptionalPriceSeatsAndPagination() throws Exception {
        when(service.search(anyString(), anyString(), any(), anyInt(), any(), any()))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(5)));

        mvc.perform(get("/api/trajets/search").param("depart", "Rabat")
                .param("destination", "Kenitra").param("date", "2030-06-15")
                .param("places", "2").param("prixMax", "30.50").param("page", "1").param("size", "3"))
                .andExpect(status().isOk());

        verify(service).search("Rabat", "Kenitra", LocalDate.of(2030, 6, 15), 2, new BigDecimal("30.50"),
                PageRequest.of(1, 3, Sort.by(Sort.Direction.ASC, "dateDepart")));
    }

    @ParameterizedTest
    @CsvSource({"places,0", "places,-1", "prixMax,-0.01", "page,-1", "size,0", "date,15/06/2030"})
    void rejectsInvalidSearchParameters(String parameter, String value) throws Exception {
        var request = get("/api/trajets/search").param("depart", "Rabat").param("destination", "Kenitra");
        request.param("date", parameter.equals("date") ? value : "2030-06-15");
        if (!parameter.equals("date")) {
            request.param(parameter, value);
        }
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void existingIdRouteStillResolves() throws Exception {
        mvc.perform(get("/api/trajets/42")).andExpect(status().isOk());
        verify(service).findById(42L);
    }
}
