<<<<<<< HEAD
import { useState } from 'react';
import { useConversaciones } from '../hooks/useConversaciones.js';

const COLORES = ['coral', 'blue', 'green', 'purple'];

function iniciales(nombre = '') {
  const partes = nombre.trim().split(/\s+/).slice(0, 2);
  const resultado = partes.map((parte) => parte[0]).join('').toUpperCase();
  return resultado || 'SC';
}

export function Conversaciones({ activa, onSelect }) {
  const { conversaciones, cargando, creando, eliminandoId, error, cargar, crear, eliminar } =
    useConversaciones();
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [nombre, setNombre] = useState('');
  const [errorFormulario, setErrorFormulario] = useState('');

  const alternarFormulario = () => {
    setMostrarFormulario((visible) => !visible);
    setErrorFormulario('');
  };

  const enviar = async (event) => {
    event.preventDefault();
    const limpio = nombre.trim();
    if (!limpio) {
      setErrorFormulario('Escribe un nombre para la conversación.');
      return;
    }
    if (limpio.length > 160) {
      setErrorFormulario('El nombre no puede pasar de 160 caracteres.');
      return;
    }
    setErrorFormulario('');
    try {
      const nueva = await crear(limpio);
      setNombre('');
      setMostrarFormulario(false);
      onSelect?.(nueva);
    } catch {
      // El hook ya expone el mensaje amigable en `error`.
    }
  };

  const borrar = async (conversacion) => {
    const confirmado = window.confirm(`¿Eliminar la conversación "${conversacion.nombre}"?`);
    if (!confirmado) return;
    try {
      await eliminar(conversacion.id);
      if (activa === conversacion.id) onSelect?.(null);
    } catch {
      // El hook ya expone el mensaje amigable en `error`.
    }
  };

  return (
    <div className="conversation-panel">
      <div className="conversation-heading">
        <span className="conversation-label">Conversaciones</span>
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
          {errorFormulario && <p className="conversation-error">{errorFormulario}</p>}
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
            </div>
          ))}
        </nav>
      )}
    </div>
  );
=======
export const conversaciones = [
  { id: 'a4b565b0-9e72-4dd5-8a34-c4d9e1a56101', nombre: 'Equipo de producto', iniciales: 'EP', color: 'coral' },
  { id: 'a4b565b0-9e72-4dd5-8a34-c4d9e1a56102', nombre: 'Diseño y experiencia', iniciales: 'DE', color: 'blue' },
  { id: 'a4b565b0-9e72-4dd5-8a34-c4d9e1a56103', nombre: 'General', iniciales: 'GE', color: 'green' }
];

export function Conversaciones({ activa, onSelect }) {
  return <nav className="conversation-list" aria-label="Conversaciones">{conversaciones.map((item) => <button className={`conversation ${activa?.id === item.id ? 'active' : ''}`} key={item.id} onClick={() => onSelect(item)}><span className={`conversation-icon ${item.color}`}>{item.iniciales}</span><span className="conversation-name">{item.nombre}</span><span className="conversation-arrow">›</span></button>)}</nav>;
>>>>>>> Wagner-Balcazar
}
