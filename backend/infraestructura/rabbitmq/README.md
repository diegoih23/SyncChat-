# RabbitMQ

RabbitMQ se levanta mediante `../docker-compose.yml`. La consola de administracion esta disponible en `http://localhost:15672` con las credenciales configuradas en el archivo `.env`.

## Topología de eventos

- Exchange: `chat.events` (tipo `topic`)
- Cola: `chat.message.created`
- Routing key: `chat.message.created`

El productor está en `servicio-mensajes` y el consumidor en `servicio-tiempo-real`. El evento publicado es:

```json
{
  "id": "uuid",
  "conversacionId": "uuid",
  "remitenteId": "uuid",
  "destinatarioId": "uuid",
  "contenido": "Hola",
  "creadoEn": "2026-01-01T12:00:00Z"
}
```

Para probarlo con el Gateway, primero inicia Docker Compose y publica un mensaje:

```bash
curl -X POST http://localhost:8080/api/mensajes \
  -H 'Content-Type: application/json' \
  -d '{"conversacionId":"11111111-1111-1111-1111-111111111111","remitenteId":"22222222-2222-2222-2222-222222222222","destinatarioId":"33333333-3333-3333-3333-333333333333","contenido":"Hola"}'
```

La respuesta `202 Accepted` confirma la publicación. Un cliente conectado al WebSocket con un JWT cuyo `sub` sea `33333333-3333-3333-3333-333333333333` recibe el evento.
