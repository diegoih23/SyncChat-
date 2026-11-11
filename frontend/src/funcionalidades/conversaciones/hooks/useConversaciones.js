import { useCallback, useEffect, useRef, useState } from 'react';
import {
  crearConversacion,
  eliminarConversacion,
  listarConversaciones,
  obtenerConversacion,
} from '../servicios/conversacionesApi.js';

const MENSAJE_CARGA = 'No fue posible cargar tus conversaciones.';
const MENSAJE_CREAR = 'No fue posible crear la conversación.';
const MENSAJE_ELIMINAR = 'No fue posible eliminar la conversación.';

export function useConversaciones() {
  const [conversaciones, setConversaciones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [creando, setCreando] = useState(false);
  const [eliminandoId, setEliminandoId] = useState(null);
  const [error, setError] = useState('');
  const montado = useRef(true);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const datos = await listarConversaciones();
      if (montado.current) setConversaciones(Array.isArray(datos) ? datos : []);
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
    cargando,
    creando,
    eliminandoId,
    error,
    cargar,
    crear,
    eliminar,
    obtener,
  };
}
