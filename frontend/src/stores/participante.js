import { defineStore } from 'pinia'
import { ref } from 'vue'
import participanteAPI from '../services/participanteAPI.js'

export const useParticipanteStore = defineStore('participante', () => {
  const items   = ref([])
  const loading = ref(false)
  const error   = ref(null)

  async function fetchAll() {
    loading.value = true; error.value = null
    try {
      const { data } = await participanteAPI.getAll()
      items.value = data
    } catch (e) {
      error.value = e.response?.data?.error || 'Error al cargar participantes'
    } finally { loading.value = false }
  }

  async function create(payload) {
    const { data } = await participanteAPI.create(payload)
    items.value.push(data)
    return data
  }

  async function remove(id) {
    await participanteAPI.remove(id)
    items.value = items.value.filter(p => p.idParticipantes !== id)
  }

  return { items, loading, error, fetchAll, create, remove }
})
