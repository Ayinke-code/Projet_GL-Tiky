# Guide Complet des Catégories

## Qu'est-ce qu'une Catégorie ?

Une **catégorie** est un conteneur de regroupement de problèmes/issues signalés dans l'école. Elle permet aux utilisateurs d'organiser et de classer les signalements par domaine (ex: Sécurité, Pédagogie, Discrimination).

---

## Features Principales

### 1. **Création de Catégorie**
- Réservée aux **ADMIN** et **TEACHER**
- Les étudiants ne peuvent pas créer
- Champ requis : `name` (unique), `subject`
- Propriétaire : automatiquement l'utilisateur connecté
- État initial : `active`

### 2. **Consultation**
- Tous les rôles voient toutes les catégories actives
- Les propriétaires voient aussi leurs archives
- Affichage du propriétaire par nom lisible
- Accès en lecture seule pour les non-propriétaires

### 3. **Modification**
- Réservée au propriétaire de la catégorie
- Modification du `name`, `subject`
- Transfert possible vers un autre propriétaire
- Vérification de propriété côté backend

### 4. **Archivage**
- Masque la catégorie de la liste active
- Reste accessible dans "Archives"
- Permet le dés-archivage
- Utile pour une suppression "soft"

### 5. **Suivi (Follow/Unfollow)**
- Tous les utilisateurs peuvent suivre
- Place la catégorie dans "Mes Catégories & Suivies"
- Stockage dans `category_followers` (DB)
- Permet de filtrer par intérêt

### 6. **Suppression Définitive**
- Réservée aux catégories archivées
- Suppression irréversible
- Réservée au propriétaire
- Confirmation requise

### 7. **Gestion des Professeurs**
- Association de plusieurs profs à une catégorie
- Les profs associés ont accès en lecture
- Stocké en JSON : `teacher_ids`

---

## Schéma de Base de Données

### Table: `categories`

```sql
CREATE TABLE categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL,
  subject VARCHAR(255),
  status VARCHAR(255) DEFAULT 'active',           -- 'active' ou 'archived'
  category_owner VARCHAR(255) NOT NULL,           -- ID du propriétaire (String)
  teacher_ids JSON,                                -- Liste des profs ayant accès
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

-- Index pour les recherches rapides
CREATE INDEX idx_status ON categories(status);
CREATE INDEX idx_owner ON categories(category_owner);
```

### Table: `category_followers`

```sql
CREATE TABLE category_followers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  teacher_id VARCHAR(255) NOT NULL,              -- ID de l'utilisateur qui suit
  FOREIGN KEY (category_id) REFERENCES categories(id),
  UNIQUE KEY unique_follower (category_id, teacher_id)
);

-- Index pour les suivis par utilisateur
CREATE INDEX idx_follower ON category_followers(teacher_id);
```

---

## API REST Endpoints

### Récupérer les Catégories

```http
GET /api/categories/active
```
**Description**: Récupère toutes les catégories actives
**Auth**: Non requis (mais recommandé)
**Réponse**: Array de CategoryResponseDTO
```json
[
  {
    "id": 1,
    "name": "Sécurité",
    "subject": "Problèmes de sécurité",
    "status": "active",
    "owner": "3",
    "createdAt": "2026-05-09T11:25:45.372847",
    "updatedAt": "2026-05-09T11:25:45.372858"
  }
]
```

### Créer une Catégorie

```http
POST /api/categories
Content-Type: application/json
X-Teacher-ID: prof.martin@école.fr

{
  "name": "Pédagogie",
  "subject": "Problèmes pédagogiques"
}
```
**Auth**: Requis (TEACHER ou ADMIN)
**Réponse**: 201 Created avec CategoryResponseDTO

### Récupérer une Catégorie

```http
GET /api/categories/{id}
```
**Description**: Récupère les détails d'une catégorie
**Réponse**: CategoryResponseDTO complet

### Modifier une Catégorie

