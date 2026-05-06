-- ============================================================
-- Script d'initialisation de la base de données Tiky
-- SGBD : MariaDB
-- ============================================================

-- Suppression des tables si elles existaient
DROP TABLE IF EXISTS Attribuer;
DROP TABLE IF EXISTS Disponibilite;
DROP TABLE IF EXISTS Notification;
DROP TABLE IF EXISTS Groupe_even;
DROP TABLE IF EXISTS Demande_cat;
DROP TABLE IF EXISTS Participant_even;
DROP TABLE IF EXISTS Abonne_cat;
DROP TABLE IF EXISTS Convers_util;
DROP TABLE IF EXISTS Message;
DROP TABLE IF EXISTS Historique_tick;
DROP TABLE IF EXISTS Abonne_tick;
DROP TABLE IF EXISTS Lien_ticket;
DROP TABLE IF EXISTS Document;
DROP TABLE IF EXISTS Conversation;
DROP TABLE IF EXISTS Ticket;
DROP TABLE IF EXISTS Etud_membre_group;
DROP TABLE IF EXISTS Groupe_etud;
DROP TABLE IF EXISTS Categorie;
DROP TABLE IF EXISTS Evenement;
DROP TABLE IF EXISTS Type_even;
DROP TABLE IF EXISTS Role;
DROP TABLE IF EXISTS Utilisateur;

-- ============================================================
-- TABLE : Utilisateur
-- ============================================================
CREATE TABLE Utilisateur (
    id_util     INT AUTO_INCREMENT,
    nom         VARCHAR(100)    NOT NULL,
    email       VARCHAR(100)    NOT NULL UNIQUE,
    mdp         VARCHAR(255)    NOT NULL,
    type        ENUM('etudiant', 'enseignant') NOT NULL,
    date_creation DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_util)
);

