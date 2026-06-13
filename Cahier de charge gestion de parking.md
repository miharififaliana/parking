# **Cahier des Charges — Système Intelligent de Gestion de Parking Moto**

**Version** : 1.0 | **Date** : Juin 2026 | **Statut** : Validé

---

## **1\. Présentation du Projet**

### **1.1 Contexte**

La gestion manuelle des parkings motos génère des erreurs coûteuses : doubles attributions de places, erreurs de facturation, perte de tickets et temps d'attente excessifs. Ce projet vise à automatiser entièrement cette gestion.

### **1.2 Objectifs**

* Automatiser l'attribution des places et la génération de tickets  
* Calculer automatiquement les montants dus  
* Fournir un tableau de bord en temps réel au gérant  
* Garantir la traçabilité complète de chaque opération  
* Assurer la persistance des données en cas de coupure électrique

### **1.3 Périmètre**

Le système couvre la gestion d'un parking moto. Il est destiné à fonctionner sur site, en mode local (application desktop).

---

## **2\. Acteurs et Rôles**

| Acteur | Rôle | Actions |
| ----- | ----- | ----- |
| Gérant | Utilisateur principal | Enregistrer entrées/sorties, consulter stats, encaisser |
| Motard | Client final | Recevoir un ticket, présenter à la sortie, payer |
| Système | Acteur automatique | Attribuer les places, calculer les montants, générer les tickets |

---

## **3\. Règles Métier**

### **3.1 Gestion des places**

* **Unicité** : une place accueille une seule moto à la fois  
* **Attribution automatique** : la place libre de plus petit numéro est proposée en priorité  
* **États** :  
  * `LIBRE` — disponible pour attribution  
  * `OCCUPEE` — moto présente, ticket actif associé  
* **Parking complet** : si aucune place n'est à l'état `LIBRE`, aucune entrée ne peut être enregistrée

### **3.2 Facturation**

* Tarif unitaire : **1 € par heure entamée** (paramétrable)  
* Calcul : `montant = CEIL(durée_en_minutes / 60) × tarif_horaire`  
* Durée minimum facturée : 1 heure  
* Exemples :  
  * 30 min → 1 heure → **1 €**  
  * 1h05 → 2 heures → **2 €**  
  * 3h00 → 3 heures → **3 €**

### **3.3 Workflow Entrée**

1. Le gérant lance la recherche d'une place disponible  
2. Le système propose automatiquement la place libre de plus petit numéro  
3. Le gérant saisit l'immatriculation et valide  
4. La place passe à l'état `OCCUPEE`  
5. Le ticket est généré et imprimé

### **3.4 Workflow Sortie**

1. Le gérant scanne ou saisit le numéro de ticket  
2. Le système calcule automatiquement le montant (durée × tarif)  
3. Le gérant encaisse le paiement et valide  
4. La place repasse à l'état `LIBRE`  
5. Le ticket est archivé avec `statut = PAYE`

---

## **4\. Spécifications Fonctionnelles**

### **4.1 Module Authentification**

* Connexion par login / mot de passe  
* Mot de passe haché avec **BCrypt** (facteur de coût ≥ 12\)  
* Déconnexion manuelle

### **4.2 Module Gestion des Places**

* Affichage de la grille de toutes les places avec code couleur :  
  * Vert : LIBRE  
  * Rouge : OCCUPEE  
* Compteurs en temps réel : total / occupées / libres

### **4.3 Module Entrées**

* Recherche automatique de la meilleure place disponible  
* Saisie obligatoire : immatriculation du véhicule

Génération du ticket au format :  
 ┌─────────────────────────────┐│   PARKING MOTO              ││   Ticket N° : TKT-2026-0001 ││   Place     : 12            ││   Immat.    : 1234 TAA      ││   Entrée    : 11/06/2026    ││   Heure     : 08:15         ││   \[QR Code\]                 │└─────────────────────────────┘

*   
* Numérotation auto-incrémentée, format `TKT-AAAA-NNNN`  
* Option d'impression sur imprimante thermique (interface JPA/ESC-POS)

### **4.4 Module Sorties**

* Recherche du ticket par numéro ou par numéro de place  
* Affichage : durée de stationnement, montant calculé  
* Validation du paiement (espèces uniquement en V1 — CB prévu en V2)  
* Archivage automatique du ticket

### **4.5 Tableau de Bord**

* **Occupation en temps réel** : jauge visuelle \+ chiffres  
* **Recettes** : aujourd'hui / cette semaine / ce mois  
* **Derniers mouvements** : liste des 10 dernières entrées/sorties

### **4.6 Module Statistiques**

* Fréquentation par jour / semaine / mois / année  
* Durée moyenne de stationnement  
* Recettes cumulées avec courbe d'évolution  
* Heures de pointe (histogramme horaire)  
* Export CSV des données

---

## **5\. Spécifications Techniques**

### **5.1 Stack Technologique**

