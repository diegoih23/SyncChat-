import { useEffect, useRef, useState } from 'react';
import { useConversaciones } from '../hooks/useConversaciones.js';
import { buscarContactos, listarParticipantes } from '../servicios/conversacionesApi.js';

const COLORES = ['coral', 'blue', 'green', 'purple'];
const ESTADOS_PARTICIPANTE = {
  ACCEPTED: 'Aceptó',
  PENDING: 'Pendiente',
  DECLINED: 'Rechazó',
  LEFT: 'Salió',
  REMOVED: 'Retirado',
};

function iniciales(nombre = '') {
  const partes = nombre.trim().split(/\s+/).slice(0, 2);
  const resultado = partes.map((parte) => parte[0]).join('').toUpperCase();
  return resultado || 'SC';
}

export function Conversaciones({ activa, onSelect, usuarioId }) {
  const { conversaciones, invitaciones, cargando, creando, eliminandoId, error, cargar, crear, invitar, cancelar, responder, eliminar } =
    useConversaciones();
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [nombre, setNombre] = useState('');
  const [correoInvitado, setCorreoInvitado] = useState('');
  const [errorFormulario, setErrorFormulario] = useState('');
  const [mensajeFormulario, setMensajeFormulario] = useState('');
  const [consultaContacto, setConsultaContacto] = useState('');
  const [resultadosContacto, setResultadosContacto] = useState([]);
  const [buscandoContactos, setBuscandoContactos] = useState(false);
  const [errorBusqueda, setErrorBusqueda] = useState('');
  const [busquedaRealizada, setBusquedaRealizada] = useState(false);
  const [participantes, setParticipantes] = useState([]);
  const [cargandoParticipantes, setCargandoParticipantes] = useState(false);
  const [errorParticipantes, setErrorParticipantes] = useState('');
  const [correoParaInvitar, setCorreoParaInvitar] = useState('');
  const [invitandoParticipante, setInvitandoParticipante] = useState(false);
  const [dialogo, setDialogo] = useState(null);
  const [correoCancelar, setCorreoCancelar] = useState('');
  const [procesandoDialogo, setProcesandoDialogo] = useState(false);
  const cancelarRef = useRef(null);

  useEffect(() => {
    if (!cargando && activa && !conversaciones.some((conversacion) => conversacion.id === activa)) {
      onSelect?.(null);
    }
  }, [activa, cargando, conversaciones, onSelect]);

  useEffect(() => {
    if (!dialogo) return undefined;
    cancelarRef.current?.focus();
    const cerrarConEscape = (event) => {
      if (event.key === 'Escape' && !procesandoDialogo) setDialogo(null);
    };
    window.addEventListener('keydown', cerrarConEscape);
    return () => window.removeEventListener('keydown', cerrarConEscape);
  }, [dialogo, procesandoDialogo]);

  useEffect(() => {
    if (!activa) {
      setParticipantes([]);
      setErrorParticipantes('');
      return undefined;
    }
    let vigente = true;
    setCargandoParticipantes(true);
    setErrorParticipantes('');
    let consultando = false;
    const actualizar = () => {
      if (consultando) return;
      consultando = true;
      listarParticipantes(activa).then((datos) => {
        if (vigente) setParticipantes(Array.isArray(datos) ? datos : []);
      }).catch((exception) => {
        if (vigente && cargandoParticipantes) setErrorParticipantes(exception.message || 'No se pudieron cargar los participantes.');
      }).finally(() => {
        consultando = false;
        if (vigente) setCargandoParticipantes(false);
      });
    };
    actualizar();
    const intervalo = window.setInterval(() => {
      if (document.visibilityState === 'visible') actualizar();
    }, 4000);
    return () => { vigente = false; window.clearInterval(intervalo); };
  }, [activa]);

  const alternarFormulario = () => {
    setMostrarFormulario((visible) => !visible);
    setErrorFormulario('');
    setMensajeFormulario('');
  };

  const enviar = async (event) => {
    event.preventDefault();
    const limpio = nombre.trim();
    if (!limpio) {
      setErrorFormulario('Escribe un nombre para la conversación.');
      return;
    }
    if (!correoInvitado.trim()) {
      setErrorFormulario('Indica el correo de la persona a invitar.');
      return;
    }
    if (limpio.length > 160) {
      setErrorFormulario('El nombre no puede pasar de 160 caracteres.');
      return;
    }
    setErrorFormulario('');
    try {
      const nueva = await crear(limpio);
      try {
        await invitar(nueva.id, correoInvitado.trim());
      } catch (exception) {
        setErrorFormulario(`Se creó la conversación, pero no se envió la invitación: ${exception.message}`);
        return;
      }
      setNombre('');
      setCorreoInvitado('');
      setMostrarFormulario(false);
      setMensajeFormulario('Invitación enviada. La persona debe aceptarla antes de acceder al chat.');
    } catch {
      // El hook ya expone el mensaje amigable en `error`.
    }
  };

  const borrar = (conversacion) => setDialogo({ tipo: 'eliminar', conversacion });

  const cancelarInvitacion = (conversacion) => {
    setCorreoCancelar('');
    setDialogo({ tipo: 'cancelar-invitacion', conversacion });
  };

  const confirmarDialogo = async (event) => {
    event.preventDefault();
    if (!dialogo) return;
    setProcesandoDialogo(true);
    try {
      if (dialogo.tipo === 'eliminar') {
        await eliminar(dialogo.conversacion.id);
        if (activa === dialogo.conversacion.id) onSelect?.(null);
      } else {
        await cancelar(dialogo.conversacion.id, correoCancelar.trim());
      }
      setDialogo(null);
    } catch {
      // El hook ya expone el error con el estilo de SyncChat.
    } finally {
      setProcesandoDialogo(false);
    }
  };

  const buscar = async (event) => {
    event.preventDefault();
    setErrorBusqueda('');
    setBusquedaRealizada(true);
    if (consultaContacto.trim().length < 3) {
      setErrorBusqueda('Escribe al menos 3 caracteres para buscar.');
      setResultadosContacto([]);
      return;
    }
    setBuscandoContactos(true);
    try {
      const resultados = await buscarContactos(consultaContacto.trim());
      setResultadosContacto(Array.isArray(resultados) ? resultados : []);
    } catch (exception) {
      setResultadosContacto([]);
      setErrorBusqueda(exception.message || 'No fue posible buscar contactos.');
    } finally {
      setBuscandoContactos(false);
    }
  };

  const invitarResultadoBuscado = async () => {
    if (!activa || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(consultaContacto.trim())) return;
    setInvitandoParticipante(true);
    setErrorBusqueda('');
    try {
      await invitar(activa, consultaContacto.trim());
      const datos = await listarParticipantes(activa);
      setParticipantes(Array.isArray(datos) ? datos : []);
      setMensajeFormulario('Invitación enviada. Aparecerá como pendiente hasta que la persona responda.');
    } catch (exception) {
      setErrorBusqueda(exception.message || 'No se pudo enviar la invitación.');
    } finally {
      setInvitandoParticipante(false);
    }
  };

  const invitarParticipante = async (event) => {
    event.preventDefault();
    if (!activa || !correoParaInvitar.trim()) return;
    setInvitandoParticipante(true);
    setErrorParticipantes('');
    try {
      await invitar(activa, correoParaInvitar.trim());
      setCorreoParaInvitar('');
      const datos = await listarParticipantes(activa);
      setParticipantes(Array.isArray(datos) ? datos : []);
    } catch (exception) {
      setErrorParticipantes(exception.message || 'No se pudo enviar la invitación.');
    } finally {
      setInvitandoParticipante(false);
    }
  };

  return (
    <div className="conversation-panel">
      <div className="conversation-heading">
        <span className="conversation-label">Chats aceptados</span>
        <button
          className="new-conversation-button"
          type="button"
          onClick={alternarFormulario}
          aria-expanded={mostrarFormulario}
          aria-label={mostrarFormulario ? 'Cancelar nueva conversación' : 'Nueva conversación'}
          title={mostrarFormulario ? 'Cancelar' : 'Nueva conversación'}
        >
          {mostrarFormulario ? '×' : '+'}
        </button>
      </div>

      {mensajeFormulario && <p className="conversation-status" role="status">{mensajeFormulario}</p>}

      <form className="contact-search" onSubmit={buscar} role="search">
        <label htmlFor="buscar-contacto">Buscar contactos</label>
        <div>
          <input
            id="buscar-contacto"
            value={consultaContacto}
            onChange={(event) => setConsultaContacto(event.target.value)}
            placeholder="Nombre o correo (3 caracteres mínimo)"
            maxLength={100}
          />
          <button type="submit" disabled={buscandoContactos || consultaContacto.trim().length < 3}>
            {buscandoContactos ? '…' : 'Buscar'}
          </button>
        </div>
        {errorBusqueda && <p className="conversation-error" role="alert">{errorBusqueda}</p>}
        {resultadosContacto.length > 0 && (
          <ul className="contact-search-results" aria-label="Resultados de contactos">
            {resultadosContacto.map((contacto, indice) => (
              <li key={`${contacto.nombre}-${indice}`}>
                <span className="contact-avatar" aria-hidden="true">{iniciales(contacto.nombre)}</span>
                <span>{contacto.nombre}</span>
              </li>
            ))}
          </ul>
        )}
        {activa && resultadosContacto.length > 0
          && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(consultaContacto.trim()) && (
            <button className="contact-invite-button" type="button" onClick={invitarResultadoBuscado}
              disabled={invitandoParticipante}>
              {invitandoParticipante ? 'Enviando…' : `Invitar a ${resultadosContacto[0].nombre} al chat abierto`}
            </button>
          )}
        {busquedaRealizada && resultadosContacto.length === 0 && !buscandoContactos && !errorBusqueda && (
          <p className="conversation-status">Sin resultados. Solo se muestran nombres; los correos e identificadores permanecen privados.</p>
        )}
      </form>

      {invitaciones.length > 0 && (
        <section className="pending-invitations" aria-label="Invitaciones pendientes">
          <span className="conversation-label">Invitaciones por responder ({invitaciones.length})</span>
          {invitaciones.map((invitacion) => (
            <article className="pending-invitation" key={invitacion.conversacionId}>
              <strong>{invitacion.nombre}</strong>
              <span>Te invitaron a esta conversación</span>
              <div>
                <button type="button" onClick={async () => {
                  try { await responder(invitacion.conversacionId, true); }
                  catch { /* El hook presenta el error. */ }
                }}>Aceptar</button>
                <button type="button" onClick={async () => {
                  try { await responder(invitacion.conversacionId, false); }
                  catch { /* El hook presenta el error. */ }
                }}>Rechazar</button>
              </div>
            </article>
          ))}
        </section>
      )}

      {mostrarFormulario && (
        <form className="new-conversation-form" onSubmit={enviar}>
          <input
            value={nombre}
            onChange={(event) => setNombre(event.target.value)}
            maxLength={160}
            placeholder="Nombre de la conversación"
            aria-label="Nombre de la conversación"
            autoFocus
          />
          <input
            type="email"
            value={correoInvitado}
            onChange={(event) => setCorreoInvitado(event.target.value)}
            placeholder="Correo de la persona a invitar"
            aria-label="Correo de la persona a invitar"
            required
          />
          {errorFormulario && <p className="conversation-error" role="alert">{errorFormulario}</p>}
          <button type="submit" disabled={creando}>
            {creando ? 'Creando…' : 'Crear conversación'}
          </button>
        </form>
      )}

      {error && (
        <div className="conversation-error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={cargar}>Reintentar</button>
        </div>
      )}

      {cargando ? (
        <p className="conversation-status">Cargando conversaciones…</p>
      ) : conversaciones.length === 0 ? (
        <p className="conversation-status">
          Aún no tienes conversaciones.
          <span>Crea la primera con el botón +.</span>
        </p>
      ) : (
        <nav className="conversation-list" aria-label="Tus conversaciones">
          {conversaciones.map((item, indice) => (
            <div className={`conversation ${activa === item.id ? 'active' : ''}`} key={item.id}>
              <button className="conversation-select" type="button" onClick={() => onSelect?.(item)}>
                <span className={`conversation-icon ${COLORES[indice % COLORES.length]}`}>
                  {iniciales(item.nombre)}
                </span>
                <span className="conversation-name">{item.nombre}</span>
              </button>
              <button
                className="conversation-delete"
                type="button"
                onClick={() => borrar(item)}
                disabled={eliminandoId === item.id}
                aria-label={`Eliminar la conversación ${item.nombre}`}
                title="Eliminar conversación"
              >
                {eliminandoId === item.id ? '…' : '×'}
              </button>
              {item.creadoPor === usuarioId && (
                <button
                  className="conversation-cancel-invite"
                  type="button"
                  onClick={() => cancelarInvitacion(item)}
                  aria-label={`Cancelar invitación de ${item.nombre}`}
                  title="Cancelar invitación pendiente por correo"
                >↶</button>
              )}
            </div>
          ))}
        </nav>
      )}

      {activa && (
        <section className="conversation-participants" aria-label="Participantes de la conversación">
          <span className="conversation-label">Participantes y estado</span>
          {cargandoParticipantes ? <p className="conversation-status">Cargando participantes…</p> : (
            <ul className="participant-list">
              {participantes.map((participante, indice) => (
                <li key={`${participante.nombre}-${indice}`}>
                  <span className={`conversation-icon ${COLORES[indice % COLORES.length]}`} aria-hidden="true">
                    {iniciales(participante.nombre)}
                  </span>
                  <span className="participant-name">{participante.nombre}{participante.administrador ? ' · Admin' : ''}</span>
                  <span className={`participant-state state-${participante.estado?.toLowerCase()}`}>
                    {ESTADOS_PARTICIPANTE[participante.estado] || participante.estado}
                  </span>
                </li>
              ))}
            </ul>
          )}
          {errorParticipantes && <p className="conversation-error" role="alert">{errorParticipantes}</p>}
          {conversaciones.some((conversacion) => conversacion.id === activa && conversacion.creadoPor === usuarioId) && (
            <form className="invite-participant-form" onSubmit={invitarParticipante}>
              <input
                type="email"
                required
                value={correoParaInvitar}
                onChange={(event) => setCorreoParaInvitar(event.target.value)}
                placeholder="Correo para invitar"
                aria-label="Correo de la persona a invitar"
              />
              <button type="submit" disabled={invitandoParticipante || !correoParaInvitar.trim()}>
                {invitandoParticipante ? 'Enviando…' : 'Invitar'}
              </button>
            </form>
          )}
        </section>
      )}

      {dialogo && (
        <div className="dialog-backdrop" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !procesandoDialogo) setDialogo(null);
        }}>
          <section className="confirm-dialog" role="dialog" aria-modal="true"
            aria-labelledby="conversation-dialog-title" aria-describedby="conversation-dialog-description">
            <span className={`confirm-dialog-icon ${dialogo.tipo === 'eliminar' ? 'danger' : ''}`} aria-hidden="true">
              {dialogo.tipo === 'eliminar' ? '!' : '↶'}
            </span>
            <h2 id="conversation-dialog-title">
              {dialogo.tipo === 'eliminar' ? 'Eliminar conversación' : 'Cancelar invitación'}
            </h2>
            <p id="conversation-dialog-description">
              {dialogo.tipo === 'eliminar'
                ? <>¿Seguro que quieres eliminar <strong>“{dialogo.conversacion.nombre}”</strong>? Esta acción no se puede deshacer.</>
                : <>Ingresa el correo de la persona cuya invitación pendiente deseas cancelar en <strong>“{dialogo.conversacion.nombre}”</strong>.</>}
            </p>
            {dialogo.tipo === 'cancelar-invitacion' && (
              <input ref={cancelarRef} className="dialog-input" type="email" required autoFocus
                value={correoCancelar} onChange={(event) => setCorreoCancelar(event.target.value)}
                placeholder="persona@correo.com" aria-label="Correo de la invitación" />
            )}
            <form className="confirm-dialog-actions" onSubmit={confirmarDialogo}>
              <button ref={dialogo.tipo === 'eliminar' ? cancelarRef : undefined} className="dialog-secondary"
                type="button" onClick={() => setDialogo(null)} disabled={procesandoDialogo}>Volver</button>
              <button className={dialogo.tipo === 'eliminar' ? 'dialog-danger' : 'dialog-primary'}
                type="submit" disabled={procesandoDialogo || (dialogo.tipo !== 'eliminar' && !correoCancelar.trim())}>
                {procesandoDialogo ? 'Procesando…' : dialogo.tipo === 'eliminar' ? 'Eliminar' : 'Cancelar invitación'}
              </button>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
