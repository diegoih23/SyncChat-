# Infraestructura local

## Inicio

Docker Compose construye automáticamente los JAR de los servicios Java mediante Dockerfiles multi-etapa. No es necesario ejecutar Maven antes.

```bash
docker compose -f backend/infraestructura/docker-compose.yml up --build
```

Las bases de datos y RabbitMQ tienen healthchecks. Los servicios Java dependen de la disponibilidad de sus recursos de infraestructura; el Gateway y el frontend se inician después de los servicios backend.

## Verificaciones rápidas

```bash
docker compose -f backend/infraestructura/docker-compose.yml ps
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8080/actuator/health
```

PostgreSQL expone sus puertos en el host como `5433` (usuarios), `5434` (conversaciones) y `5435` (mensajes). Los servicios usan `8081` (autenticación), `8082` (conversaciones), `8084` (mensajes), `8085` (tiempo real) y `8080` (Gateway). RabbitMQ usa `5672` y su consola de administración `15672`. Todos los puertos del host pueden sobrescribirse mediante variables `*_PORT_HOST`.

Para comprobar cada base de datos:

```bash
docker exec -it syncchat-postgres-usuarios psql -U syncchat -d usuarios -c '\\dt'
docker exec -it syncchat-postgres-conversaciones psql -U syncchat -d conversaciones -c '\\dt'
docker exec -it syncchat-postgres-mensajes psql -U syncchat -d mensajes -c '\\dt'
```

La conexión WebSocket se prueba con `ws://localhost:8080/ws/mensajes?token=<jwt>`. El usuario se obtiene del claim `sub` del JWT. La autenticación se prueba mediante la colección de Postman del servicio de autenticación.