-- ============================================================
-- TABLE : Role
-- ============================================================
CREATE TABLE Role (
    id      INT AUTO_INCREMENT,
    nom     VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- TABLE : Attribuer (liaison Utilisateur <-> Role)
-- ============================================================
CREATE TABLE Attribuer (
    id_util INT NOT NULL,
    id      INT NOT NULL,
    PRIMARY KEY (id_util, id),
    FOREIGN KEY (id_util) REFERENCES Utilisateur(id_util) ON DELETE CASCADE,
    FOREIGN KEY (id)      REFERENCES Role(id)             ON DELETE CASCADE
);

-- ============================================================
-- TABLE : Groupe_etud
-- ============================================================
CREATE TABLE Groupe_etud (
    id_grp_etd  INT AUTO_INCREMENT,
    nom         VARCHAR(100) NOT NULL,
    id_util     INT          NOT NULL,  -- créateur du groupe
    PRIMARY KEY (id_grp_etd),
    FOREIGN KEY (id_util) REFERENCES Utilisateur(id_util)
);

-- ============================================================
-- TABLE : Etud_membre_group
-- ============================================================
CREATE TABLE Etud_membre_group (
    id_etd_mem_grp  INT AUTO_INCREMENT,
    id_util         INT NOT NULL,
    id_grp_etd      INT NOT NULL,
    PRIMARY KEY (id_etd_mem_grp),
    FOREIGN KEY (id_util)    REFERENCES Utilisateur(id_util),
    FOREIGN KEY (id_grp_etd) REFERENCES Groupe_etud(id_grp_etd)
);

-- ============================================================
-- TABLE : Categorie
-- ============================================================
CREATE TABLE Categorie (
    id_cat      INT AUTO_INCREMENT,
    nom         VARCHAR(100)    NOT NULL,
    description TEXT            NOT NULL,
    est_valide  BOOLEAN         NOT NULL DEFAULT FALSE,
    id_util     INT             NOT NULL,   -- category owner
    id_util_1   INT,                        -- validateur (nul si créée directement)
    PRIMARY KEY (id_cat),
    FOREIGN KEY (id_util)   REFERENCES Utilisateur(id_util),
    FOREIGN KEY (id_util_1) REFERENCES Utilisateur(id_util)
);

-- ============================================================
-- TABLE : Demande_cat
-- ============================================================
CREATE TABLE Demande_cat (
    id_dem_cat      INT AUTO_INCREMENT,
    nom             VARCHAR(100)    NOT NULL,
    description     TEXT            NOT NULL,
    statut          ENUM('en_attente', 'acceptee', 'refusee') NOT NULL DEFAULT 'en_attente',
    date_creation   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_traitement DATE,                       -- nul si  pas encore traitée
    id_util         INT             NOT NULL,   -- étudiant qui propose
    id_util_1       INT,                        -- enseignant qui valide 
    id_cat          INT,                        -- catégorie créée 
    PRIMARY KEY (id_dem_cat),
    FOREIGN KEY (id_util)   REFERENCES Utilisateur(id_util),
    FOREIGN KEY (id_util_1) REFERENCES Utilisateur(id_util),
    FOREIGN KEY (id_cat)    REFERENCES Categorie(id_cat)
);

-- ============================================================
-- TABLE : Abonne_cat
-- ============================================================
CREATE TABLE Abonne_tick (
     id_abn_tick   INT AUTO_INCREMENT,
     raison        ENUM('interesse', 'peut_aider', 'aussi_impacte') NOT NULL,
     date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     derniere_visite DATETIME,          -- ajouté pour US visualiser issues suivies
     id_util       INT      NOT NULL,
     id_tick       INT      NOT NULL,
     PRIMARY KEY (id_abn_tick),
     FOREIGN KEY (id_util) REFERENCES Utilisateur(id_util),
     FOREIGN KEY (id_tick) REFERENCES Ticket(id_tick)
);

-- ============================================================
-- TABLE : Ticket
-- ============================================================
CREATE TABLE Ticket (
    id_tick         INT AUTO_INCREMENT,
    titre           VARCHAR(255)    NOT NULL,
    description     TEXT            NOT NULL,
    statut          ENUM('ouvert', 'en_cours', 'incomplet', 'ferme') NOT NULL DEFAULT 'ouvert',
    est_prive       BOOLEAN         NOT NULL DEFAULT FALSE,
    date_creation   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut_gest     ENUM('en_attente', 'accepte', 'resolu', 'reouvert') NOT NULL DEFAULT 'en_attente', -- un gestionnaire doit pouvoir accepter son rôle
    commentaire     TEXT,
    type_rejet      ENUM('info_manquante', 'reecriture', 'autre'),
    raison_rejet    TEXT,           -- rempli seulement si type_rejet = 'autre'
    statut_manager  ENUM('en_attente', 'accepte', 'refuse') NOT NULL DEFAULT 'en_attente',
    id_util         INT             NOT NULL,   -- créateur
    id_cat          INT             NOT NULL,   -- catégorie
    id_util_1       INT,                        -- manager (nul au départ)
    PRIMARY KEY (id_tick),
    FOREIGN KEY (id_util)   REFERENCES Utilisateur(id_util),
    FOREIGN KEY (id_cat)    REFERENCES Categorie(id_cat),
    FOREIGN KEY (id_util_1) REFERENCES Utilisateur(id_util)
);

-- ============================================================
-- TABLE : Document
-- ============================================================
CREATE TABLE Document (
    id_doc          INT AUTO_INCREMENT,
    nom             VARCHAR(255)    NOT NULL,
    url             VARCHAR(500)    NOT NULL,
    date_creation   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_tick         INT,            -- nul si document lié à un message
    PRIMARY KEY (id_doc),
    FOREIGN KEY (id_tick) REFERENCES Ticket(id_tick)
);

-- ============================================================
-- TABLE : Conversation
-- ============================================================
CREATE TABLE Conversation (
    id_convs        INT AUTO_INCREMENT,
    date_creation   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_tick         INT      NOT NULL UNIQUE,   -- une conversation par ticket
    PRIMARY KEY (id_convs),
    FOREIGN KEY (id_tick) REFERENCES Ticket(id_tick)
);

-- ============================================================
-- TABLE : Convers_util (participants à une conversation)
-- ============================================================
CREATE TABLE Convers_util (
    id_convs_util   INT AUTO_INCREMENT,
    role            ENUM('initiateur', 'participant') NOT NULL,
    id_convs        INT NOT NULL,
    id_util         INT NOT NULL,
    PRIMARY KEY (id_convs_util),
    FOREIGN KEY (id_convs) REFERENCES Conversation(id_convs),
    FOREIGN KEY (id_util)  REFERENCES Utilisateur(id_util)
);

-- ============================================================
-- TABLE : Message
-- ============================================================
CREATE TABLE Message (
    id_mes          INT AUTO_INCREMENT,
    contenu         TEXT        NOT NULL,
    date_creation   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_util         INT         NOT NULL,
    id_tick         INT         NOT NULL,
    id_doc          INT,        -- nul si message sans pièce jointe
    id_convs        INT         NOT NULL,
    PRIMARY KEY (id_mes),
    FOREIGN KEY (id_util)  REFERENCES Utilisateur(id_util),
    FOREIGN KEY (id_tick)  REFERENCES Ticket(id_tick),
    FOREIGN KEY (id_doc)   REFERENCES Document(id_doc),
    FOREIGN KEY (id_convs) REFERENCES Conversation(id_convs)
);

-- ============================================================
-- TABLE : Abonne_tick
-- ============================================================
CREATE TABLE Abonne_tick (
    id_abn_tick     INT AUTO_INCREMENT,
    raison          ENUM('interesse', 'peut_aider', 'aussi_impacte') NOT NULL,
    date_creation   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_util         INT      NOT NULL,
    id_tick         INT      NOT NULL,
    PRIMARY KEY (id_abn_tick),
    FOREIGN KEY (id_util) REFERENCES Utilisateur(id_util),
    FOREIGN KEY (id_tick) REFERENCES Ticket(id_tick)
);

-- ============================================================
-- TABLE : Historique_tick
-- ============================================================
CREATE TABLE Historique_tick (
    id_hist_tick    INT AUTO_INCREMENT,
    type            VARCHAR(100)    NOT NULL,
    contenu         TEXT            NOT NULL,
    date_creation   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_util         INT             NOT NULL,
    id_tick         INT             NOT NULL,
    PRIMARY KEY (id_hist_tick),
    FOREIGN KEY (id_util) REFERENCES Utilisateur(id_util),
    FOREIGN KEY (id_tick) REFERENCES Ticket(id_tick)
);

-- ============================================================
-- TABLE : Lien_ticket
-- ============================================================
CREATE TABLE Lien_ticket (
    id_lien_tick    INT AUTO_INCREMENT,
    type            ENUM('lien_simple', 'fusion') NOT NULL,
    date_creation   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut          ENUM('en_attente', 'accepte', 'refuse') NOT NULL DEFAULT 'en_attente',
    id_tick         INT,        -- ticket source (nul si fusion)
    id_tick_1       INT         NOT NULL,   -- ticket cible
    id_tick_2       INT,        -- ticket fusionné résultant (nul)
    PRIMARY KEY (id_lien_tick),
    FOREIGN KEY (id_tick)   REFERENCES Ticket(id_tick),
    FOREIGN KEY (id_tick_1) REFERENCES Ticket(id_tick),
    FOREIGN KEY (id_tick_2) REFERENCES Ticket(id_tick)
);

-- ============================================================
-- TABLE : Type_even
-- ============================================================
CREATE TABLE Type_even (
    id_typ_evt  INT AUTO_INCREMENT,
    nom         VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_typ_evt)
);

