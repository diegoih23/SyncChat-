import { useState } from 'react';
import { Chat } from '../../chat/componentes/Chat.jsx';
import { Conversaciones } from '../componentes/Conversaciones.jsx';

function iniciales(nombre = '') {
  const partes = nombre.trim().split(/\s+/).slice(0, 2);
  return partes.map((parte) => parte[0]).join('').toUpperCase() || 'SC';
}

export function PanelConversaciones({ usuario, light, onToggleTheme, onLogout }) {
  const [seleccionada, setSeleccionada] = useState(null);

  return (
    <main className={`workspace ${light ? 'light-theme' : ''}`}>
      <aside className="workspace-sidebar">
        <div className="workspace-sidebar-top">
          <a className="brand workspace-brand" href="#inicio" onClick={(event) => event.preventDefault()}>
            <span className="brand-mark">S</span>
            <span>Sync<span className="brand-accent">Chat</span></span>
          </a>
          <button
            className="theme-toggle"
            type="button"
            onClick={onToggleTheme}
            aria-label={light ? 'Activar modo oscuro' : 'Activar modo claro'}
          >
            {light ? '☾' : '☼'}
          </button>
        </div>

        <Conversaciones activa={seleccionada?.id} onSelect={setSeleccionada} />

        <div className="workspace-user">
          <span className="avatar blue">{iniciales(usuario?.nombre)}</span>
          <div className="workspace-user-info">
            <strong>{usuario?.nombre || 'Usuario'}</strong>
            <small>{usuario?.correo || ''}</small>
          </div>
          <button className="logout-button" type="button" onClick={onLogout}>
            Salir
          </button>
        </div>
      </aside>

      <section className="workspace-main">
        {seleccionada ? (
          <Chat key={seleccionada.id} nombre={seleccionada.nombre} />
        ) : (
          <div className="workspace-welcome">
            <span className="workspace-welcome-icon">✦</span>
            <h1>Elige una conversación</h1>
            <p>Selecciona una conversación de la lista o crea una nueva para empezar.</p>
          </div>
        )}
      </section>
    </main>
  );
}
