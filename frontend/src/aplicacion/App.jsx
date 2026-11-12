import { useState } from 'react';
import { comprobarCorreo, iniciarSesion, registrarUsuario, solicitarRecuperacion, restablecerPassword } from '../funcionalidades/autenticacion/servicios/autenticacionApi.js';
import { cerrarSesion, guardarSesion, obtenerToken, obtenerUsuario } from '../funcionalidades/autenticacion/servicios/sesion.js';
import { PanelConversaciones } from '../funcionalidades/conversaciones/paginas/PanelConversaciones.jsx';
import { Perfil } from '../funcionalidades/perfil/paginas/Perfil.jsx';

const features = [
  {
    icon: '✦',
    title: 'Conversa en tiempo real',
    text: 'Envía y recibe mensajes al instante, sin perder el ritmo de la conversación.',
  },
  {
    icon: '⌁',
    title: 'Todo organizado',
    text: 'Encuentra tus espacios, participantes e historial desde un solo lugar.',
  },
  {
    icon: '◈',
    title: 'Diseñado para conectar',
    text: 'Una experiencia simple, clara y lista para tus equipos y comunidades.',
  },
];

function Brand() {
  return (
    <a className="brand" href="#inicio" aria-label="SyncChat inicio">
      <span className="brand-mark">S</span>
      <span>Sync<span className="brand-accent">Chat</span></span>
    </a>
  );
}

function ThemeToggle({ light, onToggle }) {
  return <button className="theme-toggle" onClick={onToggle} aria-label={light ? 'Activar modo oscuro' : 'Activar modo claro'}>{light ? '☾' : '☼'}</button>;
}

function InteractiveTitle({ lines, className = '' }) {
  return <h1 className={`interactive-title ${className}`}>{lines.map((line) => <span className={`title-line ${line.accent ? 'title-accent' : ''}`} key={line.text}>{[...line.text].map((letter, index) => <span className="title-letter" style={{ '--letter-index': index }} key={`${line.text}-${index}`}>{letter === ' ' ? '\u00a0' : letter}</span>)}<br /></span>)}</h1>;
}

function SyncMascot({ trackedEyes, sleeping = false }) {
  const [hovered, setHovered] = useState(false);
  const [message, setMessage] = useState('¡Hola! Soy Syncy 👋');
  const [eyes, setEyes] = useState({ x: 0, y: 0 });
  const messages = ['¡Listos para conversar! ✨', 'Tu equipo te está esperando 💬', '¡Qué bueno verte por aquí!', 'Todo sincronizado 🚀'];
  const followPointer = (event) => {
    const bounds = event.currentTarget.getBoundingClientRect();
    const x = Math.max(-3, Math.min(3, ((event.clientX - bounds.left) / bounds.width - .5) * 8));
    const y = Math.max(-2, Math.min(2, ((event.clientY - bounds.top) / bounds.height - .5) * 5));
    setEyes({ x, y });
  };
  const greet = () => setMessage(messages[Math.floor(Math.random() * messages.length)]);
  return <div className={`mascot-wrap ${hovered ? 'is-hovered' : ''}`} aria-label="Mascota interactiva de SyncChat" role="button" tabIndex="0" onMouseEnter={() => setHovered(true)} onMouseLeave={() => { setHovered(false); setEyes({ x: 0, y: 0 }); }} onPointerMove={followPointer} onClick={greet} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') greet(); }}>
    <div className="mascot-glow" />
    <div className="mascot-bubble">{message}</div>
    <div className="mascot-particles" aria-hidden="true"><span>✦</span><span>·</span><span>✧</span><span>·</span></div>
    <svg className="mascot" viewBox="0 0 240 220" aria-hidden="true">
      <path className="mascot-tail" d="M43 157c-28 3-29 29-5 34 16 3 25-9 27-20" />
      <rect className="mascot-body" x="52" y="67" width="137" height="116" rx="42" />
      <path className="mascot-ear" d="M70 78 52 45c-5-10 8-19 17-11l28 24M169 78l18-33c5-10-8-19-17-11l-28 24" />
      <rect className="mascot-screen" x="76" y="87" width="89" height="61" rx="22" />
      {sleeping ? <><path className="mascot-closed-eye" d="M96 116c5 5 10 5 15 0" /><path className="mascot-closed-eye" d="M129 116c5 5 10 5 15 0" /></> : <><circle className="mascot-eye" cx="104" cy="116" r="7" transform={`translate(${trackedEyes?.x ?? eyes.x} ${trackedEyes?.y ?? eyes.y})`} /><circle className="mascot-eye" cx="137" cy="116" r="7" transform={`translate(${trackedEyes?.x ?? eyes.x} ${trackedEyes?.y ?? eyes.y})`} /></>}
      <path className="mascot-smile" d="M108 132c7 7 18 7 25 0" />
      <circle className="mascot-dot" cx="120" cy="57" r="9" />
      <path className="mascot-arm" d="M55 125c-25 4-27 30-6 35M185 125c25 4 27 30 6 35" />
      <rect className="mascot-logo" x="98" y="158" width="44" height="25" rx="9" /><text x="120" y="176" textAnchor="middle">S</text>
    </svg>
  </div>;
}

