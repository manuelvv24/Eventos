import api from './api.js'

export default {
  /**
   * Preview del CSV — no persiste nada.
   * @param {File}   archivo
   * @param {number} idEvento
   */
  preview(archivo, idEvento) {
    const form = new FormData()
    form.append('archivo', archivo)
    return api.post(`/importacion/preview?idEvento=${idEvento}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /**
   * Confirma la importación y persiste los participantes.
   */
  confirmar(archivo, idEvento) {
    const form = new FormData()
    form.append('archivo', archivo)
    return api.post(`/importacion/confirmar?idEvento=${idEvento}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  historial: ()    => api.get('/importacion/historial'),
  getById:   (id)  => api.get(`/importacion/${id}`),
}