-- ============================================================
-- TABLE : Evenement
-- ============================================================
CREATE TABLE Evenement (
    id_evt      INT AUTO_INCREMENT,
    titre       VARCHAR(255)    NOT NULL,
    description TEXT            NOT NULL,
    date_deb    DATETIME        NOT NULL,
    date_fin    DATETIME        NOT NULL,
    statut      ENUM('planifie', 'en_cours', 'termine', 'annule') NOT NULL DEFAULT 'planifie',
    id_typ_evt  INT             NOT NULL,
    id_util     INT             NOT NULL,   -- organisateur
    PRIMARY KEY (id_evt),
    FOREIGN KEY (id_typ_evt) REFERENCES Type_even(id_typ_evt),
    FOREIGN KEY (id_util)    REFERENCES Utilisateur(id_util)
);

-- ============================================================
-- TABLE : Participant_even
-- ============================================================
CREATE TABLE Participant_even (
    id_pt_evt   INT AUTO_INCREMENT,
    statut      ENUM('invite', 'confirme', 'refuse', 'present') NOT NULL DEFAULT 'invite',
    id_evt      INT NOT NULL,
    id_util     INT NOT NULL,
    PRIMARY KEY (id_pt_evt),
    FOREIGN KEY (id_evt)  REFERENCES Evenement(id_evt),
    FOREIGN KEY (id_util) REFERENCES Utilisateur(id_util)
);

-- ============================================================
-- TABLE : Groupe_even
-- ============================================================
CREATE TABLE Groupe_even (
    id_grp_evt  INT AUTO_INCREMENT,
    id_evt      INT NOT NULL,
    id_grp_etd  INT NOT NULL,
    PRIMARY KEY (id_grp_evt),
    FOREIGN KEY (id_evt)     REFERENCES Evenement(id_evt),
    FOREIGN KEY (id_grp_etd) REFERENCES Groupe_etud(id_grp_etd)
);

-- ============================================================
-- TABLE : Disponibilite
-- ============================================================
CREATE TABLE Disponibilite (
    id_dispo    INT AUTO_INCREMENT,
    heure_deb   TIME    NOT NULL,
    heure_fin   TIME    NOT NULL,
    date_       DATE    NOT NULL,
    id_evt      INT     NOT NULL,
    id_util     INT     NOT NULL,
    PRIMARY KEY (id_dispo),
    FOREIGN KEY (id_evt)  REFERENCES Evenement(id_evt),
    FOREIGN KEY (id_util) REFERENCES Utilisateur(id_util)
);

-- ============================================================
-- TABLE : Notification
-- ============================================================
CREATE TABLE Notification (
    id_not          INT AUTO_INCREMENT,
    type            VARCHAR(100)    NOT NULL,
    contenu         TEXT            NOT NULL,
    est_lu          BOOLEAN         NOT NULL DEFAULT FALSE,
    date_creation   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_tick         INT,    -- nul si notification concerne un ticket
    id_evt          INT,    -- nul si événement fait
    id_util         INT     NOT NULL,
    PRIMARY KEY (id_not),
    FOREIGN KEY (id_tick) REFERENCES Ticket(id_tick),
    FOREIGN KEY (id_evt)  REFERENCES Evenement(id_evt),
    FOREIGN KEY (id_util) REFERENCES Utilisateur(id_util)
);

-- ============================================================
-- Données initiales : Rôles
-- ============================================================
INSERT INTO Role (nom) VALUES
    ('Category Owner'),
    ('Issue Manager'),
    ('Course Leader'),
    ('Option Leader');

-- ============================================================
-- Données initiales : Types d'événements
-- ============================================================
INSERT INTO Type_even (nom) VALUES
    ('Reunion'),
    ('Audit'),
    ('Oral'),
    ('Sprint Review'),
    ('Soutenance');
