import { useCallback, useEffect, useRef, useState } from 'react';
import {
  crearConversacion,
  cancelarInvitacion,
  eliminarConversacion,
  listarConversaciones,
  listarInvitacionesPendientes,
  obtenerConversacion,
  invitarPorCorreo,
  responderInvitacion,
} from '../servicios/conversacionesApi.js';

const MENSAJE_CARGA = 'No fue posible cargar tus conversaciones.';
const MENSAJE_CREAR = 'No fue posible crear la conversación.';
const MENSAJE_ELIMINAR = 'No fue posible eliminar la conversación.';

export function useConversaciones() {
  const [conversaciones, setConversaciones] = useState([]);
  const [invitaciones, setInvitaciones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [creando, setCreando] = useState(false);
  const [eliminandoId, setEliminandoId] = useState(null);
  const [error, setError] = useState('');
  const montado = useRef(true);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const [datos, pendientes] = await Promise.all([listarConversaciones(), listarInvitacionesPendientes()]);
      if (montado.current) {
        setConversaciones(Array.isArray(datos) ? datos : []);
        setInvitaciones(Array.isArray(pendientes) ? pendientes : []);
      }
    } catch (exception) {
      if (montado.current) setError(exception.message || MENSAJE_CARGA);
    } finally {
      if (montado.current) setCargando(false);
    }
  }, []);

  useEffect(() => {
    montado.current = true;
    cargar();
    return () => {
      montado.current = false;
    };
  }, [cargar]);

  const sincronizar = useCallback(async () => {
    try {
      const [datos, pendientes] = await Promise.all([listarConversaciones(), listarInvitacionesPendientes()]);
      if (montado.current) {
        setConversaciones(Array.isArray(datos) ? datos : []);
        setInvitaciones(Array.isArray(pendientes) ? pendientes : []);
      }
    } catch {
      // Conserva los datos visibles y vuelve a sincronizar en el siguiente ciclo.
    }
  }, []);

  useEffect(() => {
    const alVolverVisible = () => {
      if (document.visibilityState === 'visible') void sincronizar();
    };
    const intervalo = window.setInterval(alVolverVisible, 4000);
    window.addEventListener('focus', alVolverVisible);
    document.addEventListener('visibilitychange', alVolverVisible);
    return () => {
      window.clearInterval(intervalo);
      window.removeEventListener('focus', alVolverVisible);
      document.removeEventListener('visibilitychange', alVolverVisible);
    };
  }, [sincronizar]);

  const crear = useCallback(async (nombre) => {
    setCreando(true);
    setError('');
    try {
      const nueva = await crearConversacion(nombre);
      setConversaciones((actuales) => [nueva, ...actuales.filter((item) => item.id !== nueva.id)]);
      return nueva;
    } catch (exception) {
      setError(exception.message || MENSAJE_CREAR);
      throw exception;
    } finally {
      setCreando(false);
    }
  }, []);

  const invitar = useCallback(async (conversacionId, correo) => {
    setError('');
    try {
      await invitarPorCorreo(conversacionId, correo);
      return true;
    } catch (exception) {
      setError(exception.message || 'No fue posible enviar la invitación.');
      throw exception;
    }
  }, []);

  const cancelar = useCallback(async (conversacionId, correo) => {
    setError('');
    try {
      await cancelarInvitacion(conversacionId, correo);
      await cargar();
    } catch (exception) {
      setError(exception.message || 'No fue posible cancelar la invitación.');
      throw exception;
    }
  }, [cargar]);

  const responder = useCallback(async (conversacionId, aceptar) => {
    setError('');
    try {
      await responderInvitacion(conversacionId, aceptar);
      await cargar();
    } catch (exception) {
      setError(exception.message || 'No fue posible responder la invitación.');
      throw exception;
    }
  }, [cargar]);

  const eliminar = useCallback(async (id) => {
    setEliminandoId(id);
    setError('');
    try {
      await eliminarConversacion(id);
      setConversaciones((actuales) => actuales.filter((item) => item.id !== id));
    } catch (exception) {
      setError(exception.message || MENSAJE_ELIMINAR);
      throw exception;
    } finally {
      setEliminandoId(null);
    }
  }, []);

  const obtener = useCallback((id) => obtenerConversacion(id), []);

  return {
    conversaciones,
    invitaciones,
    cargando,
    creando,
    eliminandoId,
    error,
    cargar,
    crear,
    invitar,
    cancelar,
    responder,
    eliminar,
    obtener,
  };
}
