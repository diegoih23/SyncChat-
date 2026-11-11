import { useCallback, useEffect, useRef, useState } from 'react';
import { useChatWebSocket } from '../hooks/useChatWebSocket.js';
import { enviarMensaje, listarMensajes } from '../../mensajes/servicios/mensajesApi.js';

function mostrarHora(fecha) {
  if (!fecha) return '';
  const valor = new Date(fecha);
  if (Number.isNaN(valor.getTime())) return '';
  return new Intl.DateTimeFormat('es', { hour: '2-digit', minute: '2-digit' }).format(valor);
}

function agregarSinDuplicados(actuales, nuevo) {
  if (!nuevo?.id || actuales.some((mensaje) => mensaje.id === nuevo.id)) return actuales;
  return [...actuales, nuevo];
}

function nuevaClaveIdempotencia() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (caracter) => {
    const aleatorio = Math.floor(Math.random() * 16);
    return (caracter === 'x' ? aleatorio : (aleatorio & 0x3) | 0x8).toString(16);
  });
}

export function Chat({ conversacion, usuario }) {
  const [texto, setTexto] = useState('');
  const [mensajes, setMensajes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const [estadoEnvio, setEstadoEnvio] = useState('');
  const listaRef = useRef(null);
  const clavePendienteRef = useRef(null);
  const conversacionId = conversacion?.id;
  const usuarioId = usuario?.id;
  const [participanteEscribiendo, setParticipanteEscribiendo] = useState(false);
  const typingTimerRef = useRef(null);
  const typingActivoRef = useRef(false);
  const recibir = useCallback((evento) => {
    if (evento?.tipo === 'typing' && evento.conversacionId === conversacionId && evento.usuarioId !== usuarioId) {
      setParticipanteEscribiendo(Boolean(evento.escribiendo));
      if (typingTimerRef.current) window.clearTimeout(typingTimerRef.current);
      if (evento.escribiendo) typingTimerRef.current = window.setTimeout(() => setParticipanteEscribiendo(false), 3500);
      return;
    }
    if (evento?.conversacionId !== conversacionId || !evento?.id) return;
    setMensajes((actuales) => agregarSinDuplicados(actuales, evento));
  }, [conversacionId, usuarioId]);
  const reportarErrorSocket = useCallback(() => {
    setError('La conexión en tiempo real se interrumpió; se intentará reconectar.');
  }, []);
  const { conectado, enviar: enviarEventoSocket } = useChatWebSocket({
    onMessage: recibir,
    onError: reportarErrorSocket,
    enabled: Boolean(conversacionId && usuarioId),
  });

  const indicarEscritura = (contenido) => {
    if (!conectado) return;
    const escribiendo = Boolean(contenido.trim());
    if (escribiendo && !typingActivoRef.current) {
      typingActivoRef.current = true;
      enviarEventoSocket({ tipo: 'typing', conversacionId, escribiendo: true });
    }
    if (typingTimerRef.current) window.clearTimeout(typingTimerRef.current);
    typingTimerRef.current = window.setTimeout(() => {
      if (typingActivoRef.current) enviarEventoSocket({ tipo: 'typing', conversacionId, escribiendo: false });
      typingActivoRef.current = false;
    }, escribiendo ? 1100 : 0);
  };

  useEffect(() => () => {
    if (typingTimerRef.current) window.clearTimeout(typingTimerRef.current);
    if (typingActivoRef.current) enviarEventoSocket({ tipo: 'typing', conversacionId, escribiendo: false });
  }, [conversacionId, enviarEventoSocket]);
  useEffect(() => {
    if (!conversacionId) return undefined;
    let activo = true;
    setCargando(true);
    setError('');
    setEstadoEnvio('');
    setMensajes([]);
    clavePendienteRef.current = null;
    listarMensajes(conversacionId)
      .then((lista) => {
        if (activo) setMensajes(Array.isArray(lista) ? lista : []);
      })
      .catch((exception) => {
        if (activo) setError(exception.message || 'No se pudo cargar el historial.');
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => { activo = false; };
  }, [conversacionId]);

  useEffect(() => {
    if (!conectado || !conversacionId) return undefined;
    let activo = true;
    listarMensajes(conversacionId).then((lista) => {
      if (!activo || !Array.isArray(lista)) return;
      setMensajes((actuales) => lista.reduce(agregarSinDuplicados, actuales));
      setError('');
    }).catch((exception) => {
      if (activo) setError(exception.message || 'No se pudo sincronizar el historial tras reconectar.');
    });
    return () => { activo = false; };
  }, [conectado, conversacionId]);

  useEffect(() => {
    if (conectado || !conversacionId) return undefined;
    let activo = true;
    let consultando = false;
    const sincronizarHistorial = () => {
      if (consultando || document.visibilityState !== 'visible') return;
      consultando = true;
      listarMensajes(conversacionId).then((lista) => {
        if (activo && Array.isArray(lista)) {
          setMensajes((actuales) => lista.reduce(agregarSinDuplicados, actuales));
          setError('');
        }
      }).catch(() => {
        // Se conserva el chat visible; el siguiente ciclo vuelve a intentar.
      }).finally(() => { consultando = false; });
    };
    const intervalo = window.setInterval(sincronizarHistorial, 4000);
    document.addEventListener('visibilitychange', sincronizarHistorial);
    return () => {
      activo = false;
      window.clearInterval(intervalo);
      document.removeEventListener('visibilitychange', sincronizarHistorial);
    };
  }, [conectado, conversacionId]);

  useEffect(() => {
    if (listaRef.current) listaRef.current.scrollTo({ top: listaRef.current.scrollHeight, behavior: 'smooth' });
  }, [mensajes]);

  async function enviar(event) {
    event.preventDefault();
    const contenido = texto.trim();
    if (!contenido || enviando) return;
    if (!usuarioId) {
      setError('No se encontró el usuario de la sesión. Vuelve a iniciar sesión.');
      return;
    }
    setEnviando(true);
    indicarEscritura('');
    setError('');
    setEstadoEnvio('');
    if (clavePendienteRef.current?.contenido !== contenido) {
      clavePendienteRef.current = { contenido, clave: nuevaClaveIdempotencia() };
    }
    try {
      const nuevo = await enviarMensaje({ conversacionId, contenido, idempotencyKey: clavePendienteRef.current.clave });
      setMensajes((actuales) => agregarSinDuplicados(actuales, nuevo));
      setTexto('');
      clavePendienteRef.current = null;
      if (!conectado) {
        setEstadoEnvio('Mensaje guardado. Tu WebSocket está desconectado; esta vista se sincronizará al reconectar.');
      }
    } catch (exception) {
      setError(exception.message || 'No se pudo enviar el mensaje. Inténtalo de nuevo.');
    } finally {
      setEnviando(false);
    }
  }

  return <div className="chat-panel">
    <header className="chat-header">
      <div>
        <p className="eyebrow">ESPACIO DE TRABAJO <span className="header-dot">/</span> MENSAJES</p>
        <h1>{conversacion?.nombre || 'Conversación'}</h1>
        <span className={`online ${conectado ? '' : 'offline'}`} role="status">
          <i /> {conectado ? 'WebSocket conectado · mensajes en tiempo real' : 'WebSocket desconectado · sincronización por historial'}
        </span>
      </div>
      <button className="details-button" type="button"><span className="details-icon">⌘</span> Canal</button>
    </header>
    {estadoEnvio && <p className="chat-delivery-status" role="status">{estadoEnvio}</p>}
    <div className="messages" ref={listaRef} aria-live="polite">
      <div className="date-divider"><span>HISTORIAL DE CONVERSACIÓN</span></div>
      {cargando ? <p className="empty-state">Cargando mensajes…</p> : mensajes.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">✦</span>
          <strong>Aún no hay mensajes</strong>
          <span>Envía el primero para iniciar la conversación.</span>
        </div>
        ) : mensajes.map((mensaje, indice) => {
          const propio = mensaje.remitenteId === usuarioId;
          return (
          <article className={`message ${propio ? 'own-message' : ''}`} key={mensaje.id}
            style={{ '--message-order': Math.min(indice, 8) }}>
            <span className={`avatar ${propio ? '' : 'purple'}`}>{propio ? 'Tú' : 'U'}</span>
            <div className="message-content">
              <div className="message-meta">
                <strong>{propio ? (usuario?.nombre || 'Tú') : (mensaje.remitenteNombre || 'Participante')}</strong>
                <time dateTime={mensaje.creadoEn}>{mostrarHora(mensaje.creadoEn)}</time>
              </div>
              <p>{mensaje.contenido}</p>
            </div>
          </article>
        );
      })}
    </div>
    {participanteEscribiendo && (
      <div className="peer-typing" role="status" aria-live="polite">
        <span className="peer-typing-dots"><i /><i /><i /></span>
        <span>Alguien está escribiendo</span>
      </div>
    )}
    {error && <p className="error-message" role="alert">{error}</p>}
    <form className="composer" onSubmit={enviar}>
      <span className="composer-plus" aria-hidden="true">＋</span>
      <input value={texto} onChange={(event) => { setTexto(event.target.value); indicarEscritura(event.target.value); }} placeholder="Escribe un mensaje…" aria-label="Mensaje" />
      <span className={`composer-hint ${texto.trim() ? 'is-typing' : ''}`} role={texto.trim() ? 'status' : undefined}>
        {texto.trim() ? <><i className="typing-dots"><b /><b /><b /></i> Estás escribiendo</> : 'Enter ↵'}
      </span>
      <button className={texto.trim() ? 'has-text' : ''} type="submit" disabled={enviando || !texto.trim()}>
        {enviando ? 'Enviando…' : 'Enviar'} <span>{enviando ? '⋯' : '↑'}</span>
      </button>
    </form>
    <p className="composer-note">Los mensajes se guardan en el historial de esta conversación.</p>
  </div>;
}
