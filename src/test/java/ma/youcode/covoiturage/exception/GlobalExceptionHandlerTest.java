package ma.youcode.covoiturage.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.NoSuchElementException;
import ma.youcode.covoiturage.controller.ReservationController;
import ma.youcode.covoiturage.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalExceptionHandlerTest {

    private final ReservationService service = mock(ReservationService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ReservationController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{"})
    void invalidBodyReturns400(String body) throws Exception {
        assertProblem(mvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON).content(body)),
                400, "Bad Request", "/api/reservations");
        verifyNoInteractions(service);
    }

    @Test
    void invalidIdReturns400() throws Exception {
        assertProblem(mvc.perform(get("/api/reservations/abc")),
                400, "Bad Request", "/api/reservations/abc");
        verifyNoInteractions(service);
    }

    @Test
    void businessValidationReturns400() throws Exception {
        when(service.cancel(1L)).thenThrow(new IllegalStateException("Annulation trop tardive."));
        assertProblem(mvc.perform(patch("/api/reservations/1/annuler")),
                400, "Bad Request", "/api/reservations/1/annuler")
                .andExpect(jsonPath("$.detail").value("Annulation trop tardive."));
    }

    @Test
    void missingReservationReturns404() throws Exception {
        when(service.findById(99L)).thenThrow(new NoSuchElementException("Reservation introuvable : 99"));
        assertProblem(mvc.perform(get("/api/reservations/99")),
                404, "Not Found", "/api/reservations/99")
                .andExpect(jsonPath("$.detail").value("Reservation introuvable : 99"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Reservation deja confirmee.", "Places insuffisantes."})
    void reservationConflictReturns409(String message) throws Exception {
        when(service.create(any())).thenThrow(new ConflictException(message));
        assertProblem(mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"passagerId":2,"trajetId":1,"pointMonteeId":3,"pointDescenteId":4,"nombrePlaces":1}
                        """)), 409, "Conflict", "/api/reservations")
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Test
    void databaseConflictReturns409WithoutDatabaseDetails() throws Exception {
        when(service.findAll()).thenThrow(new DataIntegrityViolationException("SQL secret constraint"));
        assertProblem(mvc.perform(get("/api/reservations")), 409, "Conflict", "/api/reservations")
                .andExpect(content().string(not(containsString("SQL secret constraint"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void unexpectedErrorDoesNotExposeInternalDetails() throws Exception {
        when(service.findAll()).thenThrow(new RuntimeException("Internal secret"));
        assertProblem(mvc.perform(get("/api/reservations")),
                500, "Internal Server Error", "/api/reservations")
                .andExpect(content().string(not(containsString("Internal secret"))));
    }

    private ResultActions assertProblem(ResultActions result, int statusCode, String title, String path)
            throws Exception {
        return result.andExpect(status().is(statusCode))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(statusCode))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value(path));
    }
}
