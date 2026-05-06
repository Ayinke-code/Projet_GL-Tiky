import { ref } from 'vue';

export interface Message {
  id: number;
  auteur: string;
  date: string;
  contenu: string;
  role?: 'ETUDIANT' | 'ENSEIGNANT';
}

export interface Issue {
  id: number;
  titre: string;
  description: string;
  auteur: string;
  professeur: string;
  abonnes: number;
  statut: string;
  tags: string[];
  categorie?: {
    idCat: number;
    nom: string;
    description: string;
  };
}

export const useApi = () => {
  const loading = ref(false);
  const error = ref<string | null>(null);
  const BASE_URL = 'http://localhost:8080/api';

  // Helper pour les cookies (remplace useCookie de Nuxt)
  const getCookie = (name: string) => {
    if (typeof document === 'undefined') return null;
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop()?.split(';').shift();
    return null;
  };

  const getHeaders = () => {
    const token = getCookie('auth_token');
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    return headers;
  };

  // Wrapper pour simuler $fetch
  const $fetch = async (url: string, options: any = {}) => {
    try {
      const response = await fetch(url, {
        ...options,
        headers: { ...getHeaders(), ...options.headers },
        body: options.body ? JSON.stringify(options.body) : undefined
      });

      if (!response.ok) {
        // Fallback for demo if backend is not running
        if (response.status === 404 || response.status === 403) return null;
        throw new Error('API Error');
      }
      return await response.json();
    } catch (err) {
      return null;
    }
  };

  const createIssue = async (issueData: any) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await $fetch(`${BASE_URL}/issues`, {
        method: 'POST',
        body: issueData
      });
      return response;
    } catch (err: any) {
      error.value = err.message || 'Erreur lors de la création';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const getIssues = async () => {
    loading.value = true;
    error.value = null;
    try {
      const response = await $fetch(`${BASE_URL}/issues`);
      return response || [];
    } catch (err: any) {
      error.value = err.message || 'Erreur lors du chargement';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const getIssueById = async (id: number): Promise<Issue> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await $fetch(`${BASE_URL}/issues/${id}`);
      if (!response) {
        return {
          id,
          titre: "Problème avec l'équation du second degré",
          description: "Bonjour,\n\nJe suis bloqué sur l'exercice 4...",
          auteur: "Élève Léo",
          professeur: "M. Dupont",
          abonnes: 4,
          statut: "En cours",
          tags: ["Mathématiques", "En cours", "Public"]
        };
      }
      return response;
    } catch (err: any) {
      error.value = err.message || 'Erreur lors du chargement';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const getIssueMessages = async (issueId: number): Promise<Message[]> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await $fetch(`${BASE_URL}/issues/${issueId}/messages`);
      if (!response) {
        return [
          {
            id: 1,
            auteur: "Élève Léo",
            role: "ETUDIANT",
            date: "2024-10-12T10:45:00Z",
            contenu: "Bonjour,\n\nJe suis bloqué sur l'exercice 4 de la page 42. J'ai bien calculé le discriminant ($\\\\Delta$), mais je trouve un résultat négatif (-16). D'après le cours, cela veut dire qu'il n'y a pas de solution, mais la question suivante demande de tracer la courbe.\n\nAi-je fait une erreur de calcul ou y a-t-il une astuce que je n'ai pas comprise ?"
          },
          {
            id: 2,
            auteur: "M. Dupont",
            role: "ENSEIGNANT",
            date: "2024-10-12T14:20:00Z",
            contenu: "Bonjour Léo,\n\nTon calcul du discriminant est correct, il est bien négatif ! Tu as bien appliqué le cours : l'équation f(x)=0 n'a pas de solution.\n\nCependant, cela ne t'empêche pas de tracer la courbe (la parabole). Le fait que $\\\\Delta$ < 0 signifie simplement que la courbe ne croise jamais l'axe des abscisses (l'axe horizontal). Calcule les coordonnées du sommet et prends quelques points au hasard pour la tracer."
          }
        ];
      }
      return response;
    } catch (err: any) {
      error.value = err.message || 'Erreur lors du chargement des messages';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const deleteIssue = async (id: number) => {
    loading.value = true;
    error.value = null;
    try {
      await $fetch(`${BASE_URL}/issues/${id}`, { method: 'DELETE' });
    } catch (err: any) {
      error.value = err.message || 'Erreur lors de la suppression';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const getCategories = async () => {
    loading.value = true;
    error.value = null;
    try {
      const response = await $fetch(`${BASE_URL}/categories`);
      return response || [];
    } catch (err: any) {
      error.value = err.message || 'Impossible de charger les catégories';
      return [];
    } finally {
      loading.value = false;
    }
  };

  const updateIssueStatus = async (id: number, statut: string) => {
    loading.value = true;
    error.value = null;
    try {
      return await $fetch(`${BASE_URL}/issues/${id}/status`, {
        method: 'PUT',
        body: { statut }
      });
    } catch (err: any) {
      error.value = err.message || 'Erreur lors de la mise à jour du statut';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const sendIssueMessage = async (issueId: number, content: string) => {
    loading.value = true;
    error.value = null;
    try {
      return await $fetch(`${BASE_URL}/issues/${issueId}/messages`, {
        method: 'POST',
        body: { content }
      });
    } catch (err: any) {
      error.value = err.message || 'Erreur lors de l’envoi';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    error,
    createIssue,
    getIssues,
    getIssueById,
    deleteIssue,
    getCategories,
    updateIssueStatus,
    getIssueMessages,
    sendIssueMessage
  };
};