```http
PUT /api/categories/{id}
Content-Type: application/json

{
  "name": "Pédagogie Avancée",
  "subject": "Problèmes pédagogiques avancés",
  "categoryOwner": 2
}
```
**Auth**: Requis (propriétaire uniquement)
**Vérification**: Backend vérifie `user.id === category.categoryOwner`

### Archiver une Catégorie

```http
PATCH /api/categories/{id}/archive
```
**Auth**: Requis (propriétaire uniquement)
**Effet**: Change `status` de `active` à `archived`

### Désarchiver une Catégorie

```http
PATCH /api/categories/{id}/unarchive
```
**Auth**: Requis (propriétaire uniquement)
**Effet**: Change `status` de `archived` à `active`

### Supprimer Définitivement

```http
DELETE /api/categories/{id}
```
**Auth**: Requis (propriétaire uniquement)
**Condition**: `status` doit être `archived`
**Effet**: Suppression en cascade (cascade delete sur `category_followers`)

### Suivre une Catégorie

```http
POST /api/categories/{id}/follow
```
**Auth**: Requis
**Effet**: Crée une entrée dans `category_followers`
**Idempotent**: 2ème appel ne crée pas de doublon

### Dés-suivre une Catégorie

```http
DELETE /api/categories/{id}/follow
```
**Auth**: Requis
**Effet**: Supprime l'entrée de `category_followers`

### Récupérer les Catégories Suivies

```http
GET /api/categories/followed
```
**Auth**: Requis
**Réponse**: Array d'IDs de catégories suivies
```json
[1, 3, 5, 8]
```

### Ajouter un Professeur

```http
POST /api/categories/{id}/viewers/{teacherId}
```
**Auth**: Requis (propriétaire)
**Effet**: Ajoute `teacherId` à `teacher_ids` (JSON)

### Retirer un Professeur

```http
DELETE /api/categories/{id}/viewers/{teacherId}
```
**Auth**: Requis (propriétaire)
**Effet**: Retire `teacherId` de `teacher_ids` (JSON)

---

## Frontend Components

### `categorie.vue` - Page Principale

**Sections affichées** :
```
┌─────────────────────────────────────────────┐
│ Gestion des Catégories                      │
│ [Mode consultation - Vous pouvez suivre]    │  ← Si STUDENT
│                    [+ Créer une catégorie]  │  ← Si TEACHER/ADMIN
└─────────────────────────────────────────────┘

┌─ Mes Catégories & Suivies (Possédées + Suivies) ─┐
│ [Card] [Card] [Card]                             │
└───────────────────────────────────────────────────┘

┌─ Autres Catégories (Actives non-possédées) ─┐
│ [Card] [Card] [Card]                        │
└────────────────────────────────────────────┘

┌─ Archives (Catégories archivées) ─┐
│ [Card] [Card]                    │
└────────────────────────────────────┘
```

**Logic par Rôle** :

| Action | ADMIN | TEACHER | STUDENT |
|--------|-------|---------|---------|
| Voir toutes | ✅ | ✅ | ✅ |
| Créer | ✅ | ✅ | ❌ |
| Modifier sa catégorie | ✅ | ✅ | ❌ |
| Archiver | ✅ | ✅ | ❌ |
| Suivre | ✅ | ✅ | ✅ |
| Voir propriétaire | ✅ | ✅ | ✅ |

### `CategoryCard.vue` - Carte de Catégorie

**Props**:
```javascript
{
  category: Object,        // CategoryResponseDTO
  isFollowed: Boolean,     // True si l'utilisateur suit
  hideActions: Boolean     // Pour mode lecture seule
}
```

**Contenu**:
```
┌────────────────────────────┐
│ Sécurité                   │  ← subject
│ Problèmes de sécurité      │  ← name
│ Propriétaire: Dr. Smith    │  ← getOwnerName()
│                            │
│ [Suivre] ou [Dés-suivre]  │  ← Buttons
└────────────────────────────┘
```

### `CategoryForm.vue` - Formulaire Création

**Champs**:
- `name` (text, requis, unique)
- `subject` (text, requis)
- `teacherIds` (multi-select, optionnel)

**Events**:
- `@submit`: Émis après soumission
- `@cancel`: Fermé la modale

