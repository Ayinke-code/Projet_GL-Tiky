/**
 * Fonction qui vérifie la taille et le format
 * de plusieurs fichiers
 */

const MAX_SIZE = 10 * 1024 * 1024 // 10 Mo
const MAX_TOTAL_SIZE = 20 * 1024 * 1024 // 20 Mo total

const ALLOWED_TYPES = [
  'application/pdf',
  'image/png',
  'image/jpeg',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
]

export function validateFiles(files) {
  if (!files || files.length === 0) {
    return { valid: true }
  }

  let totalSize = 0

  for (const file of files) {
    if (file.size > MAX_SIZE) {
      return {
        valid: false,
        error: `Fichier trop volumineux: ${file.name}`
      }
    }

    totalSize += file.size

    if (!ALLOWED_TYPES.includes(file.type)) {
      return {
        valid: false,
        error: `Format non autorisé: ${file.name}`
      }
    }
  }

  if (totalSize > MAX_TOTAL_SIZE) {
    return {
      valid: false,
      error: `Taille totale des fichiers trop élevée (${(totalSize / 1024 / 1024).toFixed(1)} MB)`
    }
  }

  return { valid: true }
}