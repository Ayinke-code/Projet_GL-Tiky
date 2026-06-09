<template>
  <div class="p-8 pb-8 min-h-screen bg-gray-50">
    <div class="max-w-5xl mx-auto">

      <!-- EN-TÊTE -->
      <div class="flex justify-between items-center mb-8">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">Gestion des comptes</h1>
          <p class="text-sm text-gray-500 mt-1">Créez des comptes, ajustez les rôles ou supprimez un accès.</p>
        </div>
        <UButton icon="i-lucide-user-plus" color="info" @click="openCreate">
          Créer un compte
        </UButton>
      </div>

      <!-- BARRE D'OUTILS -->
      <div class="flex flex-col sm:flex-row gap-3 mb-6">
        <UInput
          v-model="search"
          icon="i-lucide-search"
          placeholder="Rechercher par nom ou email…"
          class="flex-1 max-w-md"
          color="info"
        />
        <div class="flex items-center gap-1 bg-white border border-gray-200 rounded-lg p-1 shadow-sm">
          <button
            v-for="f in ['TOUS', ...ALL_ROLES]"
            :key="f"
            class="px-3 py-1 text-xs font-semibold rounded-md transition"
            :class="roleFilter === f ? 'bg-slate-900 text-white' : 'text-gray-500 hover:bg-gray-100'"
            @click="roleFilter = f"
          >
            {{ f === 'TOUS' ? 'Tous' : roleLabel(f) }}
          </button>
        </div>
      </div>

      <!-- TABLEAU -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table class="w-full text-left">
          <thead class="bg-gray-50 text-gray-600 uppercase text-xs font-bold tracking-wider border-b border-gray-100">
            <tr>
              <th class="px-6 py-4">Utilisateur</th>
              <th class="px-6 py-4">Rôles</th>
              <th class="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>

          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="user in filteredUsers"
              :key="user.id"
              class="hover:bg-gray-50 transition"
            >
              <!-- Utilisateur -->
              <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                  <div
                    class="h-9 w-9 rounded-full flex items-center justify-center font-bold text-sm shrink-0"
                    :class="avatarClass(user)"
                  >
                    {{ initials(user.name) }}
                  </div>
                  <div>
                    <div class="font-semibold text-gray-900 leading-tight">{{ user.name }}</div>
                    <div class="text-xs text-gray-500">{{ user.email }}</div>
                  </div>
                </div>
              </td>

              <!-- Rôles -->
              <td class="px-6 py-4">
                <div class="flex flex-wrap gap-1.5">
                  <span
                    v-for="r in orderedRoles(user.roles)"
                    :key="r"
                    class="inline-flex items-center gap-1 text-[10px] font-bold uppercase px-2 py-1 rounded tracking-wide"
                    :class="badgeClass(r)"
                  >
                    <UIcon :name="roleIcon(r)" class="size-3" />{{ roleLabel(r) }}
                  </span>
                  <span
                    v-if="!user.roles?.length"
                    class="text-[10px] font-bold uppercase px-2 py-1 rounded bg-gray-100 text-gray-400 border border-dashed border-gray-300"
                  >
                    Aucun rôle
                  </span>
                </div>
              </td>

              <!-- Actions -->
              <td class="px-6 py-4 text-right whitespace-nowrap">
                <UButton variant="ghost" icon="i-lucide-sliders-horizontal" @click="openRoles(user)" color="info">
                  Rôles
                </UButton>
                <UButton color="error" variant="ghost" icon="i-lucide-trash-2" @click="confirmDelete(user)">
                  Supprimer
                </UButton>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="!filteredUsers.length" class="py-16 text-center text-gray-400">
          <UIcon name="i-lucide-user-x" class="size-8 mb-2" />
          <p class="text-sm">Aucun utilisateur trouvé</p>
        </div>
      </div>

      <p class="text-xs text-gray-400 mt-3">
        {{ filteredUsers.length }} compte(s) · Les rôles sont cumulables.
      </p>
    </div>

    <!-- ===================== MODALE : RÔLES ===================== -->
    <Modal v-model:open="roleModalOpen" title="Modifier les rôles">

      <!-- Utilisateur ciblé -->
      <div v-if="editing" class="flex items-center gap-3 pb-4 mb-4 border-b border-gray-100">
        <div
          class="h-9 w-9 rounded-full flex items-center justify-center font-bold text-sm shrink-0"
          :class="avatarClass(editing)"
        >
          {{ initials(editing.name) }}
        </div>
        <div>
          <div class="font-semibold text-gray-900">{{ editing.name }}</div>
          <div class="text-xs text-gray-500">{{ editing.email }}</div>
        </div>
      </div>

      <!-- Cases à cocher -->
      <div class="space-y-2 mb-5">
        <label
          v-for="r in ALL_ROLES"
          :key="r"
          class="flex items-center justify-between p-3 rounded-lg border cursor-pointer transition"
          :class="draftRoles.includes(r) ? 'border-indigo-300 bg-indigo-50/60' : 'border-gray-200 hover:bg-gray-50'"
        >
          <span class="flex items-center gap-3">
            <span class="h-8 w-8 rounded-lg flex items-center justify-center shrink-0" :class="badgeClass(r)">
              <UIcon :name="roleIcon(r)" class="size-4" />
            </span>
            <span>
              <span class="block text-sm font-semibold text-gray-900">{{ roleLabel(r) }}</span>
              <span class="block text-xs text-gray-500">{{ roleDesc(r) }}</span>
            </span>
          </span>
          <input
            v-model="draftRoles"
            type="checkbox"
            :value="r"
            class="h-4 w-4 rounded border-gray-300 text-indigo-600"
          />
        </label>
      </div>

      <!-- Actions -->
      <div class="flex justify-end gap-2">
        <UButton color="neutral" variant="ghost" @click="roleModalOpen = false">Annuler</UButton>
        <UButton :loading="saving" @click="saveRoles" color="info">Enregistrer</UButton>
      </div>
    </Modal>

    <!-- ===================== MODALE : CRÉER ===================== -->
    <Modal v-model:open="createModalOpen" title="Créer un compte">
      <div class="space-y-4">

        <UFormField label="Nom complet">
          <UInput v-model="form.name" placeholder="ex : Marie Lambert" class="w-full" color="info"/>
        </UFormField>

        <UFormField label="Adresse email">
          <UInput v-model="form.email" type="email" placeholder="prenom@ecole.fr" class="w-full" color="info"/>
        </UFormField>

        <UFormField label="Mot de passe">
            <UInput
                v-model="form.password"
                type="password"
                placeholder="Mot de passe du compte"
                class="w-full"
                color="info"
            />
        </UFormField>

        <div>
          <p class="text-sm font-medium text-gray-700 mb-2">
            Rôles <span class="text-gray-400 font-normal">(cumulables)</span>
          </p>
          <div class="grid grid-cols-3 gap-2">
            <label
              v-for="r in ALL_ROLES"
              :key="r"
              class="flex flex-col items-center text-center p-3 rounded-lg border cursor-pointer transition"
              :class="form.roles.includes(r) ? 'border-indigo-300 bg-indigo-50/60' : 'border-gray-200 hover:bg-gray-50'"
            >
              <input v-model="form.roles" type="checkbox" :value="r" class="sr-only" />
              <span class="h-8 w-8 rounded-lg flex items-center justify-center mb-1.5" :class="badgeClass(r)">
                <UIcon :name="roleIcon(r)" class="size-4" />
              </span>
              <span class="text-xs font-semibold text-gray-700">{{ roleLabel(r) }}</span>
            </label>
          </div>
          <p v-if="formError" class="text-xs text-red-500 mt-2">{{ formError }}</p>
        </div>

        <div class="flex justify-end gap-2 pt-1">
          <UButton color="neutral" variant="ghost" @click="createModalOpen = false">Annuler</UButton>
          <UButton icon="i-lucide-check" :loading="saving" @click="submitCreate" color="info">Créer le compte</UButton>
        </div>
      </div>
    </Modal>

    <!-- ===================== MODALE : SUPPRIMER ===================== -->
    <ConfirmModal
      v-model:open="deleteModalOpen"
      title="Supprimer le compte ?"
      :description="`Voulez-vous vraiment supprimer le compte de ${selectedUser?.name} ? Cette action est définitive.`"
      @confirm="deleteUserConfirmed"
    />
  </div>
