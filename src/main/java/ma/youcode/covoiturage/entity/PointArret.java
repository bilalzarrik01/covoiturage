package ma.youcode.covoiturage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "points_arret",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"trajet_id", "ville"}),
            @UniqueConstraint(columnNames = {"trajet_id", "ordre"})
        }
)
public class PointArret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String ville;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer ordre;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime heurePassage;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixDepuisDepart;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trajet_id", nullable = false)
    private Trajet trajet;

    protected PointArret() {
    }

    public Long getId() {
        return id;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public Integer getOrdre() {
        return ordre;
    }

    public void setOrdre(Integer ordre) {
        this.ordre = ordre;
    }

    public LocalDateTime getHeurePassage() {
        return heurePassage;
    }

    public void setHeurePassage(LocalDateTime heurePassage) {
        this.heurePassage = heurePassage;
    }

    public BigDecimal getPrixDepuisDepart() {
        return prixDepuisDepart;
    }

    public void setPrixDepuisDepart(BigDecimal prixDepuisDepart) {
        this.prixDepuisDepart = prixDepuisDepart;
    }

    public Trajet getTrajet() {
        return trajet;
    }

    public void setTrajet(Trajet trajet) {
        this.trajet = trajet;
    }
}
