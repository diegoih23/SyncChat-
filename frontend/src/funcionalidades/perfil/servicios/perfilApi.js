import { solicitar } from '../../../servicios/api.js';

export const obtenerPerfil = (id) => solicitar(`/auth/perfil/${encodeURIComponent(id)}`);
