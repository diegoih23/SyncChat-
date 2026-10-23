const API_URL = import.meta.env.VITE_API_URL || '/api';

export async function solicitar(ruta, opciones = {}) {
  const respuesta = await fetch(`${API_URL}${ruta}`, {
    headers: { 'Content-Type': 'application/json', ...opciones.headers },
    ...opciones
  });
  if (!respuesta.ok) throw new Error(`Solicitud fallida: ${respuesta.status}`);
  return respuesta.status === 204 ? null : respuesta.json();
}