| Composant | Technologie | Version | Justification |
| ----- | ----- | ----- | ----- |
| Langage | Java | 17 LTS | Stabilité, support long terme |
| Interface | JavaFX | 21 | Riche, natif Java, CSS supporté |
| Base de données | MySQL | 8.0+ | Fiabilité, transactions ACID |
| Accès données | JDBC pur | — | Contrôle total, sans overhead ORM |
| Sécurité | BCrypt | jBCrypt 0.4 | Standard industrie pour hashage |
| QR Code | ZXing | 3.5+ | Librairie Google, open source |
| Impression | ESC/POS Java | — | Compatible imprimantes thermiques |
| Build | Maven | 3.9+ | Gestion dépendances standardisée |
| Versioning | Git \+ GitHub | — | Traçabilité du code |
| Tests | JUnit 5 | 5.10+ | Tests unitaires et d'intégration |

### **5.2 Architecture MVC**

src/

├── main/

│   ├── java/

│   │   └── com/parking/

│   │       ├── model/          \# Entités (Place, Ticket, User, Log)

│   │       ├── dao/            \# Accès base de données (JDBC)

│   │       ├── service/        \# Logique métier

│   │       ├── controller/     \# Contrôleurs JavaFX

│   │       ├── view/           \# Fichiers FXML \+ CSS

│   │       └── util/           \# QRCode, Impression, DateUtils

│   └── resources/

│       ├── fxml/               \# Layouts JavaFX

│       ├── css/                \# Styles

│       └── config.properties   \# Config DB, tarifs

└── test/

    └── java/                   \# Tests JUnit 5

### **5.3 Base de Données MySQL**

\-- Table des places

CREATE TABLE places (

    id\_place       INT PRIMARY KEY AUTO\_INCREMENT,

    numero\_place   INT NOT NULL UNIQUE,

    statut         ENUM('LIBRE', 'OCCUPEE') DEFAULT 'LIBRE',

    motif\_hs       VARCHAR(255),

    date\_hs        DATETIME,

    INDEX idx\_statut (statut),

    INDEX idx\_numero (numero\_place)

);

\-- Table des tickets

CREATE TABLE tickets (

    id\_ticket       INT PRIMARY KEY AUTO\_INCREMENT,

    numero\_ticket   VARCHAR(20) NOT NULL UNIQUE,   \-- TKT-2026-0001

    id\_place        INT NOT NULL,

    immatriculation VARCHAR(20) NOT NULL,

    date\_entree     DATETIME NOT NULL,

    date\_sortie     DATETIME,

    montant         DECIMAL(10,2),

    statut          ENUM('ACTIF', 'PAYE', 'ANNULE') DEFAULT 'ACTIF',

    FOREIGN KEY (id\_place) REFERENCES places(id\_place),

    INDEX idx\_statut (statut),

    INDEX idx\_date\_entree (date\_entree)

);

\-- Table des utilisateurs

CREATE TABLE utilisateurs (

    id\_user     INT PRIMARY KEY AUTO\_INCREMENT,

    nom         VARCHAR(100) NOT NULL,

    login       VARCHAR(50) NOT NULL UNIQUE,

    mot\_de\_passe VARCHAR(60) NOT NULL,  \-- BCrypt \= 60 chars

    role        ENUM('GERANT', 'CAISSIER', 'ADMIN') DEFAULT 'GERANT',

    actif       BOOLEAN DEFAULT TRUE,

    created\_at  DATETIME DEFAULT CURRENT\_TIMESTAMP

);

\-- Table des logs (journal des actions)

CREATE TABLE logs (

    id\_log      INT PRIMARY KEY AUTO\_INCREMENT,

    id\_user     INT NOT NULL,

    action      VARCHAR(100) NOT NULL,

    detail      TEXT,

    ip\_address  VARCHAR(45),

    created\_at  DATETIME DEFAULT CURRENT\_TIMESTAMP,

    FOREIGN KEY (id\_user) REFERENCES utilisateurs(id\_user),

    INDEX idx\_created\_at (created\_at)

);

### **5.4 Sécurité**

* Hachage BCrypt avec facteur de coût 12 minimum  
* Validation de toutes les entrées utilisateur (format immatriculation, longueurs)  
* Journal des actions horodatées pour chaque opération sensible  
* Aucun mot de passe en clair dans les logs ni dans la configuration  
* Sauvegarde automatique quotidienne via `mysqldump` (script externe)  
* Paramètres de connexion DB dans un fichier `config.properties` hors du jar

---

## **6\. Modèle de Données (Entités Java)**

// Place.java

public class Place {

    private int idPlace;

    private int numeroPlace;

    private StatutPlace statut;  // LIBRE, OCCUPEE

    private String motifHorsSevice;

    private LocalDateTime dateHorsSevice;

}

// Ticket.java

public class Ticket {

    private int idTicket;

    private String numeroTicket;     // TKT-2026-0001

    private int idPlace;

    private String immatriculation;

    private LocalDateTime dateEntree;

    private LocalDateTime dateSortie;

    private BigDecimal montant;      // BigDecimal pour la précision financière

