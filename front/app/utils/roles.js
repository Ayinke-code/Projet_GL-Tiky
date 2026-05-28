export const ROLE = Object.freeze({
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  STUDENT: 'STUDENT'
})

export const ROLE_PRIORITY = Object.freeze([
  ROLE.ADMIN,
  ROLE.TEACHER,
  ROLE.STUDENT
])

export const translateRole = (role) => {
  switch (role) {
    case ROLE.ADMIN:
      return "Admin"
    case ROLE.TEACHER:
      return "Professeur"
    case ROLE.STUDENT:
      return "Étudiant"
    default:
      return role
  }
}