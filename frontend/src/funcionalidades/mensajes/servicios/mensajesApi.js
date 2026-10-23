import { solicitar } from '../../../servicios/api.js';

export const listarMensajes = (conversacionId) => solicitar(`/mensajes?conversacionId=${conversacionId}`);
