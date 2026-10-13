/**
 * Format and weight limits of uploaded files (RGESN 4.11).
 *
 * This module is the single place where the frontend knows the limits: the
 * labels displayed next to each upload field and the checks run before the
 * transfer are both derived from the constants below, so what the user reads
 * is always what is enforced.
 *
 * The backend enforces the same rules in UploadPolicy.java
 * (back/src/main/java/fr/horizon/backend_api/service): keep both in sync.
 */

const MEBIBYTE = 1024 * 1024

/** Maximum weight of one attached document. */
export const MAX_FILE_SIZE = 10 * MEBIBYTE

/** Maximum cumulated weight of the documents of one ticket or message. */
export const MAX_TOTAL_SIZE = 20 * MEBIBYTE

/** Maximum weight of a student import file: a class list is a few kilobytes of text. */
export const MAX_IMPORT_SIZE = 2 * MEBIBYTE

/** Allowed document extensions mapped to the MIME type the browser reports for them. */
const DOCUMENT_TYPES = {
  pdf: 'application/pdf',
  png: 'image/png',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
}

/** Extensions the student import can parse (legacy .xls is not supported by the backend). */
const IMPORT_EXTENSIONS = ['csv', 'xlsx']

const DOCUMENT_FORMATS_LABEL = 'PDF, PNG, JPG, DOCX, XLSX'
const IMPORT_FORMATS_LABEL = 'CSV, XLSX'

/** Value of the `accept` attribute for document upload fields. */
export const UPLOAD_ACCEPT = Object.keys(DOCUMENT_TYPES).map(ext => `.${ext}`).join(',')

/** Value of the `accept` attribute for the student import field. */
export const IMPORT_ACCEPT = IMPORT_EXTENSIONS.map(ext => `.${ext}`).join(',')

/** Text displayed next to document upload fields, before any transfer. */
export const UPLOAD_LIMITS_LABEL
  = `Formats acceptés : ${DOCUMENT_FORMATS_LABEL} — ${MAX_FILE_SIZE / MEBIBYTE} Mo max. par fichier, ${MAX_TOTAL_SIZE / MEBIBYTE} Mo au total`

/** Text displayed next to the student import field, before any transfer. */
export const IMPORT_LIMITS_LABEL
  = `Formats acceptés : ${IMPORT_FORMATS_LABEL} — ${MAX_IMPORT_SIZE / MEBIBYTE} Mo max.`

/**
 * Returns the lower-cased extension of a file name.
 *
 * @param {string} name - File name, e.g. "Rapport.PDF"
 * @returns {string} Extension without the dot ("pdf"), or an empty string when there is none
 */
function extensionOf(name) {
  const dot = (name ?? '').lastIndexOf('.')
  // dot <= 0 also covers dotfiles such as ".pdf", which have no real base name
  return dot > 0 ? name.slice(dot + 1).toLowerCase() : ''
}

/**
 * Checks the documents attached to a ticket or a message before they are sent.
 *
 * @param {File[]} files - Files selected by the user (may be null or empty)
 * @returns {{valid: boolean, error?: string}} `valid: false` with a displayable message on the first broken rule
 */
export function validateFiles(files) {
  if (!files || files.length === 0) {
    return { valid: true }
  }

  let totalSize = 0

  for (const file of files) {
    if (file.size > MAX_FILE_SIZE) {
      return {
        valid: false,
        error: `Fichier trop volumineux : ${file.name} (${MAX_FILE_SIZE / MEBIBYTE} Mo max. par fichier)`
      }
    }

    totalSize += file.size

    // Extension and reported type must agree, exactly like the backend check
    const expectedType = DOCUMENT_TYPES[extensionOf(file.name)]
    if (!expectedType || file.type !== expectedType) {
      return {
        valid: false,
        error: `Format non autorisé : ${file.name} (formats acceptés : ${DOCUMENT_FORMATS_LABEL})`
      }
    }
  }

  if (totalSize > MAX_TOTAL_SIZE) {
    return {
      valid: false,
      error: `Taille totale des fichiers trop élevée : ${(totalSize / MEBIBYTE).toFixed(1)} Mo (${MAX_TOTAL_SIZE / MEBIBYTE} Mo max. au total)`
    }
  }

  return { valid: true }
}

/**
 * Checks a student import file before it is sent.
 * Only the extension is checked: browsers report very different MIME types for
 * CSV files depending on the operating system.
 *
 * @param {File} file - File selected by the user
 * @returns {{valid: boolean, error?: string}} `valid: false` with a displayable message when the file is rejected
 */
export function validateImportFile(file) {
  if (!file) {
    return { valid: true }
  }

  if (!IMPORT_EXTENSIONS.includes(extensionOf(file.name))) {
    return {
      valid: false,
      error: `Format non autorisé : ${file.name} (formats acceptés : ${IMPORT_FORMATS_LABEL})`
    }
  }

  if (file.size > MAX_IMPORT_SIZE) {
    return {
      valid: false,
      error: `Fichier trop volumineux : ${file.name} (${MAX_IMPORT_SIZE / MEBIBYTE} Mo max.)`
    }
  }

  return { valid: true }
}
