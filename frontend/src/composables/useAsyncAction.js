import { ref } from 'vue'

/**
 * Extrae un mensaje legible de cualquier error lanzado por la capa API.
 * @param {unknown} err
 * @param {string} fallback
 * @returns {string}
 */
export function getErrorMessage(err, fallback = 'Ocurrió un error inesperado') {
  return err?.response?.data?.error || err?.message || fallback
}

/**
 * Composable para acciones asíncronas: centraliza el patrón loading/error
 * que se repetía manualmente en cada vista.
 *
 * @example
 * const { loading, error, run } = useAsyncAction(() => usuarioAPI.getAll())
 * const data = await run()
 */
export function useAsyncAction(fn) {
  const loading = ref(false)
  const error = ref(null)

  /** Ejecuta la acción gestionando loading/error. Devuelve null si falla. */
  async function run(...args) {
    loading.value = true
    error.value = null
    try {
      return await fn(...args)
    } catch (err) {
      error.value = getErrorMessage(err)
      return null
    } finally {
      loading.value = false
    }
  }

  return { loading, error, run }
}
