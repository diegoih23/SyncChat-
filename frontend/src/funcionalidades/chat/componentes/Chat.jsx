import { useEffect, useRef, useState } from 'react';
import { enviarMensaje, listarMensajes } from '../../mensajes/servicios/mensajesApi.js';

// ID temporal hasta integrar el inicio de sesión.
const USUARIO_ID = 'b5c676c1-0f83-4ee6-9b45-d5eaf2b67201';

function mostrarHora(fecha) {
  return new Intl.DateTimeFormat('es', { hour: '2-digit', minute: '2-digit' }).format(new Date(fecha));
}

export function Chat({ conversacion }) {
  const [texto, setTexto] = useState('');
  const [mensajes, setMensajes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const listaRef = useRef(null);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');
    listarMensajes(conversacion.id)
      // Evita actualizar el chat si se cambió de conversación durante la carga.
      .then((lista) => { if (activo) setMensajes(lista); })
      .catch(() => { if (activo) setError('No se pudo cargar el historial. Comprueba que el servicio esté disponible.'); })
      .finally(() => { if (activo) setCargando(false); });
    return () => { activo = false; };
  }, [conversacion.id]);

  useEffect(() => {
    if (listaRef.current) listaRef.current.scrollTop = listaRef.current.scrollHeight;
  }, [mensajes]);

  async function enviar(event) {
    event.preventDefault();
    const contenido = texto.trim();
    if (!contenido || enviando) return;
    setEnviando(true);
    setError('');
    try {
      const nuevo = await enviarMensaje({ conversacionId: conversacion.id, remitenteId: USUARIO_ID, contenido });
      setMensajes((lista) => [...lista, nuevo]);
      setTexto('');
    } catch {
      setError('No se pudo enviar el mensaje. Inténtalo de nuevo.');
    } finally {
      setEnviando(false);
    }
  }

  return <div className="chat-panel">
    <header className="chat-header">
      <div>
        <p className="eyebrow">ESPACIO DE TRABAJO <span className="header-dot">/</span> MENSAJES</p>
        <h1>{conversacion.nombre}</h1>
        <span className="online"><i /> Tu conversación está lista</span>
      </div>
      <button className="details-button" type="button"><span className="details-icon">⌘</span> Canal</button>
    </header>
    <div className="messages" ref={listaRef} aria-live="polite">
      <div className="date-divider"><span>HISTORIAL DE CONVERSACIÓN</span></div>
      {cargando ? <p className="empty-state">Cargando mensajes…</p> : mensajes.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">✦</span>
          <strong>Aún no hay mensajes</strong>
          <span>Envía el primero para iniciar la conversación.</span>
        </div>
      ) : mensajes.map((mensaje) => {
        const propio = mensaje.remitenteId === USUARIO_ID;
        return (
          <article className={`message ${propio ? 'own-message' : ''}`} key={mensaje.id}>
            <span className={`avatar ${propio ? '' : 'purple'}`}>{propio ? 'AM' : 'MC'}</span>
            <div className="message-content">
              <div className="message-meta">
                <strong>{propio ? 'Alex Morgan' : 'Participante'}</strong>
                <time dateTime={mensaje.creadoEn}>{mostrarHora(mensaje.creadoEn)}</time>
              </div>
              <p>{mensaje.contenido}</p>
            </div>
          </article>
        );
      })}
    </div>
    {error && <p className="error-message" role="alert">{error}</p>}
    <form className="composer" onSubmit={enviar}>
      <span className="composer-plus" aria-hidden="true">＋</span>
      <input value={texto} onChange={(event) => setTexto(event.target.value)} placeholder="Escribe un mensaje…" aria-label="Mensaje" />
      <span className="composer-hint">Enter ↵</span>
      <button type="submit" disabled={enviando || !texto.trim()}>{enviando ? 'Enviando…' : 'Enviar'} <span>↑</span></button>
    </form>
    <p className="composer-note">Los mensajes se guardan en el historial de esta conversación.</p>
  </div>;
}
