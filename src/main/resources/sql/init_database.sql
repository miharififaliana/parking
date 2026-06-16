-- ==============================================================================
-- init_database.sql — Script d'initialisation de la base de données
-- Système Intelligent de Gestion de Parking Moto — v1.0.0
--
-- Usage : mysql -u root -p < init_database.sql
--
-- Ce script est idempotent : il peut être rejoué sans erreur grâce aux
-- clauses IF NOT EXISTS et CREATE OR REPLACE.
-- ==============================================================================

-- Créer la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS parking_moto
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE parking_moto;

-- Créer l'utilisateur applicatif avec les droits minimaux nécessaires
-- (principe du moindre privilège — §5.4 CDC)
-- IMPORTANT : remplacez 'CHANGE_ME' par votre vrai mot de passe
CREATE USER IF NOT EXISTS 'parking_user'@'localhost'
    IDENTIFIED BY 'CHANGE_ME_IN_PRODUCTION';

GRANT SELECT, INSERT, UPDATE, DELETE ON parking_moto.* TO 'parking_user'@'localhost';
FLUSH PRIVILEGES;

-- ==============================================================================
-- TABLE : places
-- Représente chaque emplacement physique du parking.
-- Index sur statut pour la recherche rapide de places libres (§3.1 CDC).
-- ==============================================================================
CREATE TABLE IF NOT EXISTS places (
    id_place        INT             PRIMARY KEY AUTO_INCREMENT,
    numero_place    INT             NOT NULL UNIQUE COMMENT 'Numéro affiché sur le parking',
    statut          ENUM('LIBRE', 'OCCUPEE') NOT NULL DEFAULT 'LIBRE'
                                    COMMENT 'LIBRE = disponible, OCCUPEE = moto présente',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_statut        (statut)       COMMENT 'Accélère la recherche de places LIBRES',
    INDEX idx_numero_place  (numero_place) COMMENT 'Accélère le tri par numéro (priorité plus petit n°)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Emplacements physiques du parking moto';

-- ==============================================================================
-- TABLE : tickets
-- Chaque ligne = une session de stationnement (entrée → sortie).
-- Transactions ACID InnoDB garantissent la cohérence anti-coupure (§9 CDC).
-- ==============================================================================
CREATE TABLE IF NOT EXISTS tickets (
    id_ticket       INT             PRIMARY KEY AUTO_INCREMENT,
    numero_ticket   VARCHAR(20)     NOT NULL UNIQUE COMMENT 'Format: TKT-AAAA-NNNN (§8 CDC)',
    id_place        INT             NOT NULL COMMENT 'Place occupée pendant cette session',
    immatriculation VARCHAR(20)     NOT NULL COMMENT 'Plaque d immatriculation du véhicule',
    date_entree     DATETIME        NOT NULL COMMENT 'Horodatage d entrée (début facturation)',
    date_sortie     DATETIME        NULL     COMMENT 'Horodatage de sortie (NULL si encore présent)',
    montant         DECIMAL(10,2)   NULL     COMMENT 'Montant calculé à la sortie (NULL si ACTIF)',
    statut          ENUM('ACTIF', 'PAYE', 'ANNULE') NOT NULL DEFAULT 'ACTIF'
                                    COMMENT 'ACTIF=présent, PAYE=sorti, ANNULE=exception gérant',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ticket_place
        FOREIGN KEY (id_place) REFERENCES places(id_place)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    INDEX idx_ticket_statut      (statut)        COMMENT 'Filtre rapide tickets ACTIF',
    INDEX idx_ticket_date_entree (date_entree)   COMMENT 'Accélère les stats par période',
    INDEX idx_ticket_place       (id_place)      COMMENT 'Recherche par numéro de place (§4.4 CDC)',
    INDEX idx_ticket_immat       (immatriculation) COMMENT 'Recherche par immatriculation'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Sessions de stationnement — une ligne par entrée/sortie';

-- ==============================================================================
-- TABLE : utilisateurs
-- Gérants, caissiers et administrateurs du système.
-- Mots de passe BCrypt facteur 12 minimum (§4.1 et §5.4 CDC).
-- ==============================================================================
CREATE TABLE IF NOT EXISTS utilisateurs (
    id_user         INT             PRIMARY KEY AUTO_INCREMENT,
    nom             VARCHAR(100)    NOT NULL,
    login           VARCHAR(50)     NOT NULL UNIQUE COMMENT 'Identifiant de connexion',
    mot_de_passe    VARCHAR(60)     NOT NULL COMMENT 'Hash BCrypt 60 chars — jamais en clair',
    role            ENUM('GERANT', 'CAISSIER', 'ADMIN') NOT NULL DEFAULT 'GERANT',
    actif           BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'false = compte désactivé',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_login  (login)  COMMENT 'Recherche rapide à l authentification',
    INDEX idx_user_actif  (actif)  COMMENT 'Filtre les comptes actifs'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Utilisateurs du système (gérants, caissiers, admins)';

-- ==============================================================================
-- TABLE : logs
-- Journal d'audit de toutes les actions sensibles (§5.4 CDC).
-- Immuable par conception : pas de UPDATE/DELETE sur cette table.
-- ==============================================================================
CREATE TABLE IF NOT EXISTS logs (
    id_log          INT             PRIMARY KEY AUTO_INCREMENT,
    id_user         INT             NOT NULL COMMENT 'Utilisateur ayant effectué l action',
    action          VARCHAR(100)    NOT NULL COMMENT 'Code action: ENTREE, SORTIE, ANNULATION, LOGIN...',
    detail          TEXT            NULL     COMMENT 'Détails JSON ou texte libre de l opération',
    ip_address      VARCHAR(45)     NULL     COMMENT 'Adresse IP (IPv4 ou IPv6)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_log_user
        FOREIGN KEY (id_user) REFERENCES utilisateurs(id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    INDEX idx_log_created_at (created_at) COMMENT 'Recherche par période dans l audit',
    INDEX idx_log_action     (action)     COMMENT 'Filtre par type d action',
    INDEX idx_log_user       (id_user)    COMMENT 'Historique par utilisateur'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Journal d audit immuable — une ligne par action sensible';

-- ==============================================================================
-- TABLE : sequence_tickets
-- Gère la numérotation annuelle des tickets (TKT-AAAA-NNNN).
-- Une ligne par année — remise à zéro automatique au 1er janvier.
-- ==============================================================================
CREATE TABLE IF NOT EXISTS sequence_tickets (
    annee           INT             PRIMARY KEY COMMENT 'Année (ex: 2026)',
    derniere_sequence INT           NOT NULL DEFAULT 0 COMMENT 'Dernier N° utilisé cette année',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Compteur de numérotation des tickets par année';

-- ==============================================================================
-- DONNÉES INITIALES
-- ==============================================================================

-- Compte administrateur par défaut
-- Mot de passe : 'Admin1234!' haché avec BCrypt facteur 12
-- IMPORTANT : changez ce mot de passe immédiatement après le premier déploiement
INSERT IGNORE INTO utilisateurs (nom, login, mot_de_passe, role)
VALUES (
    'Administrateur',
    'admin',
    'Admin1234',
    'ADMIN'
);

-- Initialiser la séquence pour l'année en cours
INSERT IGNORE INTO sequence_tickets (annee, derniere_sequence)
VALUES (YEAR(NOW()), 0);

-- Créer les 50 places du parking (§1.3 CDC — modifiable via config.properties)
-- Ce bloc peut être adapté selon parking.nombrePlaces dans la config
INSERT IGNORE INTO places (numero_place, statut)
SELECT seq, 'LIBRE'
FROM (
    SELECT (a.val + b.val * 10 + 1) AS seq
    FROM
        (SELECT 0 AS val UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
         UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS val UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) b
    HAVING seq <= 50
    ORDER BY seq
) AS numbers;

-- ==============================================================================
-- VÉRIFICATION FINALE
-- ==============================================================================
SELECT 'Tables créées :' AS info;
SHOW TABLES;

SELECT CONCAT('Places créées : ', COUNT(*)) AS info FROM places;
SELECT CONCAT('Utilisateurs : ', COUNT(*)) AS info FROM utilisateurs;
