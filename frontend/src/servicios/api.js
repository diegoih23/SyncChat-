const API_URL = import.meta.env.VITE_API_URL || '/api';

export async function solicitar(ruta, opciones = {}) {
  const token = localStorage.getItem('syncchat_token') || sessionStorage.getItem('syncchat_token');
  const respuesta = await fetch(`${API_URL}${ruta}`, {
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...opciones.headers },
    ...opciones
  });
  if (!respuesta.ok) {
    let mensaje = mensajePorEstado(respuesta.status);
    try {
      const detalle = await respuesta.json();
      mensaje = detalle.mensaje || detalle.message || detalle.detail || mensaje;
      if (detalle.campos && typeof detalle.campos === 'object') {
        const campo = Object.values(detalle.campos)[0];
        if (campo) mensaje = campo;
      }
    } catch {
      // Se conserva un mensaje amigable cuando la respuesta no contiene JSON.
    }
    const error = new Error(mensaje);
    error.status = respuesta.status;
    throw error;
  }
  return respuesta.status === 204 ? null : respuesta.json();
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