</template>

<script setup>
import Modal from '~/components/ui/Modal.vue'
import ConfirmModal from '~/components/ui/ConfirmModal.vue'

const { users, fetchUser, createUser, updateUserRoles, deleteUser } = useUsers()
const toast = useToast()

/** Liste ordonnée de tous les rôles disponibles dans l'application. */
const ALL_ROLES = ['ADMIN', 'TEACHER', 'STUDENT']
/**
 * Correspondance entre le nom d'un rôle et son identifiant en base.
 * Utilisé pour construire le payload de création de compte.
 */
const ROLE_IDS = { ADMIN: 1, TEACHER: 2, STUDENT: 3 }

/** Terme saisi dans la barre de recherche (filtrage par nom ou email). */
const search = ref('')

/** Rôle sélectionné dans le filtre rapide ('TOUS' ou l'une des valeurs de ALL_ROLES). */
const roleFilter = ref('TOUS')

/** Indique qu'une requête est en cours (création ou mise à jour des rôles). */
const saving = ref(false)

// ---- État des modales ----
const roleModalOpen = ref(false)
const createModalOpen = ref(false)
const deleteModalOpen = ref(false)

/** Utilisateur en cours d'édition des rôles (modale "Modifier les rôles"). */
const editing = ref(null)

/**
 * Copie temporaire des rôles de l'utilisateur en cours d'édition.
 * Modifiée dans la modale sans impacter l'état réel tant que l'utilisateur
 * n'a pas validé.
 */
