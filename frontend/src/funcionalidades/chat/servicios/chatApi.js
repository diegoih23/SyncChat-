export const crearConexionChat = (baseUrl = window.location.origin) => new WebSocket(`${baseUrl.replace(/^http/, 'ws')}/api/chat/ws`);
