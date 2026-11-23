# Covoiturage Croisé

API REST de covoiturage multi-étapes. Un conducteur publie un trajet avec son véhicule. Un passager recherche deux villes dans le bon ordre et réserve des places entre les deux arrêts. Le tarif dépend du segment parcouru et le nombre de places disponibles est commun à tout le trajet.

## Démarrer en local

Prérequis : JDK 17, Maven, Docker avec Compose. Depuis la racine du projet, démarrer PostgreSQL 17 et attendre que la commande de vérification affiche `accepting connections`.

```powershell
docker compose up -d postgres
docker compose exec postgres pg_isready -U covoiturage_user -d covoiturage_db
mvn spring-boot:run
```

L’API écoute sur `http://localhost:8080`. PostgreSQL utilise le port hôte `5433` et la base `covoiturage_db`. Compose conserve les données dans un volume. Hibernate met à jour le schéma au démarrage. Les identifiants fournis servent au développement local.

Dans un deuxième terminal, vérifier une première lecture.

```powershell
curl.exe http://localhost:8080/api/trajets
```

## Architecture

- Le **Controller** reçoit la requête HTTP et valide le DTO d’entrée.
- Le **Service** applique les règles métier et délimite les transactions.
- Le **Repository** utilise Spring Data JPA pour accéder à PostgreSQL. La recherche paginée utilise une requête JPQL.
- Les **DTO** décrivent les données échangées en JSON. MapStruct assure la conversion entre DTO et entités JPA.

Le modèle compte six entités. [Diagramme de classes](docs/diagramme-classes.png) · [Source SVG](docs/diagramme-classes.svg)

## Routes principales

Les routes de la table sont relatives à `/api`. Pour les cinq ressources avec CRUD, `POST` crée un élément, `GET` lit la liste ou un élément par ID, `PUT /{id}` modifie et `DELETE /{id}` supprime.

| Ressource | Opérations |
|---|---|
| `/conducteurs`, `/passagers`, `/vehicules` | CRUD |
| `/trajets` | CRUD, `GET /search`, `PATCH /{id}/annuler` et `PATCH /{id}/terminer` |
| `/points-arret` | CRUD. Pour créer, fournir `?trajetId=…`. Pour lister les arrêts d’un trajet, utiliser `GET /trajet/{trajetId}`. |
| `/reservations` | `POST`, `GET`, `GET /{id}`, `GET /passager/{passagerId}` et `PATCH /{id}/annuler` |

La recherche attend `depart`, `destination` et `date` au format `AAAA-MM-JJ`. Les paramètres facultatifs sont `places` (1 par défaut), `prixMax` (par personne pour le segment), `page` (0 par défaut) et `size` (10 par défaut).

## Démonstration Postman

1. Importer la [collection](postman/endpoints-manquants.postman_collection.json) et l’[environnement local](postman/covoiturage-local.postman_environment.json).
2. Sélectionner **Covoiturage - Local**. Ouvrir **Demo Soutenance** dans le Collection Runner et lancer ses **12 requêtes** dans l’ordre, une seule fois par itération.
3. Vérifier que le trajet créé apparaît dans la recherche et que deux places entre Rabat et Kénitra coûtent **60 MAD**. Les tests attendent volontairement les réponses 400, 404 et 409. Les deux dernières requêtes annulent la réservation et le trajet.

Chaque itération utilise de nouvelles données fictives. [Mode d’emploi de la collection](postman/README.md) · [Guide pour l’oral](docs/guide-soutenance.md)

## Tests

Les tests de service utilisent JUnit 5, Mockito et AssertJ. Ils s’exécutent sans base de données.

```powershell
mvn "-Dtest=*ServiceTest" test
```

Pour exécuter toute la suite, laisser Docker accessible. Le test d’intégration de la recherche démarre son propre PostgreSQL avec Testcontainers.

```powershell
mvn test
```