function AuthFrame({ light, onToggle, onBack, kicker, title, accentTitle, description, children, sleeping = false }) {
  const [trackedEyes, setTrackedEyes] = useState({ x: 0, y: 0 });
  const trackLogin = (event) => {
    const bounds = event.currentTarget.getBoundingClientRect();
    const x = Math.max(-3, Math.min(3, ((event.clientX - bounds.left) / bounds.width - .5) * 8));
    const y = Math.max(-2, Math.min(2, ((event.clientY - bounds.top) / bounds.height - .5) * 5));
    setTrackedEyes({ x, y });
  };
  return <main className="login-page" onPointerMove={trackLogin}>
    <div className="login-decoration login-decoration-one" /><div className="login-decoration login-decoration-two" />
    <nav className="login-nav"><Brand /><div className="login-nav-actions"><ThemeToggle light={light} onToggle={onToggle} /><button className="back-button" onClick={onBack}>← Volver al inicio</button></div></nav>
    <section className="login-content">
      <div className="login-welcome"><SyncMascot trackedEyes={trackedEyes} sleeping={sleeping} /><span className="status-pill"><span className="pulse-dot" /> Tu espacio está listo</span><InteractiveTitle lines={[{ text: title }, { text: accentTitle, accent: true }]} /><p>{description}</p></div>
      <div className="login-card"><span className="login-kicker">{kicker}</span>{children}</div>
    </section>
  </main>;
}

function Login({ light, onToggle, onBack, onRegister, onForgot, onAuth }) {
  const [passwordFocused, setPasswordFocused] = useState(false);
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [correoNoRegistrado, setCorreoNoRegistrado] = useState(false);
  const [cargando, setCargando] = useState(false);
  const [recordarme, setRecordarme] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    setCorreoNoRegistrado(false);
    const correoValido = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim());
    if (!correo.trim()) {
      setError('Ingresa tu correo electrónico.');
      return;
    }
    if (!correoValido) {
      setError('Ingresa un correo electrónico válido.');
      return;
    }
    if (!password) {
      setError('Ingresa tu contraseña.');
      return;
    }
    setError('');
    setCargando(true);
    try {
      const respuesta = await iniciarSesion({ correo, password });
      guardarSesion(respuesta, recordarme);
      onAuth?.(respuesta.usuario);
    } catch (exception) {
      if (exception.status === 401 || exception.status === 403) {
        try {
          const resultado = await comprobarCorreo(correo.trim());
          if (!resultado.registrado) {
            setCorreoNoRegistrado(true);
            setError('Este correo no está registrado. Puedes crear una cuenta para comenzar.');
            return;
          }
        } catch {
          // Si no se puede comprobar el correo, se conserva el error de login.
        }
        setError('El correo o la contraseña no son correctos. Revisa tus datos e inténtalo de nuevo.');
        return;
      }
      setError(exception.message || 'No fue posible iniciar sesión.');
    } finally {
      setCargando(false);
    }
  };
  return <AuthFrame light={light} onToggle={onToggle} onBack={onBack} kicker="BIENVENIDO A SYNCCHAT" title="Qué bueno" accentTitle="verte de nuevo." description="Inicia sesión para volver a tus conversaciones y mantener todo sincronizado." sleeping={passwordFocused}>
    <form onSubmit={submit} noValidate autoComplete="off"><h2>Iniciar sesión</h2><p className="login-subtitle">Ingresa tus datos para continuar.</p>
      {error && <div className="auth-error" role="alert"><span className="feedback-icon" aria-hidden="true">!</span><span>{error}{correoNoRegistrado && <> <button type="button" className="inline-link" onClick={onRegister}>Crear cuenta</button></>}</span></div>}
      <label htmlFor="email">Correo electrónico</label><input id="email" type="email" autoComplete="off" value={correo} onChange={(event) => setCorreo(event.target.value)} placeholder="tu correo@ejemplo.com" />
      <div className="password-label"><label htmlFor="password">Contraseña</label><button type="button" className="inline-link" onClick={onForgot}>¿La olvidaste?</button></div><input id="password" type="password" autoComplete="off" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Ingresa tu contraseña" onFocus={() => setPasswordFocused(true)} onBlur={() => setPasswordFocused(false)} />
      <label className="remember"><input type="checkbox" checked={recordarme} onChange={(event) => setRecordarme(event.target.checked)} /> <span>Recordarme en este dispositivo</span></label><button className="login-submit" type="submit" disabled={cargando}>{cargando ? 'Ingresando…' : 'Entrar a SyncChat'} <span>→</span></button>
      <div className="login-divider"><span>o</span></div><p className="register-prompt">¿Aún no tienes una cuenta? <button type="button" className="inline-link" onClick={onRegister}>Crear cuenta</button></p>
    </form>
  </AuthFrame>;
}

