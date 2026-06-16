**Analyse Architecturale**

*Parking Moto v1.0 — État des lieux, Écarts & Feuille de Route*

**1\. État Actuel du Projet**

**Stack Technique (Conforme CDC §5.1)**

| Composant | Attendu (CDC) | Réel dans le projet | Statut / Action |
| :---- | :---- | :---- | :---- |
| Java | 17 LTS | pom.xml → release 17 ; IntelliJ → JDK 26 | ⚠️ Aligner IntelliJ sur JDK 17 |
| JavaFX | 21 | javafx-\* 21.0.2 dans pom.xml | ✅ Conforme |
| MySQL | 8.0+ | Driver mysql-connector-j 8.3.0 | ✅ Conforme |
| JDBC pur | Oui | DatabaseConfig.java (pool maison) | ✅ Conforme (Transactions explicites) |
| BCrypt | jBCrypt 0.4 | Déclaré dans pom.xml | ❌ Non utilisé |
| ZXing | 3.5+ | Déclaré dans pom.xml | ❌ Non utilisé |
| Maven | 3.9+ | Plugins javafx-maven-plugin & assembly | ✅ Conforme |
| Tests | JUnit 5 | 1 seul test : AppConfigTest.java | ⚠️ À enrichir |

**Structure des Fichiers (Réelle vs Cible CDC §5.2)**

| src/main/java/com/parking/├── config/       ✅ AppConfig.java, DatabaseConfig.java├── controller/   ⚠️ LoginController.java (Squelette uniquement)├── exception/    ✅ 5 exceptions métier (Existantes mais non branchées)├── MainApp.java  ✅ Point d'entrée JavaFX├── model/        ❌ ABSENT (À créer)├── dao/          ❌ ABSENT (À créer)├── service/      ❌ ABSENT (À créer)└── util/         ❌ ABSENT (À créer)src/main/resources/├── config.properties     ✅ Complet (120 clés)├── sql/init\_database.sql ✅ Schéma \+ données initiales├── fxml/Login.fxml       ⚠️ Seul écran existant├── css/main.css          ✅ Styles de base└── logback.xml           ✅ Logging configuré |
| :---- |

**Fonctionnalités Opérationnelles**

