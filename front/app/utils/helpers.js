/**
 * Formate une date ISO (ou string date Java/Spring Boot) en format lisible FR.
 *
 * @param {string|Date} dateTime - Date au format ISO (ex: "2026-05-13T10:15:30") ou objet Date
 * @returns {string|null} Date formatée en "dd/mm/yyyy hh:mm" ou null si valeur invalide
 */
export function formatDateTime(dateTime) {
  if (!dateTime) return null

  const date = new Date(dateTime)

  return date.toLocaleString('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatDate(dateTime) {
  if (!dateTime) return null
  return new Date(dateTime).toLocaleDateString('fr-FR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  })
}

export function formatTime(dateTime) {
  if (!dateTime) return null
  return new Date(dateTime).toLocaleTimeString('fr-FR', {
    hour: '2-digit',
    minute: '2-digit'
  })
}