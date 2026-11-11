# Ejecutar SyncChat en cuatro computadoras

Guía para la implementación actual con Docker. **Seguir este orden: Juan →
Josibel → Wagner → Emilio.** Todos los comandos se ejecutan desde la raíz del
repositorio, donde están las carpetas `backend` y `frontend`.

| Persona | Qué ejecuta |
| --- | --- |
| Juan | Autenticación, Gateway, frontend, RabbitMQ y PostgreSQL usuarios |
| Josibel | Conversaciones y PostgreSQL conversaciones |
| Wagner | Mensajes y PostgreSQL mensajes |
| Emilio | Tiempo real / WebSocket, sin base de datos |

No ejecutar Maven ni el Compose completo al mismo tiempo que estos archivos.
No es necesario instalar PostgreSQL, RabbitMQ o Node en las computadoras.

## 1. Preparación de los cuatro equipos

### 1.1. Revisar las herramientas

Cada integrante necesita el repositorio completo y actualizado, Docker iniciado,
Docker Compose v2, Python 3, OpenSSL y curl. Comprobar:

```bash
docker info
docker compose version
python3 --version
openssl version
curl --version
```

Si Docker muestra un error de permisos, resolver el acceso a Docker según la
instalación del equipo antes de continuar. La primera construcción necesita Internet.

### 1.2. Conectarse a la misma red y compartir las IP

En cada equipo:

```bash
hostname -I
```

Elegir la IP de la conexión al router, no una dirección interna de Docker.
Anotar las cuatro. En esta guía se usan estos ejemplos:

| Juan | Wagner | Josibel | Emilio |
| --- | --- | --- | --- |
| 192.168.1.10 | 192.168.1.11 | 192.168.1.12 | 192.168.1.13 |

**Reemplazar estas direcciones por las reales en todos los pasos.**
Probar desde cada equipo las IP de los demás, por ejemplo:

```bash
ping -c 4 192.168.1.10
```

Si no responde, revisar la red, el firewall y el aislamiento de clientes Wi-Fi.
Algunas redes bloquean ping: las pruebas HTTP posteriores confirman el acceso real.

### 1.3. Crear el archivo de configuración

En cada computadora, desde la raíz del proyecto:

```bash
cp -n backend/infraestructura/.env.example backend/infraestructura/.env
nano backend/infraestructura/.env
```

`cp -n` conserva el archivo si ya existe. Configurar las IP reales:

```dotenv
JUAN_IP=192.168.1.10
WAGNER_IP=192.168.1.11
JOSIBEL_IP=192.168.1.12
EMILIO_IP=192.168.1.13
```

Escribir solo la IP, sin `http://` ni puertos.

### 1.4. Crear las contraseñas y los secretos UNA SOLA VEZ

Juan ejecuta estos comandos. Cada ejecución genera un valor diferente:

```bash
# Primer resultado: JWT_SECRET.
openssl rand -hex 32
# Segundo resultado: AUTH_DIRECTORY_TOKEN.
openssl rand -hex 32
# Tercer resultado: POSTGRES_PASSWORD.
openssl rand -hex 24
# Cuarto resultado: RABBITMQ_PASSWORD.
openssl rand -hex 24
```

Copiar cada resultado en su variable correspondiente del `.env`, reemplazando
los textos `REEMPLAZAR...`. Mantener:

```dotenv
POSTGRES_USER=syncchat
RABBITMQ_USER=syncchat
```

Compartir esos cuatro valores con los compañeros por un medio seguro, por ejemplo
un gestor de contraseñas. **Todos deben usar los mismos valores; no generar
otros en cada computadora.** No enviarlos por chats ni subir el `.env` a Git.

En Nano: guardar con `Ctrl+O`, Enter y salir con `Ctrl+X`. Luego, en cada equipo:

```bash
chmod 600 backend/infraestructura/.env
```