* **Démarrage :** MainApp.java charge la configuration, valide la connexion MySQL et affiche Login.fxml.  
* **Configuration :** AppConfig.java est un singleton complet (gestion DB, tarifs, UI, sécurité).  
* **Infrastructure JDBC :** DatabaseConfig.java fournit un pool thread-safe, autoCommit=false et des transactions explicites.  
* **Base de données :** init\_database.sql contient 5 tables, index, clés étrangères, 50 places et 1 compte admin par défaut.  
* **Interface :** Écran de login fonctionnel visuellement (sans logique d'authentification).

**Avancement par Phase du CDC**

Phase 0 — Initialisation     \[██████████████████░░\]  90% — Structure incomplète, Git OK

Phase 1 — Couche données     \[█████░░░░░░░░░░░░░░░\]  25% — SQL \+ Pool ok, pas de model/DAO

Phase 2 — Logique métier     \[░░░░░░░░░░░░░░░░░░░░\]   0% — Non débutée

Phase 3 — Interface          \[██░░░░░░░░░░░░░░░░░░\]  10% — Squelette Login seul

Phase 4 — Fonctions Avancées \[░░░░░░░░░░░░░░░░░░░░\]   0% — QR, impression, CSV à faire

Phase 5 — Tests              \[█░░░░░░░░░░░░░░░░░░░\]   5% — Seul AppConfigTest existe

Phase 6 — Déploiement        \[░░░░░░░░░░░░░░░░░░░░\]   0% — Non débuté

**2\. Gap Analysis (Écarts vs Cahier des Charges)**

**2.1 Couche Données & Modèle (§5.3, §6)**

| Exigence CDC | État dans le projet | Écart / Action Requise |
| :---- | :---- | :---- |
| Tables places, tickets, utilisateurs, logs | SQL prêt | Script à exécuter côté MySQL. |
| Colonnes motif\_hs / date\_hs sur places (CDC §5.3) | Absentes du SQL | Écart mineur — Aspect hors service non modélisé. À trancher en Phase 1\. |
| Table sequence\_tickets | Présente dans le SQL | Pas de structure DAO côté Java. |
| Entités Java (Place, Ticket, User, Log) | Absentes | Créer le package com.parking.model/. |
| Enums (StatutPlace, StatutTicket, Role) | Absents | À coder pour typer proprement les états. |
| DAO JDBC (PlaceDao, TicketDao, etc.) | Absents | Créer le package com.parking.dao/. |
| Tests unitaires DAO | Absents | Seul AppConfigTest est présent ; couverture à créer. |

**2.2 Logique Métier & Sécurité (§3, §4, §5.4, §8, §9)**

| Module CDC | État actuel | Éléments manquants / Actions |
| :---- | :---- | :---- |
| §4.1 Authentification BCrypt | Squelette d'UI | AuthService, UtilisateurDao, et couplage au LoginController (cost \>= 12). |
| §3.1 / §4.2 Places (Attribution auto) | Aucun code | PlaceService et implémentation de la règle 'plus petit numéro LIBRE'. |
| §3.3 / §4.3 Flux Entrées | Aucun code | TicketService.enregistrerEntree() et formatage TKT-AAAA-NNNN. |
| §3.2 / §4.4 Flux Sorties (Calcul) | Aucun code | TarifService (CEIL(min/60) x tarif) et TicketService.enregistrerSortie(). |
| §4.5 Tableau de bord | Aucun code | DashboardService et liaison FXML. |
| §4.6 Statistiques & CSV | Aucun code | StatsService et utilitaire ExportCsvUtil. |
| §8 QR Code | Dépendance seule | Classe utilitaire util/QrCodeGenerator.java. |
| §4.3 Impression thermique | Config seule | Classe util/ImpressionEscPos.java (désactivable via propriétés). |
| §5.4 Audit logs | Table SQL prête | LogDao et appels transverses dans chaque fin de transaction de service. |
| §9 Protection anti-coupure / double-accès | Pool \+ transactions | Verrouillage optimiste sur les statuts pour parer les conflits. |
| §9 Exceptions métier | Classes créées | ParkingCompletException, etc. existantes mais non utilisées. |

**2.3 Interface Utilisateur (§7)**

| Écran requis (CDC) | Fichier FXML | Statut actuel |
| :---- | :---- | :---- |
| Connexion | fxml/Login.fxml | ⚠️ Interface graphique prête, logique absente |
| Tableau de bord | fxml/Dashboard.fxml | ❌ Absent |
| Enregistrement entrée | fxml/Entree.fxml | ❌ Absent |
| Enregistrement sortie | fxml/Sortie.fxml | ❌ Absent |
| Gestion des places | fxml/Places.fxml | ❌ Absent |
| Statistiques | fxml/Statistiques.fxml | ❌ Absent |
| Aperçu du ticket | fxml/TicketPreview.fxml | ❌ Absent |
| Inscription utilisateur | — | ❌ Absent (Géré par défaut via SQL pour le MVP) |

**3\. Roadmap Chronologique Opérationnelle**

**Étape 0 : Finaliser l'environnement (0.5 jour)**

Objectif : Obtenir une instance MySQL fonctionnelle et connectée.  
• Exécuter le script init\_database.sql.  
• Renseigner les clés db.password et db.user dans config.properties.  
• Valider l'absence de warning MySQL au lancement de mvn javafx:run.  
• Adapter AppConfigTest.java (qui attend l'utilisateur générique).  
• Aligner le JDK de l'IDE sur la version 17 (CDC §14).

**Étape 1 : Modèles & Structures (1 jour)**

Objectif : Aligner les POJOs Java sur les tables relationnelles de la base.  
• Créer dans model/ : Place, Ticket, Utilisateur, Log.  
• Définir les enums : StatutPlace (LIBRE, OCCUPEE), StatutTicket (ACTIF, PAYE, ANNULE), Role (GERANT, CAISSIER, ADMIN).  
• Ajouter l'export adéquat dans module-info.java (exports com.parking.model;).

**Étape 2 : Couche d'Accès aux Données — DAO (3 à 4 jours)**

Objectif : Encapsuler les requêtes JDBC et gérer les transactions proprement.  
• BaseDao : Centralisation du cycle de vie des connexions, commit et rollback.  
• UtilisateurDao : Recherche par identifiant et gestion de l'authentification.  
• PlaceDao : Extraction de la liste complète et méthode de recherche findFirstLibre().  
• TicketDao : Insertion et mise à jour lors de l'archivage/sortie.  
• SequenceTicketDao : Gestion de la numérotation via SELECT FOR UPDATE (Mécanisme transactionnel).  
• LogDao : Traçabilité des actions utilisateurs dans la table d'audit.

**Étape 3 : Couche Service & Règles Métier (4 à 5 jours)**

Objectif : Orchestrer les cas d'utilisation et sécuriser la logique.  
• AuthService : Validation via jBCrypt et instanciation du SessionManager (timeout 480 min).  
• TarifService : Calcul strict du prix selon le CDC (plafond horaire par tranche entamée).  
• TicketService : Gestion unifiée du flux d'entrée et de sortie sous contexte transactionnel isolé.

**Étape 4 : Authentification & Navigation Principale (2 jours)**

Objectif : Assurer le routage sécurisé après le login.  
• Brancher LoginController sur l'AuthService.  
• Créer MainController pour piloter le conteneur global (menu latéral / onglets).  
• Concevoir Dashboard.fxml avec un rafraîchissement périodique (polling de 5000ms).

**Étape 5 : Conception du MVP Entrée / Sortie (3 à 4 jours)**

Objectif : Permettre la réalisation du cycle de vie complet d'un véhicule.  
• Concevoir Entree.fxml (attribution automatique et saisie de l'immatriculation).  
• Concevoir Sortie.fxml (recherche du ticket, calcul en temps réel du solde, validation du paiement).  
• Implémenter Places.fxml (représentation graphique en grille colorée des 50 places).  
• Intercepter correctement les exceptions métier spécifiques créées.

**Étape 6 à 8 : Modules Avancés, Finitions & Déploiement (7 jours)**

Objectif : Compléter les exigences techniques secondaires et packager le produit.  
• Intégrer util/QrCodeGenerator.java (ZXing) pour les impressions de tickets.  
• Créer util/ImpressionEscPos.java avec un commutateur d'activation globale.  
• Implémenter Statistiques.fxml (LineChart/BarChart) et la passerelle ExportCsvUtil.  
• Configurer le script de backup (mysqldump) et compiler le Fat JAR via Maven.

**4\. Priorisation & Stratégie MVP (Minimum Viable Product)**

Le périmètre du MVP est délimité de façon stricte afin de sécuriser les fonctionnalités critiques du cœur de métier à savoir : **Authentification → Attribution automatique → Enregistrement Entrée/Sortie → Suivi de l'occupation en temps réel.**

**Planification Flash sur 2 Semaines**

| Période | Objectifs & Livrables |
| :---- | :---- |
| Semaine 1(Fondations) | • J0 : Exécuter init\_database.sql \+ configurer config.properties• J1 : Implémenter le package model/ et ses enums• J2-J3 : Écrire UtilisateurDao, PlaceDao, TicketDao, SequenceTicketDao• J4-J5 : Implémenter AuthService, TarifService et TicketService |
| Semaine 2(Interfaces & MVP) | • J6 : Connecter LoginController à l'AuthService \-\> Dashboard.fxml basique• J7-J8 : Créer et interconnecter Entree.fxml & Sortie.fxml• J9 : Implémenter la grille de suivi (Places.fxml) et mener les tests complets |

| 💡 Conseil de l'Architecte Lead : Validez l'intégralité de la logique métier et des requêtes SQL à l'aide de tests d'intégration (Étapes 2 et 3\) AVANT de vous lancer dans la conception des interfaces FXML. Isoler la logique de persistance évite de devoir déboguer simultanément les dysfonctionnements graphiques, les anomalies SQL et les erreurs de règles métier. |
| :---- |

**Éléments explicitement repoussés après le MVP :**

• Les statistiques de fréquentation avancées et l'export au format CSV (§4.6).  
• L'impression physique via les commandes directes ESC/POS (§4.3).  
• L'interface d'inscription utilisateur (la création d'un administrateur par défaut via SQL suffit).  
• Le traitement fin des places hors service (motif\_hs & date\_hs).  
• L'écriture du script de sauvegarde automatisée de la base de données.