# Despliegue distribuido en cuatro computadoras

> **OBSOLETO:** este directorio conserva Compose y pasos de una versión anterior
> (incluye puertos de PostgreSQL/RabbitMQ expuestos y flujo por UUID). No ejecutes
> estos archivos. La guía canónica es `../README-distribuido.md`, y las pruebas
> para esta versión están en `../LISTA-VERIFICACION-DESPLIEGUE.md`.

Cada computadora debe clonar el mismo repositorio y ejecutar únicamente su archivo Compose.
Todas deben estar en la misma red local y usar el mismo `JWT_SECRET`.

## Distribución

| Equipo | Archivo | Componentes |
|---|---|---|
| Juan | `docker-compose-juan.yml` | Gateway, autenticación, frontend, RabbitMQ y PostgreSQL de usuarios |
| Wagner | `docker-compose-wagner.yml` | Mensajes y PostgreSQL de mensajes |
| Josibel | `docker-compose-josibel.yml` | Conversaciones y PostgreSQL de conversaciones |
| Emilio | `docker-compose-emilio.yml` | Tiempo real/WebSocket |

## Preparación común

Configurar las IP de las cuatro computadoras en el archivo `.env` correspondiente:

```env
JUAN_IP=192.168.1.10
WAGNER_IP=192.168.1.11
JOSIBEL_IP=192.168.1.12
EMILIO_IP=192.168.1.13
JWT_SECRET=cambiar-por-el-mismo-secreto-en-los-cuatro-equipos
```

Abrir los puertos de cada equipo en el firewall y comprobar:

```bash
ping $JUAN_IP
ping $WAGNER_IP
ping $JOSIBEL_IP
ping $EMILIO_IP
```

## Orden de inicio

1. Juan inicia PostgreSQL de usuarios y RabbitMQ.
2. Wagner inicia PostgreSQL de mensajes y el servicio de mensajes.
3. Josibel inicia PostgreSQL de conversaciones y su servicio.
4. Emilio inicia el servicio de tiempo real.
5. Juan inicia autenticación, Gateway y frontend.

Los comandos detallados están en cada archivo `.env.*.example` y en las secciones siguientes.

## Puertos

| Equipo | Puertos |
|---|---|
| Juan | `3000`, `8080`, `8081`, `5433`, `5672`, `15672` |
| Wagner | `8084`, `5435` |
| Josibel | `8082`, `5434` |
| Emilio | `8085` |

No deben iniciar el Compose local general en las cuatro computadoras, porque intentaría levantar todos los servicios en cada una.

## Validación final entre las cuatro computadoras

Después de iniciar los cuatro Compose, ejecutar desde cualquier equipo:

```bash
bash verificar-red.sh .env
```

El script comprueba los healthchecks de Gateway, autenticación, mensajes, conversaciones y tiempo real, además de los puertos TCP principales.

### Prueba de autenticación

Desde el navegador de Juan abrir:

```text
http://IP_DE_JUAN:3000
```

Crear un usuario e iniciar sesión. El frontend debe comunicarse con el Gateway de Juan y el Gateway debe enrutar las solicitudes a autenticación.

### Prueba de conversaciones

Con la sesión iniciada:

1. Crear una conversación.
2. Introducir el UUID del usuario destinatario.
3. Confirmar que aparece en la lista.

Si falla, revisar los logs de Josibel:

```bash
docker compose --env-file .env -f docker-compose-josibel.yml logs -f servicio-conversaciones
```

### Prueba de tiempo real

Abrir dos navegadores o dos equipos con usuarios distintos. Ambos deben iniciar sesión y conectarse a la misma conversación.

1. El navegador abre automáticamente `/ws/mensajes?token=JWT` mediante el Gateway.
2. Wagner recibe el `POST /api/mensajes`.
3. Wagner guarda el mensaje en PostgreSQL y lo publica en `chat.events`.
4. RabbitMQ de Juan entrega el evento a la cola `chat.message.created`.
5. Emilio consume el evento.
6. Emilio envía el evento al WebSocket del destinatario.
7. El segundo navegador muestra el mensaje sin recargar.

Logs útiles durante la prueba:

```bash
# Juan
docker compose --env-file .env -f docker-compose-juan.yml logs -f gateway rabbitmq

# Wagner
docker compose --env-file .env -f docker-compose-wagner.yml logs -f servicio-mensajes

# Emilio
docker compose --env-file .env -f docker-compose-emilio.yml logs -f servicio-tiempo-real
```

### Criterio de aceptación

La integración distribuida se considera correcta si:

- El frontend carga desde Juan.
- El registro y login funcionan.
- Las conversaciones se crean en la computadora de Josibel.
- Los mensajes se guardan en la computadora de Wagner.
- RabbitMQ muestra el evento publicado.
- Emilio recibe el evento y mantiene el WebSocket conectado.
- El destinatario ve el mensaje en tiempo real.
- Al recargar, el mensaje permanece en el historial.