Para estos Compose distribuidos, el origen permitido del frontend se configura
automáticamente con `JUAN_IP` y el puerto 3000. No hace falta editar
`CORS_ALLOWED_ORIGINS` para esta ejecución ni configurar `VITE_API_URL`.

### 1.5. Validar antes de iniciar

En los cuatro equipos:

```bash
bash backend/infraestructura/preflight-despliegue.sh backend/infraestructura/.env
```

Continuar cuando termine con `[OK]`. Si muestra `[FALLO]`, corregir lo indicado
y repetir. Esta prueba revisa IP, secretos y archivos Compose; todavía no prueba
las conexiones entre computadoras.

## 2. Permitir las conexiones de red

Solo se necesitan estos puertos TCP:

| Equipo | Puertos | Quién los utiliza |
| --- | --- | --- |
| Juan | 3000, 8080 | Navegadores y servicios remotos |
| Juan | 5672 | Mensajes de Wagner y tiempo real de Emilio |
| Josibel | 8082 | Gateway de Juan y mensajes de Wagner |
| Wagner | 8084 | Gateway de Juan |
| Emilio | 8085 | Gateway de Juan |

Si usan UFW en Linux, ejecutar las reglas de su equipo. Cambiar
`192.168.1.0/24` si su LAN utiliza otra subred.

**Juan:**

```bash
sudo ufw allow from 192.168.1.0/24 to any port 3000 proto tcp
sudo ufw allow from 192.168.1.0/24 to any port 8080 proto tcp
sudo ufw allow from 192.168.1.0/24 to any port 5672 proto tcp
```

**Josibel:**

```bash
sudo ufw allow from 192.168.1.0/24 to any port 8082 proto tcp
```

**Wagner:**

```bash
sudo ufw allow from 192.168.1.0/24 to any port 8084 proto tcp
```

**Emilio:**

```bash
sudo ufw allow from 192.168.1.0/24 to any port 8085 proto tcp
```

No se publican PostgreSQL, autenticación ni el panel RabbitMQ. No intentar
acceder a 5433, 5434, 5435, 8081 o 15672 desde el host: ya no se usan para
este despliegue. Las bases y autenticación se comunican por la red interna Docker.

Usar una LAN de confianza y no abrir estos puertos en el router hacia Internet.
Docker puede publicar puertos mediante reglas propias que no siempre quedan
restringidas por UFW. No activar ni cambiar el firewall remotamente sin asegurar
primero el acceso de administración.

## 3. Juan: iniciar primero

### Paso 1. Levantar sus servicios

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml up -d --build
```

### Paso 2. Revisar el estado

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml ps
curl http://localhost:8080/actuator/health
```

Esperar que los servicios con healthcheck estén `healthy` y que el frontend esté
en ejecución. El `curl` debe responder `{"status":"UP"}`.

### Paso 3. Confirmar RabbitMQ y avisar a Josibel

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml exec rabbitmq rabbitmq-diagnostics -q ping
```

Cuando la prueba sea correcta, avisar a los compañeros. El Gateway puede estar
`UP` antes que ellos, pero la aplicación completa todavía no funcionará.

## 4. Josibel: iniciar conversaciones

### Paso 1. Comprobar que puede acceder a Juan

```bash
curl http://192.168.1.10:8080/actuator/health
```

Debe responder `UP`. Conversaciones consulta usuarios mediante el Gateway de Juan;
por eso necesita la IP de Juan y el `AUTH_DIRECTORY_TOKEN` compartido.

### Paso 2. Iniciar sus servicios

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-josibel.yml up -d --build
```

Si conserva una base de conversaciones de una versión anterior, consultar la
sección 9 antes de actualizar el servicio. Para una base nueva, los scripts se
ejecutan automáticamente al inicializar el volumen.