function Register({ light, onToggle, onBack, onLogin, onAuth }) {
  const [passwordFocused, setPasswordFocused] = useState(false);
  const [datos, setDatos] = useState({ nombre: '', correo: '', password: '' });
  const [confirmacion, setConfirmacion] = useState('');
  const [aceptoTerminos, setAceptoTerminos] = useState(false);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const actualizar = (campo) => (event) => {
    setDatos((actuales) => ({ ...actuales, [campo]: event.target.value }));
    setError('');
  };
  const submit = async (event) => {
    event.preventDefault();
    const nombre = datos.nombre.trim();
    const correo = datos.correo.trim();
    if (!nombre) { setError('Escribe tu nombre para crear la cuenta.'); return; }
    if (nombre.length > 120) { setError('El nombre no puede superar los 120 caracteres.'); return; }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) { setError('Ingresa un correo electrónico válido.'); return; }
    if (correo.length > 180) { setError('El correo no puede superar los 180 caracteres.'); return; }
    if (datos.password.length < 8 || datos.password.length > 72) { setError('La contraseña debe tener entre 8 y 72 caracteres.'); return; }
    if (datos.password !== confirmacion) {
      setError('Las contraseñas no coinciden.');
      return;
    }
    if (!aceptoTerminos) {
      setError('Debes aceptar los términos y condiciones para crear la cuenta.');
      return;
    }
    setError('');
    setCargando(true);
    try {
      const respuesta = await registrarUsuario({ ...datos, nombre, correo });
      guardarSesion(respuesta);
      onAuth?.(respuesta.usuario);
    } catch (exception) {
      setError(exception.message || 'No fue posible crear la cuenta.');
    } finally {
      setCargando(false);
    }
  };
  return <AuthFrame light={light} onToggle={onToggle} onBack={onBack} kicker="COMIENZA A CONECTAR" title="Crea tu espacio." accentTitle="empieza a compartir." description="Regístrate en SyncChat y lleva tus conversaciones a un lugar más simple." sleeping={passwordFocused}>
    <form onSubmit={submit} noValidate><h2>Crear cuenta</h2><p className="login-subtitle">Completa tus datos para comenzar.</p>
      {error && <p className="auth-error" role="alert">{error}</p>}
      <label htmlFor="name">Nombre completo</label><input id="name" type="text" value={datos.nombre} onChange={actualizar('nombre')} placeholder="Tu nombre" required />
      <label htmlFor="register-email">Correo electrónico</label><input id="register-email" type="email" value={datos.correo} onChange={actualizar('correo')} placeholder="Tu correo" required />
      <label htmlFor="register-password">Contraseña</label><input id="register-password" type="password" value={datos.password} onChange={actualizar('password')} minLength="8" placeholder="Crea una contraseña" onFocus={() => setPasswordFocused(true)} onBlur={() => setPasswordFocused(false)} required />
      <label htmlFor="confirm-password">Confirmar contraseña</label><input id="confirm-password" type="password" value={confirmacion} onChange={(event) => setConfirmacion(event.target.value)} minLength="8" placeholder="Repite tu contraseña" onFocus={() => setPasswordFocused(true)} onBlur={() => setPasswordFocused(false)} required />
      <label className="remember"><input type="checkbox" checked={aceptoTerminos} onChange={(event) => { setAceptoTerminos(event.target.checked); setError(''); }} /> <span>Acepto los términos y condiciones</span></label><button className="login-submit" type="submit" disabled={cargando}>{cargando ? 'Creando…' : 'Crear mi cuenta'} <span>→</span></button>
      <p className="register-prompt auth-bottom-text">¿Ya tienes una cuenta? <button type="button" className="inline-link" onClick={onLogin}>Iniciar sesión</button></p>
    </form>
  </AuthFrame>;
}

