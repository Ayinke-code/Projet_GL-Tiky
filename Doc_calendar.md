Voici une version ultra-condensée et directe pour votre `Doc_calendar.md`.

***

```markdown
# ⚙️ Doc Backend - Spring Boot (Calendrier)

## 1. Dépendances requises (`pom.xml`)
*   `spring-boot-starter-web` (Pour l'API REST)
*   `spring-boot-starter-data-jpa` (Pour la BDD)
*   `h2` (Base de données en mémoire pour tester)

## 2. Configuration (`application.properties`)
```properties
server.port=8080
spring.datasource.url=jdbc:h2:mem:calendardb
spring.jpa.hibernate.ddl-auto=update
```

## 3. Endpoints de l'API

| Méthode | URL | Description | Payload JSON (Exemple POST) |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/events` | Liste les événements | *Aucun* |
| **POST** | `/api/events` | Crée un événement | `{"title":"Dev","date":"2025-04-05","color":"bg-blue-500"}` |
```