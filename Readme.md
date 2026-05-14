# Horizon

## Présentation

Horizon est une plateforme collaborative de gestion d’issues et de suivi communautaire entre professeurs et élèves.  
Le projet permet aux utilisateurs de créer, consulter et gérer des issues associées à des catégories thématiques.

L’objectif est de proposer une base moderne et évolutive avec :

- une architecture frontend/backend séparée,
- une authentification sécurisée,
- une API REST Spring Boot,
- une interface moderne sous Nuxt.

---

## Fonctionnalités principales

### Authentification
- Connexion utilisateur
- Gestion des rôles
- Sécurisation des endpoints via Spring Security + JWT

### Gestion des catégories
- Création de catégories
- Consultation des catégories
- Suppression de catégories
- Gestion des demandes de catégories

### Gestion des issues
- Création d’issues
- Consultation de toutes les issues
- Consultation d’une issue par ID
- Suppression d’une issue
- Mise à jour du statut d’une issue
- Gestion de la visibilité publique/privée

### Frontend
- Interface responsive avec Nuxt UI
- Gestion des loaders et états d’erreurs
- Modales de confirmation
- Composables centralisant les appels API

---

## Environnements

### Développement
- App DEV : https://172.24.1.16

### Production
- App PROD : https://172.24.1.32

### Qualité
- SonarQube DEV : https://172.24.1.1/sonar/tutorials?id=Team3_2026_DevBranch_Project
- SonarQube PROD : https://172.24.1.1/sonar/tutorials?id=Team3_2026_MasterBranch_Project

---

## Stack technique

### Frontend
- Nuxt 4.4.2
- Vue 3
- Nuxt UI 4.6.1
- TailwindCSS
- ESLint 10

### Backend
- Java 17
- Spring Boot 3.5.14
- Spring Security
- Spring Data JPA / Hibernate
- JWT (jjwt 0.11.5)
- Maven

### Base de données
- MariaDB
- H2 Database (tests)

### Tests
- JUnit 5
- Mockito
- Spring MockMvc
- JaCoCo (Sonar)

---

## CI/CD

Le projet utilise GitLab CI/CD afin d’automatiser :

- le build du backend Spring Boot,
- le build du frontend Nuxt,
- l’exécution des tests unitaires,
- l’analyse de qualité via SonarQube,
- le déploiement automatique sur les serveurs DEV et PROD.

### Pipeline

Le pipeline est découpé en plusieurs stages :

#### Build
- Compilation du backend avec Maven
- Build du frontend Nuxt
- Génération des artefacts

#### Tests
- Exécution des tests unitaires avec JUnit / Mockito
- Génération des rapports de tests

#### Déploiement
- Déploiement automatique sur les environnements DEV et PROD via SSH
- Synchronisation des fichiers frontend
- Déploiement du JAR Spring Boot
- Injection des variables d’environnement et certificats SSL

#### Analyse qualité
- Analyse du code avec SonarQube
- Vérification de la couverture de tests via JaCoCo

### Branches de déploiement

| Branche | Environnement |
|---|---|
| `dev` | Développement |
| `main` | Production |

---

## Installation

### 1. Cloner le projet

```bash
git clone https://gitlab-etu.openstack.etudis.eseo.fr/e4e/ld/projet-gl/2025-2026/horizon.git
cd project
```

---

### 2. Configurer les variables d'environnement

Copier les fichiers `.env.example` puis les renommer en `.env`.

Exemple :

```bash
cp .env.example .env
```

Configurer ensuite les variables nécessaires, notamment :

- accès MariaDB,
- JWT secret,
- ports backend/frontend,
- variables Nuxt.

---

### 3. Lancer la base de données

Démarrer MariaDB avec les identifiants renseignés dans le `.env` du backend.

Créer la base de données utilisée par le projet si nécessaire.

---

### 4. Lancer le backend

Se placer dans le dossier `back` :

```bash
cd back
```

Puis lancer Spring Boot :

```bash
mvn spring-boot:run
```

Le backend démarre par défaut sur :

```txt
http://localhost:8080
```

---

### 5. Lancer le frontend

Se placer dans le dossier `front` :

```bash
cd front
```

Installer les dépendances :

```bash
npm install
```

Puis lancer le serveur Nuxt :

```bash
npm run dev
```

Le frontend démarre par défaut sur :

```txt
http://localhost:3000
```

---

## Architecture du projet

```txt
project/
│
├── back/        # API Spring Boot
│
├── front/       # Application Nuxt 3
│
└── README.md
```

### Backend

Organisation principale :

```txt
controller/
service/
repository/
model/
security/
exception/
config/
```

### Frontend

Organisation principale :

```txt
pages/
components/
composables/
plugins/
utils/
```

---

## Scripts utiles

### Backend

Lancer le projet :

```bash
mvn spring-boot:run
```

Lancer les tests :

```bash
mvn test
```

Build du projet :

```bash
mvn clean install
```

---

### Frontend

Installation des dépendances :

```bash
npm install
```

Lancer en développement :

```bash
npm run dev
```

Build de production :

```bash
npm run build
```

Preview production :

```bash
npm run preview
```

---

## Équipe

Projet réalisé dans le cadre d’un projet collaboratif ESEO 2025-2026.

### Membres
- BATCHA Raoudate
- MANUERA VELA--BEYER Keryann
- SEHOUBO Nouchet Teddy Darius
- AMOUSSA Zaïtounath
- BAFFOUR Louis
- VAN ELSUVE Pierre