function Recovery({ light, onToggle, onBack, onLogin, onRegister }) {
  const [step, setStep] = useState(1);
  const [passwordFocused, setPasswordFocused] = useState(false);
  const [correo, setCorreo] = useState('');
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const submitStep = async (event) => {
    event.preventDefault();
    if (step === 1 && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim())) {
      setError('Ingresa un correo electrónico válido.');
      return;
    }
    if (step === 2) {
      if (!token.trim()) { setError('Ingresa el código de recuperación.'); return; }
      setError(''); setStep(3);
      return;
    }
    if (step === 3) {
      if (password.length < 8) { setError('La contraseña debe tener al menos 8 caracteres.'); return; }
      if (password !== confirmacion) { setError('Las contraseñas no coinciden.'); return; }
      setCargando(true);
      try { await restablecerPassword({ correo, token, password }); setError(''); onLogin(); }
      catch (exception) { setError(exception.message || 'No fue posible cambiar la contraseña.'); }
      finally { setCargando(false); }
      return;
    }
    setError('');
    setCargando(true);
    try {
      const resultado = await solicitarRecuperacion(correo.trim());
      if (resultado.token) setToken(resultado.token);
      setStep(2);
    } catch (exception) {
      setError(exception.message || 'No fue posible verificar el correo.');
    } finally {
      setCargando(false);
    }
  };
  return <AuthFrame light={light} onToggle={onToggle} onBack={onBack} kicker="RECUPERA TU ACCESO" title="Vuelve a tu espacio." accentTitle="sin perder el hilo." description="Te acompañamos paso a paso para recuperar tu cuenta de forma segura." sleeping={passwordFocused}>
    <form onSubmit={submitStep}><div className="recovery-progress"><span className={step >= 1 ? 'active' : ''}>1</span><i /><span className={step >= 2 ? 'active' : ''}>2</span><i /><span className={step >= 3 ? 'active' : ''}>3</span></div>
      {step === 1 && <><h2>Recuperar contraseña</h2><p className="login-subtitle">Escribe tu correo y te enviaremos un código de verificación.</p>{error && <p className="auth-error" role="alert">{error}</p>}<label htmlFor="recovery-email">Correo electrónico</label><input id="recovery-email" type="email" value={correo} onChange={(event) => setCorreo(event.target.value)} placeholder="tu correo@ejemplo.com" required /><button className="login-submit" type="submit" disabled={cargando}>{cargando ? 'Verificando…' : 'Enviar código'} <span>→</span></button></>}
       {step === 2 && <><h2>Verifica tu correo</h2><p className="login-subtitle">Ingresa el código de recuperación que recibiste.</p>{error && <p className="auth-error" role="alert">{error}</p>}<label htmlFor="verification-code">Código de recuperación</label><input id="verification-code" className="verification-input" value={token} onChange={(event) => setToken(event.target.value)} placeholder="Pega tu código" /><button className="login-submit" type="submit">Verificar código <span>→</span></button><button className="secondary-auth-button" type="button" onClick={() => setStep(1)}>Usar otro correo</button></>}
       {step === 3 && <><h2>Nueva contraseña</h2><p className="login-subtitle">Crea una contraseña segura para proteger tu cuenta.</p>{error && <p className="auth-error" role="alert">{error}</p>}<label htmlFor="new-password">Nueva contraseña</label><input id="new-password" type="password" value={password} placeholder="Ingresa tu nueva contraseña" onChange={(event) => setPassword(event.target.value)} onFocus={() => setPasswordFocused(true)} onBlur={() => setPasswordFocused(false)} /><label htmlFor="repeat-password">Repetir contraseña</label><input id="repeat-password" type="password" value={confirmacion} placeholder="Repite tu contraseña" onChange={(event) => setConfirmacion(event.target.value)} onFocus={() => setPasswordFocused(true)} onBlur={() => setPasswordFocused(false)} /><button className="login-submit" type="submit" disabled={cargando}>{cargando ? 'Guardando…' : 'Guardar contraseña'} <span>→</span></button></>}
      <p className="register-prompt auth-bottom-text"><button type="button" className="inline-link" onClick={onLogin}>← Volver a iniciar sesión</button></p>
    </form>
  </AuthFrame>;
}

