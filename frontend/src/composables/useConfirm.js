import { reactive } from 'vue'

/**
 * Estado global del diálogo de confirmación.
 * Solo debe renderizarse UN <AppConfirmDialog /> en el árbol (AppLayout).
 */
export const confirmState = reactive({
  open: false,
  title: '',
  message: '',
  confirmText: 'Confirmar',
  cancelText: 'Cancelar',
  danger: false,
  _resolve: null,
})

/**
 * Uso:
 *   const { confirm } = useConfirm()
 *   const ok = await confirm({ title: 'Eliminar', message: '...', danger: true })
 *   if (!ok) return
 */
export function useConfirm() {
  function confirm(options = {}) {
    const opts = typeof options === 'string' ? { message: options } : options
    const {
      title = '¿Estás seguro?',
      message = '',
      confirmText = 'Confirmar',
      cancelText = 'Cancelar',
      danger = false,
    } = opts

    if (confirmState._resolve) confirmState._resolve(false)

    confirmState.title = title
    confirmState.message = message
    confirmState.confirmText = confirmText
    confirmState.cancelText = cancelText
    confirmState.danger = danger
    confirmState.open = true

    return new Promise((resolve) => {
      confirmState._resolve = resolve
    })
  }

  function responder(valor) {
    confirmState.open = false
    if (confirmState._resolve) {
      confirmState._resolve(valor)
      confirmState._resolve = null
    }
  }

  /** Se llama cuando el modal se cierra sin responder (backdrop, X). */
  function alCerrarSinResponder() {
    if (confirmState._resolve) {
      confirmState._resolve(false)
      confirmState._resolve = null
    }
  }

  return { confirm, responder, alCerrarSinResponder, state: confirmState }
}
