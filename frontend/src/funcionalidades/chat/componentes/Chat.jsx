import { useState } from 'react';

export function Chat({ nombre }) {
  const [texto, setTexto] = useState('');
  const [mensajes, setMensajes] = useState([{ autor: 'Mia Chen', iniciales: 'MC', hora: '09:41', texto: 'Buenos dias equipo. Ya esta lista la nueva propuesta para revisar.' }, { autor: 'Alex Morgan', iniciales: 'AM', hora: '09:43', texto: 'La estoy viendo ahora. Me gusta mucho la direccion visual.' }]);
  const enviar = (evento) => { evento.preventDefault(); if (!texto.trim()) return; setMensajes([...mensajes, { autor: 'Alex Morgan', iniciales: 'AM', hora: 'Ahora', texto: texto.trim() }]); setTexto(''); };
  return <div className="chat-panel"><header className="chat-header"><div><p className="eyebrow">Conversacion</p><h1>{nombre}</h1><span className="online"><i /> 8 personas activas</span></div><button className="details-button">Detalles <span>...</span></button></header><div className="messages">{mensajes.map((mensaje, indice) => <article className="message" key={`${mensaje.hora}-${indice}`}><span className={`avatar ${mensaje.iniciales === 'MC' ? 'purple' : ''}`}>{mensaje.iniciales}</span><div><div className="message-meta"><strong>{mensaje.autor}</strong><time>{mensaje.hora}</time></div><p>{mensaje.texto}</p></div></article>)}</div><form className="composer" onSubmit={enviar}><input value={texto} onChange={(evento) => setTexto(evento.target.value)} placeholder="Escribe un mensaje..." aria-label="Mensaje" /><button type="submit">Enviar</button></form></div>;
}
