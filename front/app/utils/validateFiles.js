/**
 * Fonction qui vérifie la taille et le format
 * de plusieurs fichiers
 */

const MAX_SIZE = 10 * 1024 * 1024 // 10 Mo

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

  for (const file of files) {
    if (file.size > MAX_SIZE) {
      return {
        valid: false,
        error: `Fichier trop volumineux: ${file.name}`
      }
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      return {
        valid: false,
        error: `Format non autorisé: ${file.name}`
      }
    }
  }

  return { valid: true }
}