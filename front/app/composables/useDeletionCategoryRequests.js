export const useDeletionCategoryRequest = () => {
  const api = useApi()
  const basePath = '/deletion-category-requests'

  /**
   * Crée une demande de désinscription pour le follower connecté
   * @param {Number} categoryId ID de la catégorie
   * @returns {Promise<Object>} La demande créée
   */
  const createRequest = async (categoryId) => {
    return await api(`${basePath}/category/${categoryId}`, {
      method: 'POST'
    })
  }

  /**
   * Accepte une demande de désinscription (manager)
   * @param {Number} requestId ID de la requête à accepter
   * @returns {Promise<Object>} La demande acceptée
   */
  const acceptRequest = async (requestId) => {
    return await api(`${basePath}/${requestId}/accept`, {
      method: 'PUT'
    })
  }

  /**
   * Refuse une demande de désinscription avec un motif (manager)
   * @param {Number} requestId ID de la requête à refuser
   * @param {String} rejectionReason Motif du refus
   * @returns {Promise<Object>} La demande refusée
   */
  const rejectRequest = async (requestId, rejectionReason) => {
    return await api(`${basePath}/${requestId}/reject`, {
      method: 'PUT',
      body: { rejectionReason }
    })
  }

  /**
   * Récupère toutes les demandes en attente pour un manager
   * @param {Number} managerId ID du manager (owner de la catégorie)
   * @returns {Promise<Array>} Liste des demandes en attente
   */
  const getPendingRequestsForManager = async (managerId) => {
    return await api(`${basePath}/manager/${managerId}/pending`, {
      method: 'GET'
    })
  }

  /**
   * Vérifie si une demande est déjà en attente pour ce follower
   * @param {Number} categoryId ID de la catégorie
   * @returns {Promise<Boolean>} true si une demande est en attente
   */
  const checkPendingRequest = async (categoryId) => {
    return await api(`${basePath}/category/${categoryId}/has-pending`, {
      method: 'GET'
    })
  }

  return {
    createRequest,
    acceptRequest,
    rejectRequest,
    getPendingRequestsForManager,
    checkPendingRequest
  }
}
