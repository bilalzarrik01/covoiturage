package ma.youcode.covoiturage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "trajets")
public class Trajet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime dateDepart;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer placesProposees;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTrajet statut;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Conducteur conducteur;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Vehicule vehicule;

    protected Trajet() {
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(LocalDateTime dateDepart) {
        this.dateDepart = dateDepart;
    }

    public Integer getPlacesProposees() {
        return placesProposees;
    }

    public void setPlacesProposees(Integer placesProposees) {
        this.placesProposees = placesProposees;
    }

    public StatutTrajet getStatut() {
        return statut;
    }

    public void setStatut(StatutTrajet statut) {
        this.statut = statut;
    }

    public Conducteur getConducteur() {
        return conducteur;
    }

    public void setConducteur(Conducteur conducteur) {
        this.conducteur = conducteur;
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }
}
