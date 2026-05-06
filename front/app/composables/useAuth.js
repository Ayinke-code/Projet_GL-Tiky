import { computed } from 'vue'
import { useCookie, useState } from '#app'

const normalizeIssueRoles = (role, issueRoles) => {
  if (Array.isArray(issueRoles) && issueRoles.length > 0) {
    return issueRoles
  }

  if (role === 'USER') {
    return ['PARTICIPANT']
  }

  return ['CREATOR', 'PARTICIPANT']
}

const normalizeUser = (storedUser) => {
  if (!storedUser) {
    return null
  }

  const rawUser = typeof storedUser === 'string' ? JSON.parse(storedUser) : storedUser

  if (!rawUser) {
    return null
  }

  return {
    id: rawUser.id ?? null,
    name: rawUser.name ?? '',
    email: rawUser.email ?? '',
    role: rawUser.role ?? 'USER',
    followedCategories: Array.isArray(rawUser.followedCategories) ? rawUser.followedCategories : [],
    issueRoles: normalizeIssueRoles(rawUser.role ?? 'USER', rawUser.issueRoles)
  }
}

export const useAuth = () => {
  const userCookie = useCookie('auth_user')

  // État partagé pour l'utilisateur, synchronisé avec le cookie de session.
  const user = useState('user', () => normalizeUser(userCookie.value))

  const syncSession = (nextUser) => {
    const normalizedUser = normalizeUser(nextUser)
    user.value = normalizedUser
    userCookie.value = normalizedUser
  }

  const isAuthenticated = computed(() => !!user.value)
  const isUser = computed(() => user.value?.role === 'USER')
  const isTeacher = computed(() => user.value?.role === 'TEACHER')
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isIssueCreator = computed(() => {
    return user.value?.issueRoles?.includes('CREATOR') || isTeacher.value || isAdmin.value
  })
  const isIssueFollower = computed(() => user.value?.issueRoles?.includes('FOLLOWER'))
  const isParticipant = computed(() => user.value?.issueRoles?.includes('PARTICIPANT') || isAuthenticated.value)

  const canAccessCategories = computed(() => isAuthenticated.value)
  const canAccessIssues = computed(() => isAuthenticated.value)
  const canCreateIssue = computed(() => isAuthenticated.value)

  const followCategory = (id) => {
    if (user.value && !user.value.followedCategories.includes(id)) {
      user.value.followedCategories.push(id)
      userCookie.value = user.value
    }
  }

  const unfollowCategory = (id) => {
    if (user.value) {
      user.value.followedCategories = user.value.followedCategories.filter(cid => cid !== id)
      userCookie.value = user.value
    }
  }

  const setFollowedCategories = (ids) => {
    if (user.value) {
      user.value.followedCategories = ids
      userCookie.value = user.value
    }
  }

  const addIssueRole = (issueRole) => {
    if (!user.value) {
      return
    }

    if (!user.value.issueRoles.includes(issueRole)) {
      user.value.issueRoles.push(issueRole)
      userCookie.value = user.value
    }
  }

  const logout = () => {
    user.value = null
    userCookie.value = null
    navigateTo('/login')
  }

  return {
    user,
    isAuthenticated,
    isUser,
    isTeacher,
    isAdmin,
    isIssueCreator,
    isIssueFollower,
    isParticipant,
    canAccessCategories,
    canAccessIssues,
    canCreateIssue,
    followedCategories: computed(() => user.value?.followedCategories || []),
    issueRoles: computed(() => user.value?.issueRoles || []),
    followCategory,
    unfollowCategory,
    setFollowedCategories,
    syncSession,
    addIssueRole,
    logout
  }
}
