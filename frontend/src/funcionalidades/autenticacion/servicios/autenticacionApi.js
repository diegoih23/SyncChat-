import { solicitar } from '../../../servicios/api.js';

export const iniciarSesion = (credenciales) => solicitar('/auth/login', { method: 'POST', body: JSON.stringify(credenciales) });
export const registrarUsuario = (datos) => solicitar('/auth/register', { method: 'POST', body: JSON.stringify(datos) });
export const comprobarCorreo = (correo) => solicitar(`/auth/check-email?correo=${encodeURIComponent(correo)}`);
export const solicitarRecuperacion = (correo) => solicitar('/auth/recuperar', { method: 'POST', body: JSON.stringify({ correo }) });
export const restablecerPassword = (datos) => solicitar('/auth/restablecer', { method: 'POST', body: JSON.stringify(datos) });