---

## Contrôle d'Accès (RBAC)

### Backend (Service Layer)

```java
// Vérification de propriété
if (!String.valueOf(category.getCategoryOwner()).equals(String.valueOf(user.getId()))) {
    throw new UnauthorizedException("Pas propriétaire");
}

// Vérification d'archive avant suppression
if (!"archived".equals(category.getStatus())) {
    throw new IllegalStateException("Doit être archivée");
}
```

### Frontend (Composition API)

```javascript
// Computed isStudent
const isStudent = computed(() => user.value?.role === 'STUDENT')

// Vérification de propriété
const canEdit = computed(() => 
  String(selectedCat.value?.owner) === String(user.value?.id)
)

// Affichage conditionnel
v-if="!isStudent && canEdit"  // Afficher boutons édition
```

---

## État d'une Catégorie

```
┌──────────┐
│ CREATED  │  (Status: active)
└────┬─────┘
     │ CREATE
     ▼
┌──────────┐
│ ACTIVE   │  ← Visible à tous
└────┬─────┘
     │ ARCHIVE
     │         UNARCHIVE
     │        ◄────────┐
     ▼                 │
┌──────────┐    ┌──────────┐
│ ARCHIVED │───►│ RE-ACTIVE│
└────┬─────┘    └──────────┘
     │ DELETE (permanent)
     │
     ▼
┌──────────┐
│ DELETED  │  (Complètement supprimée)
└──────────┘
```

---

## Flux Complet d'Utilisation

### Scénario: Prof Martin crée une catégorie

```
1. LOGIN
   prof.martin@école.fr → Token JWT avec role=TEACHER, id=2

2. FETCH CATEGORIES
   GET /api/categories/active
   → Récupère 8 catégories

3. CREATE CATEGORY
   POST /api/categories
   {
     "name": "Nouveau Sujet",
     "subject": "Détails du sujet"
   }
   → owner devient 2 (Prof Martin)

4. EDIT CATEGORY
   PUT /api/categories/9
   {
     "name": "Sujet Modifié",
     "subject": "Nouveaux détails"
   }
   → Backend vérifie: user.id (2) == category.owner (2) 

5. ADD PROFESSOR
   POST /api/categories/9/viewers/3
   → Ajoute Dr. Smith (3) à teacher_ids

6. ARCHIVE CATEGORY
   PATCH /api/categories/9/archive
   → Status: active → archived

7. DELETE CATEGORY (Optionnel)
   DELETE /api/categories/9
   → Suppression complète (admin only)
```

### Scénario: Alice (Étudiant) suit une catégorie

```
1. LOGIN
   alice.dupont@école.fr → Token JWT avec role=STUDENT, id=5

2. FETCH CATEGORIES
   GET /api/categories/active
   → Voit les 8 catégories

3. FOLLOW CATEGORY
   POST /api/categories/3/follow
   → category_followers: {category_id: 3, teacher_id: 5}

4. VIEW IN "MES CATÉGORIES"
   Section "Mes Catégories & Suivies" affiche catégorie 3

5. CLICK ON CATEGORY
   → Modale en lecture seule
   → Boutons: [Dés-suivre] [Fermer]
   → Pas de Modifier/Archiver/Supprimer
```

---

## Statistiques par Catégorie

**Données disponibles** :
- `id`: Identifiant unique
- `name`: Nom (unique)
- `subject`: Sujet/domaine
- `status`: État (active/archived)
- `owner`: ID du propriétaire
- `createdAt`: Date de création
- `updatedAt`: Dernière modification
- `teacher_ids`: JSON array d'IDs (professeurs)
- `followers_count`: Nombre de suivis (calculé)

---

## Validation & Erreurs

### Validation Backend

```
POST /api/categories
- name: NON NULL, UNIQUE, MAX 255 caractères
- subject: MAX 255 caractères
- owner: Automatiquement défini à user.id

PUT /api/categories/{id}
- Propriété vérifiée
- Status ne change pas ici (utiliser PATCH)

DELETE /api/categories/{id}
- Status MUST be "archived"
- Propriétaire seul
- Cascade delete sur followers
```

