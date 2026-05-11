/**
 * Utilitaires pour les avatars et les couleurs
 */

export const getInitials = (name: string): string => {
  if (!name) return '?'
  return name
    .split(' ')
    .map((n: string) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

export const getAvatarColor = (role?: string): string => {
  const roleUpper = role?.toUpperCase()
  if (roleUpper === 'ENSEIGNANT' || roleUpper === 'TEACHER') {
    return 'bg-blue-100 text-blue-700'
  }
  return 'bg-emerald-100 text-emerald-700'
}

export const getStatusColor = (status: string): string => {
  const statusMap: Record<string, string> = {
    'En cours': 'bg-blue-100 text-blue-700',
    'résolu': 'bg-green-100 text-green-700',
    'resolved': 'bg-green-100 text-green-700',
    'Ouvert': 'bg-yellow-100 text-yellow-700',
    'open': 'bg-yellow-100 text-yellow-700',
    'Fermé': 'bg-gray-100 text-gray-600',
    'closed': 'bg-gray-100 text-gray-600'
  }
  return statusMap[status] || 'bg-gray-100 text-gray-600'
}

export const getVisibilityColor = (visibility: string): string => {
  return visibility === 'Privé'
    ? 'border-blue-300 text-blue-600 bg-white'
    : 'border-red-300 text-red-600 bg-white'
}
