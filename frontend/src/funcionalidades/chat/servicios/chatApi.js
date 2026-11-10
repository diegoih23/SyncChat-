export const crearConexionChat = (baseUrl = window.location.origin, token = obtenerToken()) => {
  if (!token) throw new Error('Debes iniciar sesión para conectarte al chat.');
  const ruta = `/ws/mensajes?token=${encodeURIComponent(token)}`;
  return new WebSocket(`${baseUrl.replace(/^http/, 'ws')}${ruta}`);
};

function obtenerToken() {
  return localStorage.getItem('syncchat_token') || sessionStorage.getItem('syncchat_token');
}
