export const conversaciones = [
  { id: 'a4b565b0-9e72-4dd5-8a34-c4d9e1a56101', nombre: 'Equipo de producto', iniciales: 'EP', color: 'coral' },
  { id: 'a4b565b0-9e72-4dd5-8a34-c4d9e1a56102', nombre: 'Diseño y experiencia', iniciales: 'DE', color: 'blue' },
  { id: 'a4b565b0-9e72-4dd5-8a34-c4d9e1a56103', nombre: 'General', iniciales: 'GE', color: 'green' }
];

export function Conversaciones({ activa, onSelect }) {
  return <nav className="conversation-list" aria-label="Conversaciones">{conversaciones.map((item) => <button className={`conversation ${activa?.id === item.id ? 'active' : ''}`} key={item.id} onClick={() => onSelect(item)}><span className={`conversation-icon ${item.color}`}>{item.iniciales}</span><span className="conversation-name">{item.nombre}</span><span className="conversation-arrow">›</span></button>)}</nav>;
}
