const API_URL = import.meta.env?.VITE_API_URL || '/api';

export async function solicitar(ruta, opciones = {}) {
  const token = localStorage.getItem('syncchat_token') || sessionStorage.getItem('syncchat_token');
  const respuesta = await fetch(`${API_URL}${ruta}`, {
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...opciones.headers },
    ...opciones
  });
  const texto = await respuesta.text();
  let cuerpo = null;
  if (texto.trim()) {
    try {
      cuerpo = JSON.parse(texto);
    } catch {
      if (respuesta.ok) throw new Error('El servidor devolvió una respuesta no válida.');
    }
  }
  if (!respuesta.ok) {
    let mensaje = mensajePorEstado(respuesta.status);
    if (cuerpo && typeof cuerpo === 'object') {
      mensaje = cuerpo.mensaje || cuerpo.message || cuerpo.detail || mensaje;
      if (cuerpo.campos && typeof cuerpo.campos === 'object') {
        const campo = Object.values(cuerpo.campos)[0];
        if (campo) mensaje = campo;
      }
    }
    const error = new Error(mensaje);
    error.status = respuesta.status;
    throw error;
  }
  return cuerpo;
}

function mensajePorEstado(status) {
  const mensajes = {
    400: 'Revisa los datos ingresados.',
    401: 'Tu sesión no es válida o ha expirado.',
    403: 'No tienes permisos para realizar esta acción.',
    404: 'No encontramos la información solicitada.',
    409: 'La información ya está registrada.',
    429: 'Hay demasiadas solicitudes. Intenta nuevamente en unos minutos.',
    500: 'Ocurrió un problema en el servidor.',
    502: 'El servicio no está disponible en este momento.',
    503: 'El servicio no está disponible en este momento.'
  };
  return mensajes[status] || 'No fue posible completar la solicitud.';
}
