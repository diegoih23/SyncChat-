import { solicitar } from '../../../servicios/api.js';

export const iniciarSesion = (credenciales) => solicitar('/auth/login', { method: 'POST', body: JSON.stringify(credenciales) });
