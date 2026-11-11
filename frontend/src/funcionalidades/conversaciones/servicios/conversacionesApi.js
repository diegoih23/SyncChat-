import { solicitar } from '../../../servicios/api.js';

export const listarConversaciones = () => solicitar('/conversaciones');

export const buscarContactos = (consulta) =>
  solicitar(`/auth/contactos/buscar?consulta=${encodeURIComponent(consulta)}`);

export const crearConversacion = (nombre) =>
  solicitar('/conversaciones', {
    method: 'POST',
    body: JSON.stringify({ nombre }),
  });

export const invitarPorCorreo = (conversacionId, correo) =>
  solicitar(`/conversaciones/${encodeURIComponent(conversacionId)}/invitaciones`, {
    method: 'POST',
    body: JSON.stringify({ correo }),
  });

export const listarInvitacionesPendientes = () => solicitar('/conversaciones/invitaciones/pendientes');

export const listarParticipantes = (conversacionId) =>
  solicitar(`/conversaciones/${encodeURIComponent(conversacionId)}/participantes`);

export const responderInvitacion = (conversacionId, aceptar) =>
  solicitar(`/conversaciones/invitaciones/${encodeURIComponent(conversacionId)}/${aceptar ? 'aceptar' : 'rechazar'}`, {
    method: 'POST',
  });

export const cancelarInvitacion = (conversacionId, correo) =>
  solicitar(`/conversaciones/${encodeURIComponent(conversacionId)}/invitaciones`, {
    method: 'DELETE',
    body: JSON.stringify({ correo }),
  });

export const obtenerConversacion = (id) => solicitar(`/conversaciones/${encodeURIComponent(id)}`);

export const eliminarConversacion = (id) =>
  solicitar(`/conversaciones/${encodeURIComponent(id)}`, { method: 'DELETE' });
