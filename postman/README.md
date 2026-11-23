# Démo Postman — Covoiturage

1. Démarrer PostgreSQL et l’application Spring Boot.
2. Importer `endpoints-manquants.postman_collection.json` et `covoiturage-local.postman_environment.json`.
3. Sélectionner l’environnement **Covoiturage - Local**. Adapter `baseUrl` si nécessaire, sans `/` final.
4. Dans Collection Runner, sélectionner **uniquement Demo Soutenance**, conserver l’ordre des 12 requêtes et lancer une itération. Supprimer toute ancienne valeur locale qui masquerait les variables de l’environnement.

La démo crée un conducteur, un passager, un véhicule et un trajet Casablanca → Rabat → Kenitra. Les dates sont calculées à J+7. Les identifiants sont enregistrés automatiquement dans la collection et l’environnement.

La réservation porte sur **Rabat → Kenitra, 2 places** : `(80 − 50) × 2 = 60 MAD`. Un GET vérifie ce montant. Trois requêtes attendent volontairement **400**, **404** et **409** ; elles doivent apparaître comme des tests réussis. La démo annule ensuite la réservation puis le trajet. Un statut HTTP inattendu arrête le scénario.

Chaque nouvelle exécution crée des données fictives uniques. Les ressources restent en base pour inspection ; la démo ne les supprime pas.

Les six autres dossiers sont un catalogue des **34 routes implémentées**, avec tests 201, 200 ou 204. Utiliser leurs requêtes individuellement avec des IDs valides : leurs suppressions ne sont pas un scénario global et peuvent être refusées si des ressources sont liées. Aucun PUT/DELETE de réservation n’est implémenté côté Java.

**Terminer un trajet** exige un trajet `PUBLIE` ou `COMPLET` dont le départ est déjà passé. Le trajet futur de la démo serait refusé avec 400 : pour démontrer le succès, sélectionner un trajet éligible dans `trajetId`.

Vérification du 23 novembre 2026. Les 12 requêtes de ce dossier ont été rejouées par HTTP sur une instance locale de l’application avec une base PostgreSQL 17 isolée. Les codes attendus, la recherche du trajet, le montant de 60 MAD et les deux annulations ont été confirmés. Ce contrôle a été effectué hors du Runner Postman.
