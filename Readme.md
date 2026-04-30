# Projet Fullstack - Spring Boot + Nuxt + MariaDB

## Description

Application fullstack composée de :

- Backend : Spring Boot (Java 17, Maven) - API REST sécurisée JWT
- Frontend : Nuxt.js (Vue.js)
- Base de données : MariaDB (Docker)

Le backend et le frontend sont lancés en local pour faciliter le développement.  
Seule la base de données est conteneurisée.

---

## Architecture

/back → Spring Boot API (Java 17)
/front → Nuxt.js
/docker → MariaDB (Docker)
/docker-compose.yml

---

## Prérequis

- Docker
- Docker Compose
- Java 17+
- Maven
- Node.js + npm

---

## Lancer la base de données

```bash
docker-compose up --build

---

## Lancer le backend en local

cd back
mvn spring-boot:run

---

## Lancer le frontend en local

cd front
npm install
npm run dev

---

## Utilisateur admin

email: 'admin@admin.fr'
password: 'password'

---