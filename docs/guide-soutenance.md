# Covoiturage Croisé — Guide de soutenance

## 1. Présenter le projet

« Un conducteur annonce son itinéraire et ses arrêts. Un passager peut réserver uniquement la partie du trajet qui l’intéresse. »

**Exemple.** Le conducteur part de Casablanca pour Kénitra. Sara monte à Rabat et descend à Kénitra.

**Question du jury — Que signifie “multi-étapes” ?** Un trajet comporte plusieurs arrêts ordonnés. Le passager choisit son arrêt de montée et son arrêt de descente.

## 2. Expliquer les six entités

Un **Conducteur** possède des **Vehicule** et publie des **Trajet**. Chaque trajet référence un conducteur et un véhicule. Chaque **PointArret** appartient à un trajet. Une **Reservation** relie un **Passager** à un trajet et référence deux arrêts de ce trajet.

**Exemple.** Le véhicule de Yassine est utilisé pour le trajet Casablanca → Rabat → Kénitra. La réservation de Sara référence les arrêts Rabat et Kénitra. [Ouvrir le diagramme](diagramme-classes.png)

**Question du jury — Pourquoi deux liens entre réservation et arrêt ?** Ils correspondent à deux rôles différents : où le passager monte et où il descend.

## 3. Suivre une requête

Le **Controller** reçoit le JSON sous forme de **DTO** et vérifie les champs. Le **Service** décide si l’action est permise. Le **Repository** lit ou écrit dans PostgreSQL. MapStruct prépare le DTO de réponse à partir de l’entité.

**Exemple.** Pour une réservation, le client envoie les identifiants et le nombre de places. Le service calcule le montant et le contrôleur retourne le résultat avec HTTP 201.

**Question du jury — Pourquoi utiliser un DTO ?** Il définit ce que le client peut envoyer et recevoir. Le client demande deux places et le serveur calcule le prix.

## 4. Créer un trajet

Le service vérifie que le véhicule appartient au conducteur, que sa capacité suffit et que le départ est futur. Il faut au moins deux villes distinctes. Dans l’ordre fourni, les numéros, heures et prix doivent augmenter. Aucun arrêt ne peut précéder le départ. Le premier prix vaut zéro. Le trajet est publié avec ses arrêts dans une transaction.

**Exemple.** Départ à 9 h de Casablanca, prix 0. Rabat à 10 h, prix cumulé 50. Kénitra à 11 h, prix cumulé 80.

**Question du jury — Peut-on retirer des places après une réservation ?** Oui, jusqu’au nombre de places déjà confirmées. Si toutes les places proposées sont prises après la modification, le trajet passe à `COMPLET`.

## 5. Expliquer la recherche JPQL

La requête cherche un trajet publié dans la journée demandée et encore réservable au moment de la recherche. Elle vérifie que les deux villes existent sur le même trajet, dans le bon ordre. Elle filtre les places restantes et, si demandé, le prix maximal par personne. PostgreSQL renvoie une page. Une requête de comptage calcule le total avec les mêmes filtres.

**Exemple.** Avec `depart=Rabat`, `destination=Kenitra`, `places=2` et `prixMax=30`, le trajet de l’exemple convient si deux places restent libres. La page 0 est la première page.

**Question du jury — Pourquoi utiliser JPQL ?** La requête exprime les filtres avec les entités Java. La base filtre et pagine les résultats avant leur retour au service.

## 6. Créer une réservation

Le service verrouille le trajet, vérifie qu’il est publié et futur, puis charge le passager et les arrêts. La montée doit précéder la descente. Le service refuse un doublon confirmé ou une demande dépassant les places disponibles.

**Exemple.** Sara réserve deux places de Rabat à Kénitra. Elle reçoit une réservation `CONFIRMEE` avec un identifiant.

**Question du jury — Sara peut-elle réserver à nouveau le même trajet ?** Une seule réservation confirmée par passager et par trajet est autorisée. Une ancienne réservation annulée ne compte plus comme doublon.

