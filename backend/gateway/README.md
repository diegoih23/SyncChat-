# API Gateway

El Gateway escucha en el puerto `8080` y centraliza las rutas:

| Ruta | Servicio | Protección |
|---|---|---|
| `/api/auth/**` | autenticación: `8081` | pública |
| `/api/conversaciones/**` | conversaciones: `8082` | JWT |
| `/api/mensajes/**` | mensajes: `8084` | JWT |
| `/ws/mensajes/**` | tiempo real: `8085` | WebSocket con JWT |

Las rutas hacia autenticación eliminan los prefijos `/api/auth` antes de reenviar. Las variables `AUTH_SERVICE_URL`, `CONVERSATIONS_SERVICE_URL`, `MESSAGES_SERVICE_URL` y `REALTIME_WS_URL` permiten cambiar los destinos dentro de Docker.

El filtro JWT verifica firma y expiración usando el mismo `JWT_SECRET` del servicio de autenticación. CORS permite por defecto `http://localhost:3000` y `http://localhost:5173`; puede cambiarse mediante `CORS_ALLOWED_ORIGINS`.

El frontend agrega automáticamente el token guardado en `localStorage` o `sessionStorage` mediante `frontend/src/servicios/api.js`.
