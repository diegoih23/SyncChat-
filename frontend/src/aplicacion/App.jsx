import { useState } from 'react';
import { Conversaciones, conversaciones } from '../funcionalidades/conversaciones/componentes/Conversaciones.jsx';
import { Chat } from '../funcionalidades/chat/componentes/Chat.jsx';

export function App() {
  const [conversacion, setConversacion] = useState(conversaciones[0]);
  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">S</span><span>SyncChat</span></div>
        <p className="eyebrow">Tus espacios</p>
        <Conversaciones activa={conversacion} onSelect={setConversacion} />
        <div className="sidebar-footer"><span className="avatar">AM</span><span><strong>Alex Morgan</strong><small>Disponible</small></span></div>
      </aside>
      <section className="workspace"><header className="topbar"><span className="mobile-brand"><span className="brand-mark">S</span>SyncChat</span><button className="profile-button" aria-label="Perfil de Alex Morgan">AM</button></header><Chat conversacion={conversacion} /></section>
    </main>
  );
}
