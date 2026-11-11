# Servicio de tiempo real

## WebSocket

La ruta es `/ws/mensajes`. Cada cliente debe enviar su JWT mediante el parámetro `token`:

```javascript
const token = localStorage.getItem('syncchat_token');
const socket = new WebSocket(`ws://localhost:8080/ws/mensajes?token=${encodeURIComponent(token)}`);
socket.onmessage = (evento) => console.log(JSON.parse(evento.data));
```

El servicio valida firma y expiración del JWT con `JWT_SECRET`, obtiene el usuario desde el claim `sub` y rechaza la conexión si el token falta o es inválido. El Gateway enruta la conexión a este servicio. Las sesiones se guardan en memoria y se eliminan cuando el socket se cierra. Un usuario puede tener varias sesiones abiertas.

## RabbitMQ

El servicio escucha la cola `tiempo-real.mensajes`, enlazada al exchange `syncchat.mensajes` con la clave `mensaje.creado`. El evento debe contener `destinatarioId`, por ejemplo:

```json
{
  "id": "mensaje-1",
  "conversacionId": "conversacion-1",
  "remitenteId": "usuario-2",
  "destinatarioId": "usuario-1",
  "contenido": "Hola",
  "creadoEn": "2026-11-10T20:00:00Z"
}
```

El evento se entrega únicamente a las sesiones abiertas del destinatario. Si no está conectado, permanece sin entregar en el flujo de tiempo real; la persistencia corresponde al servicio de mensajes.
