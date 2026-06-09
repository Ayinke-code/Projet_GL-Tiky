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

    /**
     * Récupère les utilisateurs filtrés par une liste de rôles.
     *
     * @return liste des utilisateurs correspondant aux rôles
     * @throws erreur API si la requête échoue
     */
    const fetchUser = async () => {
        try {
            const res = await api('/users');

            users.value = res

            return res
        } catch (err) {
            throw err
        }
    }

    /**
     * Crée un nouveau compte utilisateur.
     *
     * @param payload objet contenant { name, email, roles }
     * @return l'utilisateur créé retourné par l'API
     * @throws erreur API si la requête échoue
     */
    const createUser = async (payload) => {
        console.log("payload : ", payload)
        try {
            const res = await api('/users', {
                method: 'POST',
                body: payload,
            })
 
            return res
        } catch (err) {
            throw err
        }
    }
 
    /**
     * Met à jour les rôles d'un utilisateur existant.
     *
     * @param id identifiant de l'utilisateur
     * @param roles nouvelle liste de rôles à appliquer
     * @return la réponse de l'API
     * @throws erreur API si la requête échoue
     */
    const updateUserRoles = async (id, roles) => {
        try {
            const res = await api(`/users/${id}/roles`, {
                method: 'POST',
                body: { roles },
            })
 
            return res
        } catch (err) {
            throw err
        }
    }
 
    /**
     * Supprime un compte utilisateur.
     *
     * @param id identifiant de l'utilisateur à supprimer
     * @return la réponse de l'API
     * @throws erreur API si la requête échoue
     */
    const deleteUser = async (id) => {
        try {
            const res = await api(`/users/${id}`, {
                method: 'DELETE',
            })
 
            return res
        } catch (err) {
            throw err
        }
    }
 
    return { users, fetchUser, fetchUserByRoles, createUser, updateUserRoles, deleteUser }
}