### Paso 3. Comprobar y avisar a Wagner

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-josibel.yml ps
curl http://localhost:8082/actuator/health
```

Cuando responda `UP`, avisar a Wagner. El puerto 8082 debe ser accesible tanto
desde Juan como desde Wagner.

## 5. Wagner: iniciar mensajes

### Paso 1. Comprobar conversaciones de Josibel

```bash
curl http://192.168.1.12:8082/actuator/health
```

Debe responder `UP`. Mensajes consulta a conversaciones para comprobar que el
usuario tiene permiso antes de enviar. Compose configura esa conexión usando
`JOSIBEL_IP`; no hace falta escribir una URL adicional en el `.env`.

Juan también debe tener RabbitMQ listo. Si Netcat está instalado, comprobar:

```bash
nc -vz 192.168.1.10 5672
```

Esto prueba el puerto, no las credenciales.

### Paso 2. Iniciar sus servicios

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-wagner.yml up -d --build
```

### Paso 3. Comprobar

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-wagner.yml ps
curl http://localhost:8084/actuator/health
```

Esperar `UP`. Mensajes utiliza su PostgreSQL local y publica eventos en el
RabbitMQ de Juan; no inicia un RabbitMQ propio.

## 6. Emilio: iniciar tiempo real

### Paso 1. Confirmar que RabbitMQ de Juan está listo

Verificar con Juan. Si Netcat está instalado:

```bash
nc -vz 192.168.1.10 5672
```

### Paso 2. Iniciar su servicio

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-emilio.yml up -d --build
```

### Paso 3. Comprobar

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-emilio.yml ps
curl http://localhost:8085/actuator/health
```

Esperar `UP`. Este servicio recibe eventos de RabbitMQ de Juan y los entrega
por WebSocket. No necesita base de datos.

## 7. Probar la aplicación entre todos

### Paso 1. Juan comprueba los servicios remotos

```bash
curl http://192.168.1.11:8084/actuator/health
curl http://192.168.1.12:8082/actuator/health
curl http://192.168.1.13:8085/actuator/health
curl http://192.168.1.10:8080/actuator/health
```

Todos deben responder `{"status":"UP"}`. Esto comprueba salud y conectividad;
la prueba de chat siguiente comprueba el flujo completo.

### Paso 2. Abrir el frontend

Todos, incluido Juan, abren en su navegador:

```text
http://192.168.1.10:3000
```

Usar la IP real de Juan. **No usar `localhost` desde otra computadora**: apunta
a esa misma computadora, no a Juan.

El frontend envía `/api` y `/ws/mensajes` a su Nginx, que los reenvía al Gateway.
El Gateway se conecta con los servicios de cada integrante. No hay que cambiar
el código del frontend para conectarse a los otros equipos.

### Paso 3. Probar conversaciones y mensajes

1. Registrar dos usuarios y abrir una sesión distinta para cada uno.
2. Crear una conversación e invitar al segundo usuario desde la interfaz.
3. El segundo usuario debe **aceptar la invitación** para participar.
4. Enviar mensajes en ambos sentidos y comprobar que aparecen sin recargar.
5. Recargar la página y comprobar que el historial permanece.
6. Desconectar y reconectar una sesión para comprobar la recuperación del historial.

Solo los participantes aceptados pueden acceder al chat. Mensajes comprueba el
JWT y la participación; tiempo real entrega los eventos a las sesiones autorizadas.
Los reintentos de un mismo envío no deberían duplicar el mensaje.

## 8. Consultar errores, detener y volver a iniciar

Cada persona usa su archivo. Este ejemplo es para Juan; los demás sustituyen
`docker-compose-juan.yml` por el archivo de su nombre.

```bash
# Ver los últimos errores.
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml logs --tail=100

# Detener sin borrar los datos.
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml down

