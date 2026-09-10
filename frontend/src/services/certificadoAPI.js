import api from './api.js'

export default {
  /** Genera el certificado para un registro y devuelve metadatos */
  generar:        (idRegistro)             => api.post(`/certificados/generar/${idRegistro}`),
  /** Descarga el PDF — devuelve blob con idPlantilla opcional */
  descargar:      (idRegistro, idPlantilla) => api.get(`/certificados/generar/${idRegistro}${idPlantilla ? '?idPlantilla=' + idPlantilla : ''}`, { responseType: 'blob' }),
  /** Descarga PDF por ID de certificado con idPlantilla opcional */
  descargarPorId: (idCert, idPlantilla)     => api.get(`/certificados/${idCert}/pdf${idPlantilla ? '?idPlantilla=' + idPlantilla : ''}`, { responseType: 'blob' }),
  /** Verificación pública — sin token */
  verificar:      (codigo)                 => api.get(`/public/verificar/${codigo}`),
  /** Certificados del usuario logueado */
  misCertificados: ()                       => api.get('/certificados/mis-certificados'),
  /** Regenera el PDF de un certificado existente con otra plantilla (solo admin/superadmin). El código no cambia */
  regenerar:       (idRegistro, idPlantilla) => api.post(`/certificados/regenerar/${idRegistro}${idPlantilla ? '?idPlantilla=' + idPlantilla : ''}`),
  /** Regenera en lote los certificados emitidos de un evento (solo admin/superadmin) */
  regenerarPorEvento: (idEvento, idPlantilla) => api.post(`/certificados/regenerar/evento/${idEvento}${idPlantilla ? '?idPlantilla=' + idPlantilla : ''}`),
}
