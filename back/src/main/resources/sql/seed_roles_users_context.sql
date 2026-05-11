-- Horizon seed script (MariaDB)
-- Objectif:
-- 1) Initialiser les roles statiques Spring Security
-- 2) Inserer des utilisateurs de test
-- 3) Inserer des relations contextuelles (owner/follower/creator/manager)

START TRANSACTION;

-- ============================================================
-- 1) Roles statiques (RBAC)
-- ============================================================
INSERT INTO roles (name)
SELECT 'STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'STUDENT');

INSERT INTO roles (name)
SELECT 'TEACHER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'TEACHER');

INSERT INTO roles (name)
SELECT 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

-- ============================================================
-- 2) Utilisateurs de test
-- ============================================================
-- IMPORTANT: remplace @pwd par un vrai hash BCrypt genere via PasswordEncoder.
-- Exemple Java: passwordEncoder.encode("password")
SET @pwd := '$2a$10$8J4J6Qx7rj1mc6Q8nRrN8e2fH4QxC1G8e3R2m9LrG8e6K9x2nGm3u';

INSERT INTO users (name, email, password, role_id)
SELECT 'Admin', 'admin@admin.fr', @pwd, r.id
FROM roles r
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'admin@admin.fr');

INSERT INTO users (name, email, password, role_id)
SELECT 'Prof Martin', 'teacher1@horizon.fr', @pwd, r.id
FROM roles r
WHERE r.name = 'TEACHER'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'teacher1@horizon.fr');

INSERT INTO users (name, email, password, role_id)
SELECT 'Dr Smith', 'teacher2@horizon.fr', @pwd, r.id
FROM roles r
WHERE r.name = 'TEACHER'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'teacher2@horizon.fr');

INSERT INTO users (name, email, password, role_id)
SELECT 'Alice Student', 'student1@horizon.fr', @pwd, r.id
FROM roles r
WHERE r.name = 'STUDENT'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'student1@horizon.fr');

INSERT INTO users (name, email, password, role_id)
SELECT 'Bob Student', 'student2@horizon.fr', @pwd, r.id
FROM roles r
WHERE r.name = 'STUDENT'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'student2@horizon.fr');

-- ============================================================
-- 3) Categories (Category Owner = categoryOwner)
-- ============================================================
INSERT INTO categories (name, status, category_owner, subject, created_at, updated_at)
SELECT 'Maintenance & Infrastructure', 'active', CAST(u.id AS CHAR), 'Maintenance', NOW(), NOW()
FROM users u
WHERE u.email = 'teacher1@horizon.fr'
  AND NOT EXISTS (SELECT 1 FROM categories c WHERE c.name = 'Maintenance & Infrastructure');

INSERT INTO categories (name, status, category_owner, subject, created_at, updated_at)
SELECT 'Scolarite & Administration', 'active', CAST(u.id AS CHAR), 'Scolarite', NOW(), NOW()
FROM users u
WHERE u.email = 'teacher2@horizon.fr'
  AND NOT EXISTS (SELECT 1 FROM categories c WHERE c.name = 'Scolarite & Administration');

-- Category followers (teacher qui suit une categorie)
INSERT INTO category_followers (teacher_id, category_id)
SELECT CAST(t.id AS CHAR), c.id
FROM users t
JOIN categories c ON c.name = 'Maintenance & Infrastructure'
WHERE t.email = 'teacher2@horizon.fr'
  AND NOT EXISTS (
    SELECT 1 FROM category_followers cf
    WHERE cf.teacher_id = CAST(t.id AS CHAR)
      AND cf.category_id = c.id
  );

-- ============================================================
-- 4) Issues (Issue Creator = creator_id ; Issue Manager = manager_id)
-- ============================================================
-- NOTE: manager_id n'existe pas encore dans ton schema actuel.
-- Ajoute cette colonne avant d'utiliser la partie manager.

