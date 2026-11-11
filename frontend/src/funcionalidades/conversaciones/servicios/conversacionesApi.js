import { solicitar } from '../../../servicios/api.js';

export const listarConversaciones = () => solicitar('/conversaciones');

export const crearConversacion = (nombre) =>
  solicitar('/conversaciones', {
    method: 'POST',
    body: JSON.stringify({ nombre }),
  });

export const obtenerConversacion = (id) => solicitar(`/conversaciones/${encodeURIComponent(id)}`);

export const eliminarConversacion = (id) =>
  solicitar(`/conversaciones/${encodeURIComponent(id)}`, { method: 'DELETE' });
