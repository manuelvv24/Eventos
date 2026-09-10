import { computed } from 'vue'

/**
 * Valida la fortaleza de una contraseña.
 *
 * Uso:
 *   const password = ref('')
 *   const { reglas, fortaleza, esValida } = usePasswordStrength(password)
 */
export function usePasswordStrength(passwordRef) {
  const reglas = computed(() => {
    const p = passwordRef.value || ''
    return [
      { key: 'longitud',   label: 'Mínimo 8 caracteres',              cumple: p.length >= 8 },
      { key: 'mayusculas', label: 'Una mayúscula y una minúscula',     cumple: /[A-Z]/.test(p) && /[a-z]/.test(p) },
      { key: 'numero',     label: 'Al menos un número',                cumple: /\d/.test(p) },
      { key: 'simbolo',    label: 'Al menos un símbolo (ej. !@#$%*)',  cumple: /[^A-Za-z0-9\s]/.test(p) },
    ]
  })

  /** Puntaje 0–5: reglas cumplidas + bono por longitud ≥ 12. */
  const puntaje = computed(() => {
    const p = passwordRef.value || ''
    if (!p) return 0
    const cumplidas = reglas.value.filter(r => r.cumple).length
    const bono = p.length >= 12 ? 1 : 0
    return Math.min(cumplidas + bono, 5)
  })

  const fortaleza = computed(() => {
    switch (puntaje.value) {
      case 0: return { nivel: 0, texto: '',                color: 'bg-surface-container-highest', textoClase: 'text-on-surface-variant' }
      case 1: return { nivel: 1, texto: 'Muy débil',       color: 'bg-error',                     textoClase: 'text-error' }
      case 2: return { nivel: 2, texto: 'Débil',           color: 'bg-warning',                   textoClase: 'text-warning' }
      case 3: return { nivel: 3, texto: 'Aceptable',       color: 'bg-yellow-500',                textoClase: 'text-yellow-600' }
      case 4: return { nivel: 4, texto: 'Fuerte',          color: 'bg-success',                   textoClase: 'text-success' }
      default:return { nivel: 5, texto: 'Muy fuerte',       color: 'bg-success',                   textoClase: 'text-success' }
    }
  })

  const esValida = computed(() => {
    const p = passwordRef.value || ''
    return p.length > 0 && reglas.value.every(r => r.cumple)
  })

  return { reglas, puntaje, fortaleza, esValida }
}
