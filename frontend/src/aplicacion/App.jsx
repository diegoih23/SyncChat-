import { useState } from 'react';
import { Conversaciones } from '../funcionalidades/conversaciones/componentes/Conversaciones.jsx';
import { Chat } from '../funcionalidades/chat/componentes/Chat.jsx';

export function App() {
  const [conversacion, setConversacion] = useState('Equipo de producto');
  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">S</span><span>SyncChat</span></div>
        <p className="eyebrow">Tus espacios</p>
        <Conversaciones activa={conversacion} onSelect={setConversacion} />
        <div className="sidebar-footer"><span className="avatar">AM</span><span><strong>Alex Morgan</strong><small>Disponible</small></span></div>
      </aside>
      <section className="workspace"><header className="topbar"><span className="mobile-brand">SyncChat</span><button className="profile-button">AM</button></header><Chat nombre={conversacion} /></section>
    </main>
  );
}
