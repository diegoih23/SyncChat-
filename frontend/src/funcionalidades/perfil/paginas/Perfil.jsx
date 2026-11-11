import { useEffect, useState } from 'react';
import { obtenerPerfil } from '../servicios/perfilApi.js';

function iniciales(nombre = '') {
  return nombre.trim().split(/\s+/).slice(0, 2).map((parte) => parte[0]).join('').toUpperCase() || 'SC';
}

export function Perfil({ usuario, light, onToggleTheme, onBack, onLogout }) {
  const [perfil, setPerfil] = useState(usuario);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!usuario?.id) {
      setCargando(false);
      return;
    }
    obtenerPerfil(usuario.id)
      .then(setPerfil)
      .catch((exception) => setError(exception.message || 'No fue posible cargar tu perfil.'))
      .finally(() => setCargando(false));
  }, [usuario?.id]);

  return <main className={`profile-page ${light ? 'light-theme' : ''}`}>
    <header className="profile-header">
      <button className="profile-back" type="button" onClick={onBack}>← Volver al chat</button>
      <div className="profile-header-actions">
        <button className="theme-toggle" type="button" onClick={onToggleTheme} aria-label={light ? 'Activar modo oscuro' : 'Activar modo claro'}>{light ? '☾' : '☼'}</button>
        <button className="profile-logout" type="button" onClick={onLogout}>Cerrar sesión</button>
      </div>
    </header>
    <section className="profile-content">
      <div className="profile-intro"><span className="profile-kicker">TU ESPACIO PERSONAL</span><h1>Tu perfil,<br /><span>tu identidad.</span></h1><p>Administra la información con la que participas en las conversaciones de SyncChat.</p></div>
      <div className="profile-card">
        <div className="profile-card-top"><div className="profile-avatar">{iniciales(perfil?.nombre)}</div><div><span className="profile-status"><i /> Cuenta activa</span><h2>{perfil?.nombre || 'Usuario'}</h2><p>{perfil?.correo || 'Sin correo registrado'}</p></div></div>
        {cargando && <p className="profile-message">Cargando información…</p>}
        {error && <p className="profile-error" role="alert">{error}</p>}
        <div className="profile-details"><div><span>Nombre completo</span><strong>{perfil?.nombre || '—'}</strong></div><div><span>Correo electrónico</span><strong>{perfil?.correo || '—'}</strong></div><div><span>ID de usuario</span><strong className="profile-id">{perfil?.id || '—'}</strong></div></div>
        <div className="profile-note"><span>✦</span><p>Tu cuenta está protegida por autenticación segura. Tus conversaciones se mantienen sincronizadas en tiempo real.</p></div>
      </div>
    </section>
  </main>;
}