INSERT INTO ticket (titre, description, statut, est_prive, id_cat, id_util, date_creation, date_modification)
SELECT 'Climatisation en panne',
       'La climatisation de l amphi A est en panne depuis lundi',
       'OUVERT',
       0,
       c.id,
       u.id,
       NOW(),
       NOW()
FROM categories c
JOIN users u ON u.email = 'student1@horizon.fr'
WHERE c.name = 'Maintenance & Infrastructure'
  AND NOT EXISTS (SELECT 1 FROM ticket t WHERE t.titre = 'Climatisation en panne');

INSERT INTO ticket (titre, description, statut, est_prive, id_cat, id_util, date_creation, date_modification)
SELECT 'Eclairage salle B12 defaillant',
       'Deux neons clignotent en continu dans la salle B12 depuis 3 jours',
       'OUVERT',
       0,
       c.id,
       u.id,
       NOW(),
       NOW()
FROM categories c
JOIN users u ON u.email = 'student1@horizon.fr'
WHERE c.name = 'Maintenance & Infrastructure'
  AND NOT EXISTS (
    SELECT 1 FROM ticket t
    WHERE t.titre = 'Eclairage salle B12 defaillant'
      AND t.id_cat = c.id
  );

INSERT INTO ticket (titre, description, statut, est_prive, id_cat, id_util, date_creation, date_modification)
SELECT 'Projecteur amphi C HS',
       'Le projecteur de l amphi C ne s allume plus pendant les cours du matin',
       'OUVERT',
       0,
       c.id,
       u.id,
       NOW(),
       NOW()
FROM categories c
JOIN users u ON u.email = 'student2@horizon.fr'
WHERE c.name = 'Maintenance & Infrastructure'
  AND NOT EXISTS (
    SELECT 1 FROM ticket t
    WHERE t.titre = 'Projecteur amphi C HS'
      AND t.id_cat = c.id
  );

INSERT INTO ticket (titre, description, statut, est_prive, id_cat, id_util, date_creation, date_modification)
SELECT 'Erreur sur mon releve de notes',
       'La note du devoir 2 n apparait pas sur mon releve semestriel',
       'OUVERT',
       0,
       c.id,
       u.id,
       NOW(),
       NOW()
FROM categories c
JOIN users u ON u.email = 'student1@horizon.fr'
WHERE c.name = 'Scolarite & Administration'
  AND NOT EXISTS (
    SELECT 1 FROM ticket t
    WHERE t.titre = 'Erreur sur mon releve de notes'
      AND t.id_cat = c.id
  );

INSERT INTO ticket (titre, description, statut, est_prive, id_cat, id_util, date_creation, date_modification)
SELECT 'Carte etudiante non recue',
       'Je n ai toujours pas recu ma carte etudiante alors que l inscription est validee',
       'OUVERT',
       0,
       c.id,
       u.id,
       NOW(),
       NOW()
FROM categories c
JOIN users u ON u.email = 'student2@horizon.fr'
WHERE c.name = 'Scolarite & Administration'
  AND NOT EXISTS (
    SELECT 1 FROM ticket t
    WHERE t.titre = 'Carte etudiante non recue'
      AND t.id_cat = c.id
  );

-- ============================================================
-- 5) Issue followers (ABAC relationnel)
-- ============================================================
-- Si la table n'existe pas, cree-la:
-- CREATE TABLE issue_followers (
--   issue_id BIGINT NOT NULL,
--   user_id BIGINT NOT NULL,
--   reason ENUM('INTERESTED','ABLE_TO_HELP','ALSO_IMPACTED') NOT NULL,
--   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--   PRIMARY KEY (issue_id, user_id),
--   CONSTRAINT fk_issue_followers_issue FOREIGN KEY (issue_id) REFERENCES ticket(id_tick),
--   CONSTRAINT fk_issue_followers_user FOREIGN KEY (user_id) REFERENCES users(id)
-- );

COMMIT;
