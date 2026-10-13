# Fiche de déploiement — Horizon

> Document de remise client — Plateforme collaborative de gestion d'issues
> **Équipe** : Team3 — ESEO 2025-2026
> **Version du document** : 1.0 — 10/06/2026

---

## 1. Présentation

**Tiky** est une plateforme collaborative de gestion d'issues et de suivi
communautaire entre professeurs et élèves. L'application repose sur une
architecture **frontend / backend séparée** :

- une **API REST** Spring Boot (Java) ;
- une **interface web** Nuxt (Vue 3), servie en fichiers statiques ;
- une base de données **MariaDB**.

---

## 2. Architecture de déploiement

L'application est déployée **sans Docker** : le backend est livré sous forme de
fichier exécutable `.jar` et le frontend sous forme de fichiers statiques.
Un serveur **Apache** assure la façade HTTPS, sert le frontend et relaie les
appels API vers le backend.

```
                    Internet / réseau ESEO
                            │  HTTPS (443)
                            ▼
              ┌──────────────────────────────┐
              │           Apache 2           │
              │  ─ Redirection 80 → 443      │
              │  ─ Sert le front statique    │  /var/www/html
              │  ─ Reverse-proxy /api ───────┼──┐
              └──────────────────────────────┘  │ http://localhost:8081/api
                                                 ▼
                                   ┌───────────────────────────┐
                                   │  Backend Spring Boot      │
                                   │  java -jar app.jar (8081) │
                                   └─────────────┬─────────────┘
                                                 │ JDBC (3306)
                                                 ▼
                                       ┌───────────────────┐
                                       │     MariaDB       │
                                       └───────────────────┘
```

---

## 3. Poids de l'application

Éléments réellement transférés et installés sur le serveur de production :

| Artefact | Taille | Description |
|---|---|---|
| Backend — `app.jar` | **86,2 Mo** (82,2 Mio) | Fat-jar Spring Boot autonome : serveur Tomcat embarqué + toutes les dépendances. *(86 219 205 octets)* |
| Frontend — fichiers statiques | **0,98 Mo** (0,94 Mio) | Build Nuxt (`/.output/public`), 18 fichiers. |
| **Empreinte totale déployée** | **≈ 87 Mo** | Hors base de données. |

> La base de données MariaDB et les documents téléversés par les utilisateurs
> (dossier `uploads`) viennent s'ajouter à cette empreinte et croissent avec
> l'usage.

---

## 4. Volume de code

| Composant | Fichiers | Lignes de code |
|---|---|---|
| Backend (Java) | 209 fichiers — 152 applicatifs + 57 de test | ~24 900 |
| Frontend (`.vue` / `.js` / `.ts`) | 89 fichiers — dont 64 composants `.vue` | ~15 500 |
| **Total** | **~298 fichiers** | **~40 400 lignes** |

---

## 5. Stack technique

### Frontend
- Nuxt 4.4.2 / Vue 3
- Nuxt UI 4.6.1 + TailwindCSS
- STOMP/WebSocket (temps réel)
- FullCalendar (agenda)

### Backend
- Java 17 / Spring Boot 3.5.14
- Spring Security + JWT (jjwt 0.11.5)
- Spring Data JPA / Hibernate
- WebSocket (notifications temps réel)
- Apache POI / OpenCSV (import Excel/CSV)

### Base de données
- MariaDB (production)
- H2 (tests automatisés uniquement)

### Qualité & tests
- JUnit 5, Mockito, Spring MockMvc
- Selenium (tests end-to-end)
- JaCoCo + SonarQube (couverture et qualité du code)

---

## 6. Prérequis du serveur de production

| Élément | Détail |
|---|---|
| **Java** | JRE 17 (ou supérieur compatible) — nécessaire pour exécuter `app.jar` |
| **MariaDB** | Version 10.x, base et utilisateur dédiés |
| **Apache 2** | Modules requis : `proxy`, `proxy_http`, `ssl`, `headers`, `rewrite` |
| **Certificat SSL** | `server.crt` + `server.key` (HTTPS obligatoire) |
| **Réseau** | Ports **80** (redirigé) et **443** (public) ouverts ; port **8081** interne pour le backend |
| **Mémoire** | Prévoir ~512 Mo à 1 Go de RAM pour la JVM *(estimation usuelle, à ajuster selon la charge réelle)* |
| **Stockage** | ~100 Mo pour l'application + espace pour la base et les documents téléversés |