const draftRoles = ref([])

/** Utilisateur sélectionné pour suppression (transmis à la ConfirmModal). */
const selectedUser = ref(null)

/**
 * Données du formulaire de création de compte.
 * Réinitialisé à chaque ouverture de la modale via openCreate().
 */
const form = reactive({ name: '', email: '', password: '', roles: [] })

/** Message d'erreur affiché sous le formulaire de création (validation côté client ou erreur API). */
const formError = ref('')

/** Charge la liste des utilisateurs dès le montage du composant. */
onMounted(() => fetchUser())

/**
 * Liste des utilisateurs filtrés selon la recherche textuelle et le filtre de rôle actifs.
 * Recalculée automatiquement à chaque changement de `users`, `search` ou `roleFilter`.
 *
 * @type {import('vue').ComputedRef<Array>}
 */
const filteredUsers = computed(() =>
  (users.value ?? []).filter((u) => {
    const matchSearch =
      !search.value ||
      u.name.toLowerCase().includes(search.value.toLowerCase()) ||
      u.email.toLowerCase().includes(search.value.toLowerCase())
    const matchRole = roleFilter.value === 'TOUS' || u.roles?.includes(roleFilter.value)
    return matchSearch && matchRole
  })
)

// ---- helpers ----
const roleLabel = (r) => ({ ADMIN: 'Admin', TEACHER: 'Enseignant', STUDENT: 'Élève' }[r] ?? r)
const roleDesc = (r) => ({
  ADMIN: 'Gère les comptes et les matières',
  TEACHER: 'Traite les tickets et plannings',
  STUDENT: 'Pose des questions et suit les tickets'
}[r] ?? '')
const roleIcon = (r) => ({
  ADMIN: 'i-lucide-shield-check',
  TEACHER: 'i-lucide-presentation',
  STUDENT: 'i-lucide-graduation-cap'
}[r] ?? 'i-lucide-user')
const badgeClass = (r) => ({
  ADMIN: 'bg-slate-800 text-white',
  TEACHER: 'bg-indigo-100 text-indigo-700',
  STUDENT: 'bg-blue-100 text-blue-700'
}[r] ?? 'bg-gray-100 text-gray-600')

/**
 * Filtre et trie une liste de rôles selon l'ordre défini dans ALL_ROLES.
 * Garantit un affichage cohérent des badges quel que soit l'ordre reçu de l'API.
 *
 * @param {string[]} roles - Liste de rôles à trier
 * @returns {string[]} Rôles triés selon ALL_ROLES
 */