### Codes d'Erreur

| Code | Signification | Solution |
|------|---------------|----------|
| 400 | Données invalides | Vérifier champs requis |
| 401 | Non authentifié | Se connecter |
| 403 | Accès refusé | Vérifier propriété |
| 404 | Catégorie non trouvée | Vérifier ID |
| 409 | Conflit (ex: nom déjà pris) | Utiliser autre nom |

---

## 🔧 Points Techniques Clés

### JSON Storage (teacher_ids)

```java
@Column(columnDefinition = "JSON")
@JdbcTypeCode(SqlTypes.JSON)  // ← IMPORTANT: Force JSON, pas binaire
@Builder.Default
private List<String> teacherIds = new ArrayList<>();
```

**Stockage en DB** :
```sql
teacher_ids: ["2", "3", "4"]  -- JSON Array of strings
```

### Type Mismatch (Frontend)

```javascript
// BUGUÉ: Comparaison String vs Number
if (cat.owner === user?.id)  // "3" === 3 → false

// CORRECT: Conversion String
if (String(cat.owner) === String(user?.id))  // "3" === "3" → true
```

### Conditional Rendering

```vue
<!-- Admin/Teacher propriétaire: Boutons complets -->
<template v-if="!isStudent && String(cat.owner) === String(user?.id)">
  <button>Modifier</button>
  <button>Archiver</button>
  <button>Supprimer</button>
</template>

<!-- Étudiant: Bouton Fermer seul -->
<template v-else-if="isStudent">
  <button>Fermer</button>
</template>

<!-- Autres: Lecture seule -->
<div v-else>Lecture seule</div>
```

---

## Exemple Complet: Fetch + Affichage

### Frontend

```javascript
// 1. Importer le composable
import { useCategory } from '~/composables/useCategory'
import { useAuth } from '~/composables/useAuth'

// 2. Utiliser dans setup
const { categories, fetchCategories } = useCategory()
const { user } = useAuth()

// 3. Charger les données
onMounted(async () => {
  await fetchCategories()
})

// 4. Afficher conditionnellement
const ownedCategories = computed(() =>
  categories.value.filter(c => 
    String(c.owner) === String(user.value?.id)
  )
)

const followedCategories = computed(() =>
  categories.value.filter(c => 
    followedIds.value.includes(c.id)
  )
)
```

### Template

```vue
<div v-for="cat in ownedCategories" :key="cat.id">
  <h3>{{ cat.name }}</h3>
  <p>{{ cat.subject }}</p>
  <p>Propriétaire: {{ getOwnerName(cat.owner) }}</p>
  
  <!-- Actions pour propriétaire -->
  <button @click="editCategory(cat.id)">Modifier</button>
  <button @click="archiveCategory(cat.id)">Archiver</button>
</div>
```

---

## Relations Entre Entités

```
┌─────────────┐
│  Utilisateur│
│  (id: 1-7)  │
└────────┬────┘
         │ Crée
         ▼
┌──────────────────┐      ┌─────────────────────┐
│   Catégories     │◄─────│ category_followers  │
│  (id: 1-8)       │ Suivi│  (user follows cat) │
│  owner: user_id  │      │                     │
│  teacher_ids: [] │      └─────────────────────┘
└──────────────────┘
```

---

## Checklist de Vérification

- [ ] Catégories s'affichent pour tous les rôles
- [ ] Bouton "Créer" caché pour étudiants
- [ ] Modification réservée au propriétaire
- [ ] Noms des propriétaires affichés (pas d'IDs)
- [ ] Archive masque de la liste active
- [ ] Suivre/Dés-suivre fonctionne
- [ ] Type conversion `String(owner) === String(id)` utilisé
- [ ] `@JdbcTypeCode(SqlTypes.JSON)` présent
- [ ] Roles correctement mappés (STUDENT/TEACHER/ADMIN)

---

**Version**: 1.0  
**Last Updated**: Mai 2026  
**Auteur**: Horizon Dev Team 