## 7. Calculer le prix

Chaque arrêt porte un prix cumulé depuis le départ. On soustrait le prix de la montée à celui de la descente, puis on multiplie par le nombre de places. Java utilise `BigDecimal` pour ce calcul.

**Exemple.** Rabat vaut 50 MAD et Kénitra 80 MAD. Pour deux places, **(80 − 50) × 2 = 60 MAD**.

**Question du jury — Pourquoi soustraire 50 ?** Le passager monte à Rabat. Les 50 MAD correspondent à la partie du trajet effectuée avant sa montée.

## 8. Compter les places et gérer les statuts

Places libres = places proposées − somme des places des réservations confirmées du trajet. La dernière place rend le trajet `COMPLET`. Une annulation individuelle est possible au moins 24 h avant le départ. Si elle libère des places sur un trajet complet, il redevient `PUBLIE`.

**Exemple.** Avec quatre places proposées et deux confirmées, il en reste deux. Une seconde réservation de deux places remplit le trajet.

**Question du jury — Réutilisez-vous une place après la descente d’un passager ?** Dans cette version, la capacité est commune à tout le trajet. Seul le tarif est calculé par segment.

## 9. Éviter la surréservation

`@Transactional` regroupe les vérifications et l’écriture. Le verrou `PESSIMISTIC_WRITE` sur le trajet oblige deux demandes pour ce même trajet à attendre leur tour. La seconde relit la situation après la fin de la première transaction.

**Exemple.** Deux passagers demandent la dernière place. Si la première réservation est confirmée, le trajet devient complet. La seconde demande doit être refusée.

**Question du jury — Une transaction seule suffit-elle ?** Deux transactions pourraient lire la même place disponible avant d’écrire. Le verrou empêche ces deux décisions simultanées sur le même trajet.

## 10. Lire une erreur HTTP

Le gestionnaire global transforme les exceptions en réponses `ProblemDetail`. Le client reçoit un code HTTP et une description. La démo illustre trois cas.

| Requête | Réponse attendue |
|---|---|
| Envoyer une réservation vide | 400, données invalides |
| Lire une réservation dont l’identifiant est absent | 404, ressource introuvable |
| Réserver une deuxième fois sur le même trajet encore publié | 409, conflit |

**Question du jury — Pourquoi un doublon donne-t-il 409 ?** La demande est correctement formée, mais une réservation confirmée existe déjà pour ce passager et ce trajet.

## 11. Expliquer les tests

**JUnit 5** exécute les tests. **Mockito** permet de simuler les dépendances. `@Mock` crée un objet simulé. `@InjectMocks` instancie le vrai service avec ces objets. **AssertJ** sert à vérifier le résultat ou l’exception.

**Exemple.** Dans `TrajetUpdateServiceTest`, on prépare trois places déjà confirmées, on demande de réduire l’offre à deux, puis on vérifie le refus et l’absence d’enregistrement.

**Question du jury — Que prouve ce test sans base ?** Il vérifie la décision du service. Un test distinct avec PostgreSQL et Testcontainers vérifie la requête JPQL réelle.

## 12. Démontrer l’API avec Postman

Démarrer la base et l’application, importer la collection et l’environnement, puis lancer **Demo Soutenance** dans le Runner. Les identifiants passent automatiquement d’une requête à l’autre.

**Exemple de narration.** « Je crée les utilisateurs, le véhicule et le trajet. Je retrouve le trajet depuis Rabat. Je réserve deux places et je relis le montant de 60 MAD. Je montre trois erreurs attendues, puis j’annule la réservation et le trajet. » [Instructions de lancement](../README.md)

**Question du jury — Pourquoi la démo annule-t-elle le trajet au lieu de le terminer ?** La démo crée un départ à J+7. L’action `terminer` exige que le départ soit déjà passé.