# Volver a iniciar o aplicar cambios del .env.
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml up -d --build
```

**No usar `down -v`: elimina los volúmenes de datos.** Un simple `restart` no
aplica variables nuevas del `.env`; usar `up -d --build`.

| Problema | Qué hacer |
| --- | --- |
| El preflight rechaza un secreto | Reemplazar los valores de ejemplo por los valores aleatorios compartidos. |
| `Connection refused` | Revisar `ps` y `logs`: el servicio puede estar detenido o arrancando. |
| La conexión no responde | Revisar IP, firewall, Wi-Fi y que el otro equipo siga encendido. |
| Puerto ocupado | Detener el despliegue anterior o el proceso que lo usa; no ejecutar Maven en paralelo. |
| Error RabbitMQ | Comprobar `JUAN_IP`, puerto 5672 y credenciales iguales a las de Juan. |
| Mensajes no autorizados | Comprobar que la invitación esté aceptada, los secretos coincidan y Wagner acceda a Josibel. |
| Mensajes sin actualización en vivo | Revisar logs de Emilio, RabbitMQ y la conexión WS en las herramientas del navegador. |
| Error CORS | Abrir `http://IP_DE_JUAN:3000`, corregir `JUAN_IP` y aplicar `up -d --build` en Juan y Emilio. |

Cambiar una contraseña en `.env` no actualiza las credenciales de una base o de
RabbitMQ ya inicializados. No borrar los volúmenes como solución sin respaldar
los datos. Si cambia una IP, actualizarla en los cuatro `.env` y aplicar de nuevo
el comando de inicio en los equipos afectados.

Conservar `docker-compose.yml`: los archivos individuales reutilizan sus
configuraciones con `extends`, sin iniciar sus demás servicios. No combinar
los cuatro archivos con varios `-f` ni iniciar el Compose completo en cada equipo.

## 9. Solo para instalaciones que ya tienen datos

**Si es una instalación nueva, omitir esta sección.**

Si antes ejecutaban todo en una computadora, detener ese despliegue con su comando
original, sin `-v`. Los datos no se trasladan automáticamente a los demás equipos:
hacer backup y restauración si necesitan conservarlos.

### Actualizar una base antigua de conversaciones

Los scripts de inicialización solo se ejecutan automáticamente cuando el volumen
está vacío. Si Josibel conserva una base de una versión anterior, primero probar
las migraciones en un PostgreSQL temporal aislado:

```bash
bash backend/base-datos/conversaciones/tests/ejecutar-prueba-migracion.sh
```

La prueba no se conecta a las bases de los equipos. Después, con PostgreSQL de
Josibel en ejecución y antes de actualizar su servicio, respaldar y migrar desde
la raíz del proyecto. Estos comandos suponen `POSTGRES_USER=syncchat`:

```bash
docker exec syncchat-postgres-conversaciones pg_dump -U syncchat conversaciones > conversaciones-backup.sql
docker exec -i syncchat-postgres-conversaciones psql -U syncchat -d conversaciones -v ON_ERROR_STOP=1 < backend/base-datos/conversaciones/002-destinatario.sql
docker exec -i syncchat-postgres-conversaciones psql -U syncchat -d conversaciones -v ON_ERROR_STOP=1 < backend/base-datos/conversaciones/003-participantes.sql
docker exec -i syncchat-postgres-conversaciones psql -U syncchat -d conversaciones -v ON_ERROR_STOP=1 < backend/base-datos/conversaciones/004-backfill-participantes.sql
```

Si un comando falla, detenerse y revisar el error antes de continuar. Las
conversaciones existentes conservan al creador como administrador aceptado;
el destinatario anterior queda pendiente y debe aceptar la invitación. La
migración `004` puede repetirse sin duplicar participantes ni cambiar respuestas
existentes. No eliminar `destinatario_id` hasta completar la transición de clientes.

### Prueba automatizada opcional, solo para desarrollo local

Este comando se conserva para una instancia local que ya esté disponible en
los puertos 18080 y 13000; **no corresponde al despliegue de cuatro equipos**:

```bash
SYNCCHAT_API_BASE=http://localhost:18080/api \
SYNCCHAT_FRONTEND_ORIGIN=http://localhost:13000 \
  node backend/pruebas/integracion/probar-chat-local.mjs
```
