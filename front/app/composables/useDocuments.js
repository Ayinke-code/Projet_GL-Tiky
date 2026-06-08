export const useDocuments = () => {
    const api = useApi()

    /**
     * Télécharge le document demandé
     */
    const downloadFile = async (file) => {
        try {
            const blob = await api(`/documents/${file.id}/download`, {
                method: 'GET',
                responseType: 'blob'
            })

            const url = URL.createObjectURL(blob)

            const link = document.createElement('a')
            link.href = url
            link.download = file.originalName || 'file'
            document.body.appendChild(link)
            link.click()

            link.remove()
            URL.revokeObjectURL(url)

        } catch (e) {
            console.error(e)
        }
    }

    /**
     * Supprime un document donné
     */
    const deleteFile = async (file) => {
        try {
            const res = await api(`/documents/${file.id}`, {
                method: 'DELETE'
            })

        } catch (e) {
            console.error(e)
        }
    }

    return { downloadFile, deleteFile }
}
