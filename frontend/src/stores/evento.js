import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import eventoAPI from '../services/eventoAPI.js'

export const useEventoStore = defineStore('evento', () => {
  const items   = ref([])
  const loading = ref(false)
  const error   = ref(null)

  const total = computed(() => items.value.length)

  async function fetchAll() {
    loading.value = true; error.value = null
    try {
      const { data } = await eventoAPI.getAll()
      items.value = data
    } catch (e) {
      error.value = e.response?.data?.error || 'Error al cargar eventos'
    } finally { loading.value = false }
  }

  async function create(payload) {
    const { data } = await eventoAPI.create(payload)
    items.value.push(data)
    return data
  }

  async function update(id, payload) {
    const { data } = await eventoAPI.update(id, payload)
    const idx = items.value.findIndex(e => e.idEventos === id)
    if (idx !== -1) items.value[idx] = data
    return data
  }

  async function cancel(id) {
    let dataRes = null
    try {
      const res = await eventoAPI.cancel(id)
      dataRes = res.data
    } catch (e) {
      const ev = items.value.find(e => Number(e.idEventos) === Number(id))
      if (ev) {
        dataRes = await update(id, { ...ev, estadoEvento: 'CANCELADO' })
      } else {
        throw e
      }
    }
    const idx = items.value.findIndex(e => Number(e.idEventos) === Number(id))
    if (idx !== -1) {
      items.value[idx] = { ...items.value[idx], ...(dataRes || {}), estadoEvento: 'CANCELADO' }
    }
    return dataRes
  }

  async function remove(id) {
    await eventoAPI.remove(id)
    items.value = items.value.filter(e => Number(e.idEventos) !== Number(id))
  }

  return { items, loading, error, total, fetchAll, create, update, cancel, remove }
})
