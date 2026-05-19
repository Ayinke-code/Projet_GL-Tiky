export const useUsers = () => {
    const api = useApi()


    /**
     * État global des utilisateurs.
     * Contient la liste des utilisateurs récupérés via l’API.
     */
    const users = useState('users', () => null)


    /**
     * Récupère les utilisateurs filtrés par une liste de rôles.
     *
     * @param rolesList liste des rôles utilisés pour filtrer les utilisateurs
     * @return liste des utilisateurs correspondant aux rôles
     * @throws erreur API si la requête échoue
     */
    const fetchUserByRoles = async (rolesList) => {
        try {
            const res = await api('/users/byRoles', {
                method: 'POST',
                body: rolesList,
            });

            users.value = res
        } catch (err) {
            throw err
        }
    }


    return { users, fetchUserByRoles }
}