<template>
  <div></div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'blank'
})

const router = useRouter()
const token = useCookie('auth_token')

onMounted(() => {
  if (token.value) {
    router.push('/issues')
  } else {
    router.push('/login')
  }
})
</script>