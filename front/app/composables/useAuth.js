import { useState } from '#app'

export const useAuth = () => {
  // État partagé pour l'utilisateur
  // En production, cela serait initialisé depuis un cookie ou un token JWT
  const user = useState('user', () => ({
    id: 'teacher-default',
    role: 'TEACHER', 
    name: 'Prof. Martin',
    followedCategories: [] 
  }))

  const isTeacher = computed(() => user.value?.role === 'TEACHER')
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  const followCategory = (id) => {
    if (user.value && !user.value.followedCategories.includes(id)) {
      user.value.followedCategories.push(id)
    }
  }

  const unfollowCategory = (id) => {
    if (user.value) {
      user.value.followedCategories = user.value.followedCategories.filter(cid => cid !== id)
    }
  }

  const setFollowedCategories = (ids) => {
    if (user.value) {
      user.value.followedCategories = ids
    }
  }

  const logout = () => {
    user.value = null
    navigateTo('/login')
  }

  return {
    user,
    isTeacher,
    isAdmin,
    followedCategories: computed(() => user.value?.followedCategories || []),
    followCategory,
    unfollowCategory,
    setFollowedCategories,
    logout
  }
}