### Protocole HTTP/2 (RGESN 6.7)

Toutes les ressources (HTML, CSS, JS, images, polices, réponses de l'API) sont
servies en **HTTP/2** sur le port 443 : directive `Protocols h2 http/1.1` dans
`apache-site-dev.conf` / `apache-site-prod.conf`, module Apache `http2` activé
par le pipeline. HTTP/1.1 n'est conservé qu'en repli (anciens clients et
ouverture des WebSockets).

HTTP/2 exige un MPM Apache multi-thread (`event` ou `worker`) : avec
`mpm_prefork`, la directive est ignorée et le serveur reste en HTTP/1.1.

Vérification après déploiement (réponse attendue : `HTTP/2 200`) :

```bash
apache2ctl -M | grep -E 'mpm_|http2'
curl -skI --http2 https://<serveur>/ | head -1
```

---

## 7. Procédure de déploiement

Le déploiement est **automatisé** via le pipeline GitLab CI/CD. Un push sur la
branche `main` déclenche la mise en production ; un push sur `dev` met à jour
l'environnement de développement.

Étapes exécutées automatiquement par le pipeline :

1. **Build backend** — `mvn clean package` génère `app.jar`.
2. **Build frontend** — `npm run build` génère les fichiers statiques.
3. **Tests** — tests unitaires (JUnit) puis tests end-to-end (Selenium).
4. **Déploiement** (via SSH) :
   - transfert du `.jar` vers `~/app/backend/app.jar` ;
   - synchronisation du frontend vers `/var/www/html/` ;
   - mise en place de la configuration Apache et des certificats SSL ;
   - redémarrage du backend (`java -jar app.jar`).
5. **Analyse qualité** — rapport SonarQube.

> Aucune intervention manuelle n'est requise sur le serveur en fonctionnement
> nominal. Le backend tourne comme processus Java (relancé automatiquement à
> chaque déploiement).

---

## 8. Configuration (variables d'environnement)

Le backend se configure via un fichier `.env` (transmis de façon sécurisée par
la CI). Principales variables :

| Variable | Rôle |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Connexion MariaDB |
| `SERVER_PORT` | Port d'écoute du backend (**8081** en production) |
| `SERVER_CONTEXT_PATH` | Préfixe de l'API (`/api`) |
| `JWT_SECRET`, `JWT_EXPIRATION` | Sécurité des jetons d'authentification |
| `JPA_DDL_AUTO` | Stratégie de mise à jour du schéma de base |
| `PATH_TO_DOCUMENTS` | Dossier de stockage des documents téléversés |

Côté frontend, l'URL de l'API (`NUXT_PUBLIC_API_BASE`) est injectée au moment du
build selon l'environnement.

---

## 9. Environnements

| Environnement | URL | Branche |
|---|---|---|
| Développement | https://172.24.1.16 | `dev` |
| Production | https://172.24.1.32 | `main` |

Tableaux de bord qualité (SonarQube) :
- DEV : https://172.24.1.1/sonar/tutorials?id=Team3_2026_DevBranch_Project
- PROD : https://172.24.1.1/sonar/tutorials?id=Team3_2026_MasterBranch_Project

---

## 10. Conformité — licences tierces

Les rapports d'attribution des dépendances open-source utilisées sont fournis
dans le dossier [`docs/licenses/`](licenses/) :

- `THIRD-PARTY.txt` — dépendances backend (Java/Maven) ;
- `licences-front.csv` — dépendances frontend (Node/npm).

---

*Document créé pour la remise client. Les tailles d'artefacts correspondent au
dernier build de référence (10/06/2026) et peuvent légèrement varier d'une
compilation à l'autre.*
