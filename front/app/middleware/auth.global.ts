export default defineNuxtRouteMiddleware((to) => {
  const token = useCookie<string | null>('auth_token')
  const publicPaths = ['/login']
  const isPublicPath = publicPaths.includes(to.path)

  if (to.path === '/') {
    return navigateTo('/login')
  }

  if (!token.value && !isPublicPath) {
    return navigateTo('/login')
  }
})
