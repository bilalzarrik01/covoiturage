package ma.youcode.covoiturage.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.youcode.covoiturage.repository.TrajetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Same package as the entities to use their protected JPA constructors without changing production code.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TrajetSearchRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2030, 6, 15);

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Testcontainers removes the database; Hibernate must not drop it after the container stops.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired EntityManager entityManager;
    @Autowired TrajetRepository repository;
    private Conducteur conducteur;
    private Vehicule vehicule;
    private Passager passager;

    @BeforeEach
    void associations() {
        String unique = UUID.randomUUID().toString();
        conducteur = new Conducteur();
        conducteur.setNom("Test");
        conducteur.setPrenom("Conducteur");
        conducteur.setEmail(unique + "@example.com");
        conducteur.setTelephone("0600000000");
        conducteur.setNumeroPermis(unique);
        entityManager.persist(conducteur);

        vehicule = new Vehicule();
        vehicule.setMarque("Dacia");
        vehicule.setModele("Logan");
        vehicule.setImmatriculation(unique);
        vehicule.setCouleur("Blanc");
        vehicule.setCapacite(4);
        vehicule.setConducteur(conducteur);
        entityManager.persist(vehicule);

        passager = new Passager();
        passager.setNom("Test");
        passager.setPrenom("Passager");
        passager.setEmail(unique + "@example.com");
        passager.setTelephone("0600000001");
        entityManager.persist(passager);
    }

    @Test
    void findsIntermediateDepartureAndDestinationIgnoringCaseAndWhitespace() {
        Route route = trip(DATE.atTime(9, 0), StatutTrajet.PUBLIE);

        assertThat(search(" rabat ", "KENITRA", 1, null).getContent())
                .extracting(Trajet::getId).containsExactly(route.trajet().getId());
    }

    @Test
    void rejectsReversedAndIdenticalStops() {
        trip(DATE.atTime(9, 0), StatutTrajet.PUBLIE);

        assertThat(search("Kenitra", "Rabat", 1, null)).isEmpty();
        assertThat(search("Rabat", "Rabat", 1, null)).isEmpty();
    }

    @Test
    void maximumPriceUsesTheSegmentDifferenceAndIncludesTheBoundary() {
        trip(DATE.atTime(9, 0), StatutTrajet.PUBLIE);
        // Rabat = 50, Kenitra = 80: segment price = 30, not 80 or the trip total 150.
        assertThat(search("Rabat", "Kenitra", 1, new BigDecimal("30.00"))).hasSize(1);
        assertThat(search("Rabat", "Kenitra", 1, new BigDecimal("29.99"))).isEmpty();
        assertThat(search("Rabat", "Kenitra", 1, BigDecimal.ZERO)).isEmpty();
        assertThat(search("Rabat", "Kenitra", 1, null)).hasSize(1);
    }

    @Test
    void withoutReservationsAllProposedSeatsAreAvailable() {
        trip(DATE.atTime(9, 0), StatutTrajet.PUBLIE);

        assertThat(search("Rabat", "Kenitra", 4, null)).hasSize(1);
        assertThat(search("Rabat", "Kenitra", 5, null)).isEmpty();
    }

    @Test
    void sumsOnlyConfirmedSeatsAcrossTheEntireTripEvenForNonOverlappingSegments() {
        Route route = trip(DATE.atTime(9, 0), StatutTrajet.PUBLIE);
        reserve(route, 0, 1, 2, StatutReservation.CONFIRMEE);
        reserve(route, 1, 2, 1, StatutReservation.CONFIRMEE);
        reserve(route, 2, 3, 4, StatutReservation.ANNULEE);

        // Neither confirmed segment overlaps Kenitra -> Tanger, but both consume capacity.
        assertThat(search("Kenitra", "Tanger", 2, null)).isEmpty();
        assertThat(search("Kenitra", "Tanger", 1, null)).hasSize(1);
    }

    @Test
    void reservationsOfAnotherTripDoNotConsumeThisTripsSeats() {
        Route available = trip(DATE.atTime(9, 0), StatutTrajet.PUBLIE);
        Route full = trip(DATE.atTime(10, 0), StatutTrajet.PUBLIE);
        reserve(full, 0, 3, 4, StatutReservation.CONFIRMEE);

        assertThat(search("Rabat", "Kenitra", 4, null).getContent())
                .extracting(Trajet::getId).containsExactly(available.trajet().getId());
    }

    @ParameterizedTest
    @EnumSource(value = StatutTrajet.class, names = {"COMPLET", "TERMINE", "ANNULE"})
    void excludesEveryStatusExceptPublie(StatutTrajet status) {
        trip(DATE.atTime(9, 0), status);
        Route published = trip(DATE.atTime(10, 0), StatutTrajet.PUBLIE);

        assertThat(search("Rabat", "Kenitra", 1, null).getContent())
                .extracting(Trajet::getId).containsExactly(published.trajet().getId());
    }

    @Test
    void filtersTripDepartureDateWithInclusiveStartAndExclusiveNextDay() {
        trip(DATE.minusDays(1).atTime(23, 59), StatutTrajet.PUBLIE);
        Route midnight = trip(DATE.atStartOfDay(), StatutTrajet.PUBLIE);
        Route lastMinute = trip(DATE.atTime(23, 59), StatutTrajet.PUBLIE);
        trip(DATE.plusDays(1).atStartOfDay(), StatutTrajet.PUBLIE);

        assertThat(search("Rabat", "Kenitra", 1, null).getContent())
                .extracting(Trajet::getId)
                .containsExactly(midnight.trajet().getId(), lastMinute.trajet().getId());
    }

    @Test
    void paginatesWithAccurateFilteredCountAndAscendingDepartureDate() {
        Route late = trip(DATE.atTime(14, 0), StatutTrajet.PUBLIE);
        Route early = trip(DATE.atTime(8, 0), StatutTrajet.PUBLIE);
        Route middle = trip(DATE.atTime(11, 0), StatutTrajet.PUBLIE);
        trip(DATE.atTime(7, 0), StatutTrajet.ANNULE);
        Route full = trip(DATE.atTime(9, 0), StatutTrajet.PUBLIE);
        reserve(full, 0, 1, 4, StatutReservation.CONFIRMEE);

        Page<Trajet> first = page(0);
        Page<Trajet> second = page(1);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(Trajet::getId)
                .containsExactly(early.trajet().getId(), middle.trajet().getId());
        assertThat(second.getContent()).extracting(Trajet::getId).containsExactly(late.trajet().getId());
        assertThat(page(2)).isEmpty();
        assertThat(page(2).getTotalElements()).isEqualTo(3);
    }

    private Page<Trajet> page(int page) {
        entityManager.flush();
        return repository.search("Rabat", "Kenitra", DATE.atStartOfDay(),
                DATE.plusDays(1).atStartOfDay(), 1, new BigDecimal("30"), PageRequest.of(page, 2));
    }

    private Page<Trajet> search(String from, String to, int seats, BigDecimal price) {
        entityManager.flush();
        return repository.search(from, to, DATE.atStartOfDay(), DATE.plusDays(1).atStartOfDay(),
                seats, price, PageRequest.of(0, 10));
    }

    private Route trip(LocalDateTime departure, StatutTrajet status) {
        Trajet trajet = new Trajet();
        trajet.setDateDepart(departure);
        trajet.setPlacesProposees(4);
        trajet.setStatut(status);
        trajet.setConducteur(conducteur);
        trajet.setVehicule(vehicule);
        entityManager.persist(trajet);
        String[] cities = {"Casablanca", "Rabat", "Kenitra", "Tanger"};
        int[] prices = {0, 50, 80, 150};
        List<PointArret> stops = new ArrayList<>();
        for (int i = 0; i < cities.length; i++) {
            PointArret stop = new PointArret();
            stop.setTrajet(trajet);
            stop.setVille(cities[i]);
            stop.setOrdre(i + 1);
            stop.setHeurePassage(departure.plusHours(i));
            stop.setPrixDepuisDepart(BigDecimal.valueOf(prices[i]));
            entityManager.persist(stop);
            stops.add(stop);
        }
        return new Route(trajet, stops);
    }

    private void reserve(Route route, int from, int to, int seats, StatutReservation status) {
        Reservation reservation = new Reservation();
        reservation.setTrajet(route.trajet());
        reservation.setPassager(passager);
        reservation.setPointMontee(route.stops().get(from));
        reservation.setPointDescente(route.stops().get(to));
        reservation.setNombrePlaces(seats);
        reservation.setMontantTotal(route.stops().get(to).getPrixDepuisDepart()
                .subtract(route.stops().get(from).getPrixDepuisDepart()).multiply(BigDecimal.valueOf(seats)));
        reservation.setDateReservation(DATE.minusDays(1).atStartOfDay());
        reservation.setStatut(status);
        entityManager.persist(reservation);
    }

    private record Route(Trajet trajet, List<PointArret> stops) { }
}