const orderedRoles = (roles) => ALL_ROLES.filter((r) => roles?.includes(r))
 
/**
 * Retourne les classes Tailwind de l'avatar (initiales) d'un utilisateur
 * en fonction de son rôle le plus prioritaire (ADMIN > TEACHER > STUDENT).
 *
 * @param {{ roles?: string[] }} user - Objet utilisateur
 * @returns {string} Classes CSS pour la couleur de l'avatar
 */
const avatarClass = (user) => {
  if (user.roles?.includes('ADMIN')) return 'bg-slate-800 text-white'
  if (user.roles?.includes('TEACHER')) return 'bg-indigo-100 text-indigo-600'
  if (user.roles?.includes('STUDENT')) return 'bg-blue-100 text-blue-600'
  return 'bg-gray-100 text-gray-500'
}

/**
 * Génère les initiales d'un utilisateur à partir de son nom complet (2 caractères max).
 * Exemple : 'Marie Lambert' → 'ML'
 *
 * @param {string} name - Nom complet de l'utilisateur
 * @returns {string} Initiales en majuscules
 */
const initials = (name) =>
  name.split(' ').map((p) => p[0]).join('').slice(0, 2).toUpperCase()

// ---- RÔLES ----
const openRoles = (user) => {
  editing.value = user
  draftRoles.value = [...(user.roles ?? [])]
  roleModalOpen.value = true
}

/**
 * Envoie la mise à jour des rôles à l'API, puis met à jour l'état local
 * sans rechargement complet de la liste.
 *
 * @returns {Promise<void>}
 */
const saveRoles = async () => {
  saving.value = true
  try {
    const roles = orderedRoles(draftRoles.value)
    await updateUserRoles(editing.value.id, roles)
    editing.value.roles = roles // mise à jour locale immédiate
    roleModalOpen.value = false
    toast.add({ title: 'Rôles mis à jour', description: editing.value.name, icon: 'i-lucide-check' })
  } catch {
    toast.add({ title: 'Erreur', description: 'Impossible de mettre à jour les rôles.', color: 'error' })
  } finally {
    saving.value = false
  }
}

// ---- CRÉATION ----
const openCreate = () => {
  form.name = ''
  form.email = ''
  form.password = ''       // ← reset
  form.roles = ['STUDENT']
  formError.value = ''
  createModalOpen.value = true
}

/**
 * Soumet le formulaire de création de compte.
 * Convertit les noms de rôles en IDs avant l'envoi (ex: 'ADMIN' → 1).
 * En cas de succès, insère l'utilisateur retourné par l'API dans la liste locale.
 *
 * @returns {Promise<void>}
 */
const submitCreate = async () => {
  if (!form.roles.length) {
    formError.value = 'Sélectionnez au moins un rôle.'
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      email: form.email.trim(),
      password: form.password,
      roleIds: orderedRoles(form.roles).map(r => ROLE_IDS[r])
    }
    const created = await createUser(payload)
    users.value = [...(users.value ?? []), created ?? { id: Date.now(), name: payload.name, email: payload.email, roles: orderedRoles(form.roles) }]
    createModalOpen.value = false
    toast.add({ title: 'Compte créé', description: payload.name, icon: 'i-lucide-check' })
  } catch {
    formError.value = 'Impossible de créer le compte.'
  } finally {
    saving.value = false
  }
}

// ---- SUPPRESSION ----
const confirmDelete = (user) => {
  selectedUser.value = user
  deleteModalOpen.value = true
}

/**
 * Appelée après confirmation dans la ConfirmModal.
 * Supprime l'utilisateur via l'API puis le retire de la liste locale.
 *
 * @returns {Promise<void>}
 */
const deleteUserConfirmed = async () => {
  try {
    await deleteUser(selectedUser.value.id)
    users.value = (users.value ?? []).filter((u) => u.id !== selectedUser.value.id)
    toast.add({ title: 'Compte supprimé', description: selectedUser.value.name, icon: 'i-lucide-trash-2' })
  } catch {
    toast.add({ title: 'Erreur', description: 'Suppression impossible.', color: 'error' })
  }
}
</script>