    private StatutTicket statut;     // ACTIF, PAYE, ANNULE

}

---

## **7\. Interfaces Utilisateur**

### **Écrans à développer**

1. **Écran de connexion** — Login, mot de passe, bouton connexion, registration  
2. **Tableau de bord** — Vue principale, jauges d'occupation, recettes  
3. **Enregistrement d'entrée** — Formulaire immatriculation, place proposée, bouton valider  
4. **Enregistrement de sortie** — Saisie ticket, calcul montant, validation paiement  
5. **Gestion des places** — Grille visuelle  
6. **Statistiques** — Graphiques de fréquentation et recettes  
7. **Aperçu du ticket** — Prévisualisation avant impression

### **Charte graphique**

* Interface sobre et claire, contrastes forts pour lisibilité rapide  
* Code couleur cohérent : vert (libre/succès), rouge (occupé/erreur)  
* Police sans-serif lisible (Roboto ou Arial)  
* Boutons larges pour utilisation possible sur écran tactile

---

## **8\. Gestion des Tickets**

### **Format du numéro**

`TKT-AAAA-NNNN` où `AAAA` \= année en cours, `NNNN` \= séquence sur 4 chiffres remise à zéro chaque année.

### **Contenu imprimé**

* Numéro du ticket, numéro de place, immatriculation  
* Date et heure d'entrée  
* QR Code encodant le numéro de ticket (lecture optique possible)  
* Nom du parking, coordonnées

### **États du ticket**

* `ACTIF` — véhicule présent dans le parking  
* `PAYE` — sortie validée et paiement encaissé  
* `ANNULE` — annulé par le gérant (cas exceptionnel, journalisé)

---

## **9\. Gestion des Exceptions et Cas Limites**

| Situation | Comportement attendu |
| ----- | ----- |
| Parking complet | Message clair, aucune attribution possible |
| Ticket introuvable | Alerte gérant, possibilité de recherche manuelle par place |
| Coupure électrique | Redémarrage → reprise exacte du dernier état MySQL |
| Imprimante déconnectée | Ticket affiché à l'écran, impression différée |
| Double validation | Transaction MySQL \+ vérification statut avant toute écriture |
| Place déjà occupée | Refus, alerte, journalisation |

---

## **12\. Jalons et Planning Estimatif**

| Phase | Contenu | Durée estimée |
| ----- | ----- | ----- |
| Phase 0 — Initialisation | Setup Maven, Git, MySQL, structure MVC | 1 semaine |
| Phase 1 — Couche données | JDBC, DAO, modèles, tests unitaires | 2 semaines |
| Phase 2 — Logique métier | Services, règles métier, facturation | 2 semaines |
| Phase 3 — Interface | JavaFX, tous les écrans, CSS | 3 semaines |
| Phase 4 — Fonctions avancées | QR Code, impression, export CSV | 1 semaine |
| Phase 5 — Tests et recette | JUnit, tests intégration, validation gérant | 1 semaine |
| Phase 6 — Déploiement | Installation, documentation, formation | 1 semaine |
| **Total** |  | **\~11 semaines** |

## **14\. Contraintes Techniques**

* **OS cible** : Windows 10/11 (déploiement)  
* **JDK** : Java 17 LTS minimum, distribution Eclipse Adoptium recommandée  
* **Réseau** : fonctionnement 100% local (hors ligne), MySQL en local  
* **Matériel minimal** : 4 Go RAM, écran 1280×720, imprimante thermique USB (optionnel)  
* **Licences** : toutes les dépendances doivent être open source (Apache 2.0 / MIT)

\+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

Premier script avec claude

Objet : Lancement du développement de l'application complète Bonjour. Je souhaite que tu développes une application complète en te basant rigoureusement sur le cahier des charges que je t'ai fourni (voir section Fichiers). Pour mener à bien ce projet de manière propre et exhaustive, nous allons procéder par étapes. Ne génère pas tout le code d'un coup. À chaque étape, tu devras me présenter ton travail et attendre ma validation avant de passer à la suite. Voici la feuille de route que nous allons suivre :

* Étape 1 : Analyse et Architecture \-\> Fais-moi un résumé technique de mon cahier des charges pour me prouver que tu as bien compris tous les besoins (attribution des places, tickets, calcul des montants, tableau de bord, persistance et sécurité anti-coupure).  
* Étape 2 : Initialisation du projet et Configuration \-\> fournis-moi la structure complète des dossiers et fichiers de l'application (l'arborescence), les fichiers de configuration de base (comme le `package.json`, `requirements.txt`, `.env`, etc.) et les commandes pour initialiser proprement l'environnement de développement.  
* Étape 3 : Modélisation des données \-\> Crée le schéma de la base de données (tables, relations, index) qui garantit la résilience et la persistance des données en cas de coupure électrique.  
* Étape 4 : Développement du Backend / Logique métier \-\> Code des API, de la logique d'attribution des places et des modules de calcul.  
* Étape 5 : Développement du Frontend / Interface \-\> Code du tableau de bord en temps réel et des interfaces de gestion.

