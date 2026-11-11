import { useCallback, useEffect, useRef, useState } from 'react';
import { crearConexionChat } from '../servicios/chatApi.js';
import { obtenerToken } from '../../autenticacion/servicios/sesion.js';

export function useChatWebSocket({ onMessage, onError, enabled = true, maxDelay = 10000 } = {}) {
  const socketRef = useRef(null);
  const timerRef = useRef(null);
  const intentoRef = useRef(0);
  const [conectado, setConectado] = useState(false);

  useEffect(() => {
    if (!enabled || !obtenerToken()) return undefined;
    let activo = true;
    const conectar = () => {
      if (!activo || socketRef.current?.readyState === WebSocket.OPEN) return;
      try {
        const socket = crearConexionChat();
        socketRef.current = socket;
        socket.onopen = () => { intentoRef.current = 0; setConectado(true); };
        socket.onmessage = (event) => {
          try {
            onMessage?.(JSON.parse(event.data));
          } catch (error) {
            onError?.(error);
          }
        };
        socket.onerror = (error) => onError?.(error);
        socket.onclose = () => {
          setConectado(false);
          if (!activo) return;
          const delay = Math.min(1000 * 2 ** intentoRef.current++, maxDelay);
          timerRef.current = window.setTimeout(conectar, delay);
        };
      } catch (error) { onError?.(error); }
    };
    conectar();
    return () => {
      activo = false;
      if (timerRef.current) window.clearTimeout(timerRef.current);
      socketRef.current?.close(); socketRef.current = null;
    };
  }, [enabled, maxDelay, onError, onMessage]);

  const enviar = useCallback((mensaje) => {
    if (socketRef.current?.readyState !== WebSocket.OPEN) return false;
    socketRef.current.send(JSON.stringify(mensaje)); return true;
  }, []);
  return { conectado, enviar };
}