export function App() {
  const [light, setLight] = useState(false);
  const [usuario, setUsuario] = useState(() => obtenerUsuario());
  const [vista, setVista] = useState(() => (obtenerToken() ? 'app' : 'inicio'));
  const authProps = { light, onToggle: () => setLight(!light), onBack: () => setVista('inicio') };
  const entrar = (datosUsuario) => {
    setUsuario(datosUsuario);
    setVista('app');
  };
  const salir = () => {
    cerrarSesion();
    setUsuario(null);
    setVista('inicio');
  };
  if (vista === 'app') return <PanelConversaciones usuario={usuario} light={light} onToggleTheme={() => setLight(!light)} onLogout={salir} onProfile={() => setVista('perfil')} />;
  if (vista === 'perfil') return <Perfil usuario={usuario} light={light} onToggleTheme={() => setLight(!light)} onBack={() => setVista('app')} onLogout={salir} />;
  if (vista === 'login') return <div className={light ? 'light-theme' : ''}><Login {...authProps} onRegister={() => setVista('registro')} onForgot={() => setVista('recuperar')} onAuth={entrar} /></div>;
  if (vista === 'registro') return <div className={light ? 'light-theme' : ''}><Register {...authProps} onLogin={() => setVista('login')} onAuth={entrar} /></div>;
  if (vista === 'recuperar') return <div className={light ? 'light-theme' : ''}><Recovery {...authProps} onLogin={() => setVista('login')} onRegister={() => setVista('registro')} /></div>;
  return (
    <main className={`landing-page ${light ? 'light-theme' : ''}`} id="inicio">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <nav className="navigation" aria-label="Navegación principal">
        <Brand />
        <div className="nav-links">
          <a href="#caracteristicas">Características</a>
        </div>
        <div className="nav-actions"><ThemeToggle light={light} onToggle={() => setLight(!light)} /><button className="nav-login" onClick={() => setVista('login')}>Iniciar sesión <span>↗</span></button></div>
      </nav>

      <section className="hero-section">
        <div className="hero-copy">
          <div className="status-pill"><span className="pulse-dot" /> Comunicación que fluye</div>
          <InteractiveTitle lines={[{ text: 'Conecta.' }, { text: 'Conversa.', accent: true }, { text: 'Sincroniza.' }]} />
          <p className="hero-description">
            SyncChat reúne a tus equipos en un espacio simple para conversar,
            compartir ideas y mantener cada mensaje en movimiento.
          </p>
          <div className="hero-actions">
            <button className="primary-button" onClick={() => setVista('login')}>Comenzar ahora <span>→</span></button>
            <a className="text-button" href="#como-funciona">Descubre SyncChat <span>↓</span></a>
          </div>
          <div className="hero-note"><span>✦</span> Mensajería instantánea para equipos conectados</div>
        </div>

        <div className="hero-visual" aria-label="Vista previa de una conversación en SyncChat">
          <div className="visual-orbit orbit-one" />
          <div className="visual-orbit orbit-two" />
          <div className="chat-preview">
            <div className="preview-topbar">
              <div className="preview-brand"><span className="mini-mark">S</span> SyncChat</div>
              <span className="preview-menu">•••</span>
            </div>
            <div className="preview-heading">
              <div><span className="preview-label">CONVERSACIÓN ACTIVA</span><h2>Equipo creativo</h2></div>
              <span className="online-count"><i /> 8 en línea</span>
            </div>
            <div className="preview-messages">
              <div className="preview-message incoming"><span className="message-avatar avatar-purple">HC</span><div><strong>Harina C.</strong><p>¿Listos para compartir las ideas?</p><time>10:42</time></div></div>
              <div className="preview-message outgoing"><div><strong>Tú</strong><p>Ya tengo una propuesta. ¡La envío ahora!</p><time>10:43</time></div><span className="message-avatar avatar-coral">AM</span></div>
              <div className="typing-indicator"><span /><span /><span /> Alguien está escribiendo</div>
            </div>
            <div className="preview-composer"><span>Escribe un mensaje...</span><button aria-label="Enviar mensaje">↑</button></div>
          </div>
          <div className="floating-card floating-online"><span className="floating-icon">✓</span><div><strong>Conexión activa</strong><small>Todo sincronizado</small></div></div>
          <div className="floating-card floating-event"><span className="event-line" /><div><strong>Mensaje entregado</strong><small>Hace un momento</small></div></div>
        </div>
      </section>

      <section className="features-section" id="caracteristicas">
        <div className="section-heading" id="como-funciona"><span>UNA NUEVA FORMA DE CONECTAR</span><h2>Lo esencial para conversar mejor.</h2></div>
        <div className="feature-grid">
          {features.map((feature) => <article className="feature-card" key={feature.title}><span className="feature-icon">{feature.icon}</span><h3>{feature.title}</h3><p>{feature.text}</p></article>)}
        </div>
      </section>

      <footer className="landing-footer"><Brand /><span>Comunicación en tiempo real, hecha simple.</span><span>© 2026 SyncChat</span></footer>
    </main>
  );
}
