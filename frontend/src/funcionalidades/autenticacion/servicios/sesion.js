const TOKEN = 'syncchat_token';
const USUARIO = 'syncchat_usuario';

export function guardarSesion(respuesta, recordar = false) {
  cerrarSesion();
  const almacenamiento = recordar ? localStorage : sessionStorage;
  almacenamiento.setItem(TOKEN, respuesta.token);
  almacenamiento.setItem(USUARIO, JSON.stringify(respuesta.usuario));
}

export function obtenerToken() {
  return localStorage.getItem(TOKEN) || sessionStorage.getItem(TOKEN);
}

export function obtenerUsuario() {
  const datos = localStorage.getItem(USUARIO) || sessionStorage.getItem(USUARIO);
  if (!datos) return null;
  try {
    return JSON.parse(datos);
  } catch {
    return null;
  }
}

export function cerrarSesion() {
  localStorage.removeItem(TOKEN); localStorage.removeItem(USUARIO);
  sessionStorage.removeItem(TOKEN); sessionStorage.removeItem(USUARIO);
}
