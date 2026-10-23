const conversaciones = [
  { nombre: 'Equipo de producto', iniciales: 'EP', color: 'coral', mensajes: 3 },
  { nombre: 'Diseño y experiencia', iniciales: 'DE', color: 'blue', mensajes: 0 },
  { nombre: 'General', iniciales: 'GE', color: 'green', mensajes: 0 }
];

export function Conversaciones({ activa, onSelect }) {
  return <nav className="conversation-list">{conversaciones.map((item) => <button className={`conversation ${activa === item.nombre ? 'active' : ''}`} key={item.nombre} onClick={() => onSelect(item.nombre)}><span className={`conversation-icon ${item.color}`}>{item.iniciales}</span><span className="conversation-name">{item.nombre}</span>{item.mensajes > 0 && <span className="unread">{item.mensajes}</span>}</button>)}</nav>;
}
