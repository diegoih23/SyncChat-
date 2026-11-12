import { solicitar } from '../../../servicios/api.js';

export const listarMensajes = (conversacionId) => solicitar(`/mensajes/conversacion/${conversacionId}`);

export const enviarMensaje = (mensaje) => solicitar('/mensajes', {
  method: 'POST',
  body: JSON.stringify(mensaje)
});
