import { computed } from 'vue'
import type { Ref } from 'vue'
import { useCookie, useState } from '#app'
import type { User } from '~/types'

const normalizeIssueRoles = (role: string, issueRoles?: string[] | null): string[] => {
  if (Array.isArray(issueRoles) && issueRoles.length > 0) {
    return issueRoles
  }

  if (role === 'STUDENT' || role === 'USER') {
    return ['PARTICIPANT']
  }

  return ['CREATOR', 'PARTICIPANT']
}

const normalizeUser = (storedUser: string | User | null | undefined): User | null => {
  if (!storedUser) {
    return null
  }

  const rawUser = typeof storedUser === 'string' ? JSON.parse(storedUser) : storedUser

  if (!rawUser || typeof rawUser !== 'object') {
    return null
  }

  const candidate = rawUser as Record<string, unknown>
  const followedCategories = Array.isArray(candidate.followedCategories)
    ? candidate.followedCategories.filter((categoryId): categoryId is number => typeof categoryId === 'number')
    : []
  const issueRoles = Array.isArray(candidate.issueRoles)
    ? candidate.issueRoles.filter((issueRole): issueRole is string => typeof issueRole === 'string')
    : null

  return {
    id: (candidate.id as string | number) ?? '',
    name: typeof candidate.name === 'string' ? candidate.name : '',
    email: typeof candidate.email === 'string' ? candidate.email : '',
    role: (typeof candidate.role === 'string' ? candidate.role : 'STUDENT') as User['role'],
    followedCategories,
    issueRoles: normalizeIssueRoles(typeof candidate.role === 'string' ? candidate.role : 'STUDENT', issueRoles)
  }
}

export const useAuth = () => {
  const userCookie = useCookie<string | null>('auth_user')

  // État partagé pour l'utilisateur, synchronisé avec le cookie de session.
  const user = useState<User | null>('user', () => normalizeUser(userCookie.value))

  const syncSession = (nextUser: User | null): void => {
    const normalizedUser = normalizeUser(nextUser)
    user.value = normalizedUser
    userCookie.value = normalizedUser ? JSON.stringify(normalizedUser) : null
  }

  const isAuthenticated = computed(() => !!user.value)
  const isUser = computed(() => user.value?.role === 'STUDENT' || user.value?.role === 'USER')
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

  const followCategory = (id: number): void => {
    if (user.value && !(user.value.followedCategories ?? []).includes(id)) {
      user.value.followedCategories = [...(user.value.followedCategories ?? []), id]
      userCookie.value = JSON.stringify(user.value)
    }
  }

  const unfollowCategory = (id: number): void => {
    if (user.value) {
      user.value.followedCategories = (user.value.followedCategories ?? []).filter(cid => cid !== id)
      userCookie.value = JSON.stringify(user.value)
    }
  }

  const setFollowedCategories = (ids: number[]): void => {
    if (user.value) {
      user.value.followedCategories = ids
      userCookie.value = JSON.stringify(user.value)
    }
  }

  const addIssueRole = (issueRole: string): void => {
    if (!user.value) {
      return
    }

    if (!(user.value.issueRoles ?? []).includes(issueRole)) {
      user.value.issueRoles = [...(user.value.issueRoles ?? []), issueRole]
      userCookie.value = JSON.stringify(user.value)
    }
  }

  const logout = (): void => {
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
