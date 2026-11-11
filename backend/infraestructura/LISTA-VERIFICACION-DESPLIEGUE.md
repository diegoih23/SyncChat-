# Lista de verificación para despliegue y aceptación

## A. Antes de distribuir a las cuatro laptops

- [ ] Cada equipo tiene la misma revisión del repositorio (código, Dockerfiles y Compose).
- [ ] Cada `.env` tiene las cuatro IP reales y los mismos `JWT_SECRET`, `AUTH_DIRECTORY_TOKEN` y credenciales RabbitMQ.
- [ ] Los secretos no son los de ejemplo; permisos del `.env`: `600`.
- [ ] Ejecutado `backend/infraestructura/preflight-despliegue.sh backend/infraestructura/.env` en cada laptop.
- [ ] En Josibel: backup de conversaciones y aplicación de `002`, `003` y `004` en ese orden.
- [ ] Los puertos de PostgreSQL y el panel RabbitMQ no están publicados. Abrir únicamente 3000/8080 en Juan, 5672 RabbitMQ en Juan, 8084 en Wagner, 8082 en Josibel y 8085 en Emilio, restringidos a la LAN.
- [ ] `JWT_SECRET`, `AUTH_DIRECTORY_TOKEN` y RabbitMQ iguales en los equipos necesarios; no reutilizar secretos entre instalaciones diferentes.
- [ ] La red Wi-Fi no es de invitados y permite conexiones entre dispositivos; no usar port-forwarding de Internet.

## B. Despliegue

Desde la raíz del repo, iniciar/reconstruir en este orden, usando el archivo y `.env` de cada equipo:

1. Juan: `docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml up -d --build`
2. Josibel: `docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-josibel.yml up -d --build`
3. Wagner: `docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-wagner.yml up -d --build`
4. Emilio: `docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-emilio.yml up -d --build`

No usar `down -v`. Las imágenes se reconstruyen por equipo; no se comparte el daemon de Docker entre laptops.

## C. Prueba local previa (sin laptops remotas)

Ejecutar contra el Gateway de la instancia local:

```bash
SYNCCHAT_API_BASE=http://localhost:18080/api \
  node backend/pruebas/integracion/probar-chat-local.mjs
```

El script registra cuentas con correos únicos, prueba una conversación de dos personas y un grupo, verifica invitaciones pendientes, aceptación, mensajes, historial, distribución WebSocket, reintento idempotente y denegación a un usuario ajeno. Limpia solo los mensajes, conversaciones y cuentas temporales que él mismo creó.

## D. Desde un celular en la misma Wi-Fi

- [ ] Abrir `http://IP_DE_JUAN:3000` (nunca `localhost` desde el celular).
- [ ] Registrar/iniciar sesión con una cuenta de prueba.
- [ ] Crear un chat, invitar por correo y confirmar que el otro usuario aparece como pendiente.
- [ ] Mientras está pendiente, comprobar que no puede abrir el chat ni consultar su historial.
- [ ] Aceptar la invitación en el segundo dispositivo; comprobar que el chat ya aparece y los participantes muestran nombre/estado, nunca UUID.
- [ ] Enviar mensajes en ambas direcciones sin recargar; la interfaz debe indicar WebSocket conectado.
- [ ] Enviar desde un grupo y confirmar que todas las sesiones conectadas de miembros aceptados lo reciben.
- [ ] Confirmar que un usuario ajeno no puede entrar aunque conozca el ID de la conversación.
- [ ] Apagar Wi-Fi o cerrar el navegador del celular, enviar un mensaje desde otro miembro y volver a abrir el chat: el historial debe recuperar el mensaje.
- [ ] Desconectar solo WebSocket si es posible; la interfaz debe mostrar claramente el estado sin afirmar entrega realtime y sincronizar historial al reconectar.

## E. Desde otra laptop

- [ ] Abrir la misma URL `http://IP_DE_JUAN:3000` desde una laptop distinta.
- [ ] Verificar login, invitación por correo, aceptar/rechazar y vista de participantes/estados.
- [ ] Dejar una sesión abierta en la primera laptop y otra en la segunda: comprobar entrega realtime y varias sesiones.
- [ ] Reiniciar el navegador de una sesión y comprobar recuperación mediante historial.
- [ ] Revisar que no hay errores de CORS, JWT ni WebSocket (`101 Switching Protocols`) en DevTools.

## F. Salud, red y seguridad

Desde cualquier laptop después del arranque:

```bash
bash backend/infraestructura/distribuido/verificar-red.sh backend/infraestructura/.env
```

- [ ] Gateway y servicios remotos responden `UP`.
- [ ] RabbitMQ acepta conexiones solo desde la LAN autorizada; el panel 15672 no se publica.
- [ ] PostgreSQL no tiene puertos publicados en `docker compose ps`.
- [ ] El Gateway solo permite el origen del frontend de Juan; si cambia IP/puerto, actualizar CORS y recrear Gateway.
- [ ] No se usan secretos de ejemplo, no se registran tokens en logs y el `.env` no se comparte ni se commitea.

> HTTP sin TLS es aceptable únicamente para esta práctica en una LAN de confianza. Para redes no confiables, publicar la aplicación mediante HTTPS/WSS antes de usarla.
