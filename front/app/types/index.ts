/**
 * Types et interfaces centralisées pour l'application
 */

export interface User {
  id: string | number
  name: string
  email: string
  role: 'ADMIN' | 'ENSEIGNANT' | 'ETUDIANT' | 'TEACHER' | 'STUDENT' | 'USER'
  roles?: string[]
  followedCategories?: number[]
  issueRoles?: string[]
}

export interface Message {
  id: number
  auteur: string
  date: string
  contenu: string
  role?: 'ETUDIANT' | 'ENSEIGNANT'
}

export interface Category {
  idCat?: number
  id?: number
  nom: string
  name?: string
  description: string
  owner?: string | number
  status?: 'active' | 'archived'
  subject?: string
}

export interface Issue {
  id: number
  titre: string
  title?: string
  description: string
  auteur: string
  author?: string
  professeur: string
  teacher?: string
  abonnes: number
  statut: string
  status?: string
  visibilite?: 'Public' | 'Privé'
  tags: string[]
  categorie?: Category
}

export interface RDVProposal {
  active: boolean
  date: string
  auteur: string
  author?: string
}

export interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isUser: boolean
  isTeacher: boolean
  logout: () => void
  syncSession: (user: User | null) => void
}

export interface ApiResponse<T> {
  data?: T
  error?: string | null
  loading?: boolean
}
