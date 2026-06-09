import { ref } from 'vue'
import en from '~/i18n/locales/en.json'
import fr from '~/i18n/locales/fr.json'

const messages = {
  en,
  fr
}

// shared reactive locale across imports
const locale = ref('fr')

function t(path) {
  if (!path) return ''
  const parts = path.split('.')
  let node = messages[locale.value]
  for (const p of parts) {
    if (!node) return path
    node = node[p]
  }
  return node ?? path
}

export function useLoginI18n() {
  return {
    locale,
    t,
    setLocale: (l) => { locale.value = l }
  }
}

