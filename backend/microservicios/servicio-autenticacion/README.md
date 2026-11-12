# Servicio de autenticación

## Ejecución

Desde `backend/microservicios/servicio-autenticacion`:

```bash
mvn test
mvn spring-boot:run
```

La ejecución completa usa `backend/infraestructura/docker-compose.yml`, que conecta el servicio con `postgres-usuarios`. La colección `postman/SyncChat-Autenticacion.postman_collection.json` usa el Gateway en `http://localhost:8080`.

## Endpoints

| Método | Ruta externa | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | Crea un usuario y devuelve JWT |
| POST | `/api/auth/login` | Valida correo/contraseña y devuelve JWT |
| GET | `/api/auth/perfil/{id}` | Consulta el perfil público |
| POST | `/api/auth/recuperar` | Genera token de recuperación de 15 minutos |
| POST | `/api/auth/restablecer` | Cambia la contraseña usando el token |

Las contraseñas y tokens de recuperación se almacenan con BCrypt. En desarrollo el token de recuperación se devuelve en la respuesta para poder probar el flujo; en producción debe enviarse por correo y omitirse de la respuesta.
