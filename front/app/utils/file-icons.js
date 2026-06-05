/**
 * Fonction qui renvoie une icone pour chaque
 * type de document dispo
 */

const FILE_ICONS = {

    // PDF
    'application/pdf': 'material-icon-theme:pdf',

    // Images
    'image/': 'lucide:image',

    // Word
    'application/msword': 'tabler:file-type-doc',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'tabler:file-type-docx',

    // Excel
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'mdi:file-excel-outline',

    default: 'lucide:file'
}

export function getFileIcon(type) {
  if (!type) return 'i-lucide-file'

  const key = Object.keys(FILE_ICONS).find(k => type.includes(k))
  return FILE_ICONS[key] || 'i-lucide-file'
}