# SyncChat: guía paso a paso para los cuatro integrantes

> **Aviso:** esta guía contiene pasos históricos. Para secretos, orden de inicio,
> puertos finales y pruebas, seguir `README-distribuido.md` y
> `LISTA-VERIFICACION-DESPLIEGUE.md`. PostgreSQL, autenticación y el panel
> RabbitMQ ya no se publican en la LAN.

Esta guía explica cómo conectar las computadoras e iniciar la aplicación con
Docker. No hace falta ejecutar Maven ni instalar PostgreSQL, RabbitMQ o Node
directamente: los archivos Compose construyen y ejecutan los servicios.

## 1. Distribución del trabajo

| Persona | Equipo | Servicios |
| --- | --- | --- |
| Juan | PC 1 | Autenticación, Gateway, frontend, RabbitMQ y PostgreSQL usuarios |
| Wagner | PC 2 | Mensajes y PostgreSQL mensajes |
| Josibel | PC 3 | Conversaciones y PostgreSQL conversaciones |
| Emilio | PC 4 | Tiempo real / WebSocket |

Los ejemplos usan estas IP. **Deben reemplazarlas por las IP reales de su red**:

```text
Juan:    192.168.1.10
Wagner:  192.168.1.11
Josibel: 192.168.1.12
Emilio:  192.168.1.13
```

## 2. Preparación que deben hacer TODOS

### Paso 1: conectarse a la misma red

Conectar los cuatro equipos al mismo router, por Wi-Fi o cable. Evitar redes de
invitados que impidan la comunicación entre dispositivos.

En Linux, consultar la IP:

```bash
hostname -I
```

Si aparecen varias direcciones, elegir la de la interfaz conectada al router,
no una dirección interna de Docker. Pueden consultar las interfaces con:

```bash
ip -4 addr
```

Anotar las cuatro IP. Conviene reservarlas en el router para que no cambien.

### Paso 2: comprobar conexión entre equipos

Desde cada computadora, probar las IP de los otros integrantes:

```bash
ping -c 4 192.168.1.10
ping -c 4 192.168.1.11
ping -c 4 192.168.1.12
ping -c 4 192.168.1.13
```

Una respuesta confirma conectividad básica. Si no responde, revisar la red y el
firewall; algunas redes bloquean ping aunque permitan conexiones TCP.

### Paso 3: tener Docker y el proyecto

Instalar Docker Engine y el complemento Docker Compose v2 según su sistema.
Comprobar:

```bash
docker --version
docker compose version
docker info
```

Docker debe estar iniciado. Si aparece un error de permisos, configurar el acceso
a Docker según la instalación del equipo o ejecutar los comandos Docker con `sudo`.

Cada uno necesita una copia completa del repositorio con estos archivos actualizados.
Abrir una terminal en la raíz del proyecto: allí están `backend/` y `frontend/`.
Todos los comandos siguientes se ejecutan desde esa raíz.

No ejecutar `mvn spring-boot:run` simultáneamente con los contenedores: ambos
intentarían ocupar los mismos puertos.

### Paso 4: preparar las variables

En cada equipo, crear el archivo de configuración local:

```bash
cp backend/infraestructura/.env.example backend/infraestructura/.env
nano backend/infraestructura/.env
```

Si `.env` ya existe, editarlo sin copiar encima para no perder su configuración.
Guardar las IP reales de los cuatro integrantes:

```dotenv
JUAN_IP=192.168.1.10
WAGNER_IP=192.168.1.11
JOSIBEL_IP=192.168.1.12
EMILIO_IP=192.168.1.13
JWT_SECRET=syncchat-development-secret-key-change-in-production-32
POSTGRES_USER=syncchat
POSTGRES_PASSWORD=syncchat
RABBITMQ_USER=syncchat
RABBITMQ_PASSWORD=syncchat
```

Las IP se escriben sin `http://` y sin puerto. En Nano, guardar con `Ctrl+O`,
confirmar con Enter y salir con `Ctrl+X`.

Usar secretos aleatorios compartidos según `README-distribuido.md`. No usar
credenciales de desarrollo ni subir/compartir archivos `.env`.

### Paso 5: revisar los puertos

| Equipo | Puertos TCP publicados | Uso |
| --- | --- | --- |
| Juan | 3000, 8080, 5672 | Frontend, Gateway y RabbitMQ AMQP |
| Wagner | 8084 | Mensajes |
| Josibel | 8082 | Conversaciones |
| Emilio | 8085 | Tiempo real / WebSocket |

Las secciones siguientes muestran reglas UFW para permitir las conexiones
necesarias desde la LAN. Sustituir `192.168.1.0/24` por su subred real.
No es necesario abrir PostgreSQL a los demás equipos: cada servicio usa su base
de datos por la red interna de Docker. Tampoco necesitan exponer autenticación
ni el panel RabbitMQ a otros equipos para que funcione el chat.

Si UFW está inactivo, las reglas no lo activan. No activar ni modificar un firewall
remotamente sin asegurarse de conservar el acceso de administración.
Docker puede administrar sus propias reglas de publicación y eludir ciertas
restricciones UFW: no considerar estas reglas una garantía de aislamiento.
Hacer esta práctica en una LAN de confianza, sin redirecciones de puertos del router.

## 3. Juan — PC 1

Juan debe iniciar primero porque aloja RabbitMQ, utilizado por Wagner y Emilio.

### Paso 1: revisar las IP

En `backend/infraestructura/.env`, comprobar especialmente las cuatro IP:
el Gateway necesita las de Wagner, Josibel y Emilio para enviarles solicitudes.

### Paso 2: permitir las conexiones necesarias

En Linux con UFW:

```bash
sudo ufw allow from 192.168.1.0/24 to any port 3000 proto tcp
sudo ufw allow from 192.168.1.0/24 to any port 8080 proto tcp
sudo ufw allow from 192.168.1.0/24 to any port 5672 proto tcp
```

El puerto 5672 permite que mensajes y tiempo real se conecten a RabbitMQ.
El 3000 permite abrir el frontend y el 8080 permite acceder al Gateway directamente.

### Paso 3: validar el archivo

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml config --quiet
```

Si termina sin errores, la configuración Compose es válida. Esto no prueba
todavía las conexiones de red ni el funcionamiento de los servicios.

### Paso 4: construir e iniciar

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml up -d --build
```

Este comando inicia únicamente PostgreSQL usuarios, RabbitMQ, autenticación,
Gateway y frontend. La primera construcción puede tardar porque descarga
imágenes y dependencias; se necesita acceso a Internet.

### Paso 5: comprobar el estado

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml ps
curl http://localhost:8080/actuator/health
```

Los servicios con healthcheck deben alcanzar el estado `healthy`. El frontend
no tiene healthcheck: debe aparecer en ejecución. Para los endpoints se espera
`{"status":"UP"}`. El Gateway puede estar sano aunque los otros equipos aún
no estén disponibles; las funciones remotas fallarán hasta que estos inicien.

Comprobar RabbitMQ:

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml exec rabbitmq rabbitmq-diagnostics -q ping
```

No abrir ni publicar el panel administrativo de RabbitMQ en el despliegue.

### Paso 6: avisar a los compañeros

Cuando RabbitMQ esté listo, avisar a Wagner y Emilio. Compartir la IP real de Juan
y confirmar que ambos la tienen en `JUAN_IP`.

## 4. Wagner — PC 2

### Paso 1: confirmar que Juan ya inició RabbitMQ

Revisar que `JUAN_IP` y las credenciales RabbitMQ coincidan con las de Juan.
Si tienen instalado Netcat, comprobar el puerto remoto:

```bash
nc -vz 192.168.1.10 5672
```

La conexión TCP debe tener éxito. Esto comprueba el puerto, no las credenciales.

### Paso 2: permitir conexiones al servicio de mensajes

```bash
sudo ufw allow from 192.168.1.0/24 to any port 8084 proto tcp
```

Juan usará este puerto para que su Gateway llame al servicio de mensajes.

### Paso 3: validar e iniciar

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-wagner.yml config --quiet
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-wagner.yml up -d --build
```

Se inician PostgreSQL mensajes y el servicio de mensajes. No se inicia RabbitMQ
en esta computadora: el servicio se conecta al RabbitMQ de Juan.

### Paso 4: comprobar el servicio

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-wagner.yml ps
curl http://localhost:8084/actuator/health
```

Esperar `{"status":"UP"}`. Si falla, consultar los logs:

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-wagner.yml logs --tail=100 servicio-mensajes
```

### Paso 5: comprobar el acceso desde Juan

Pedir a Juan que ejecute en su equipo:

```bash
curl http://192.168.1.11:8084/actuator/health
```

Si responde `UP`, el servicio es accesible desde la computadora del Gateway.
Cuando se envíen mensajes, este servicio publicará eventos en `chat.events`
con la routing key `chat.message.created`.

## 5. Josibel — PC 3

### Paso 1: revisar la configuración

Confirmar que `JWT_SECRET` sea igual al de Juan y que Juan tenga la IP real
de Josibel en `JOSIBEL_IP`. Este servicio no necesita conectarse a RabbitMQ.

### Paso 2: permitir conexiones al servicio de conversaciones

```bash
sudo ufw allow from 192.168.1.0/24 to any port 8082 proto tcp
```

### Paso 3: validar e iniciar

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-josibel.yml config --quiet
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-josibel.yml up -d --build
```

Se inician únicamente PostgreSQL conversaciones y el servicio de conversaciones.

### Paso 4: comprobar el servicio

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-josibel.yml ps
curl http://localhost:8082/actuator/health
```

Esperar `{"status":"UP"}`. Si falla:

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-josibel.yml logs --tail=100 servicio-conversaciones
```

### Paso 5: comprobar el acceso desde Juan

Pedir a Juan que ejecute:

```bash
curl http://192.168.1.12:8082/actuator/health
```

Cuando responda `UP`, el Gateway podrá acceder al servicio de conversaciones.

## 6. Emilio — PC 4

### Paso 1: confirmar el acceso a RabbitMQ de Juan

Revisar `JUAN_IP`, las credenciales RabbitMQ y el `JWT_SECRET` compartido.
Si tienen Netcat:

```bash
nc -vz 192.168.1.10 5672
```

### Paso 2: permitir conexiones al servicio de tiempo real

```bash
sudo ufw allow from 192.168.1.0/24 to any port 8085 proto tcp
```

El Gateway de Juan se conectará a este puerto por WebSocket.

### Paso 3: validar e iniciar

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-emilio.yml config --quiet
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-emilio.yml up -d --build
```

Solo se inicia el servicio de tiempo real. Emilio no necesita PostgreSQL ni
un RabbitMQ propio.

### Paso 4: comprobar el servicio

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-emilio.yml ps
curl http://localhost:8085/actuator/health
```

Esperar `{"status":"UP"}`. Si falla:

```bash
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-emilio.yml logs --tail=100 servicio-tiempo-real
```

El servicio declara y consume la cola `chat.message.created`, enlazada al exchange
`chat.events` con la routing key `chat.message.created` en RabbitMQ de Juan.

### Paso 5: comprobar el acceso desde Juan

Pedir a Juan que ejecute:

```bash
curl http://192.168.1.13:8085/actuator/health
```

El WebSocket remoto está en `ws://192.168.1.13:8085/ws/mensajes`. El frontend
no necesita conectarse directamente a Emilio: lo hace mediante el Gateway.
Una comprobación HTTP de salud no prueba por sí sola el flujo WebSocket.

## 7. Prueba final entre todos

### Paso 1: comprobar los cuatro servicios desde Juan

```bash
curl http://192.168.1.10:8080/actuator/health
curl http://192.168.1.11:8084/actuator/health
curl http://192.168.1.12:8082/actuator/health
curl http://192.168.1.13:8085/actuator/health
```

Todos deben responder `UP`. Usar las IP reales.

### Paso 2: abrir el frontend

Desde los navegadores de los integrantes, abrir:

```text
http://192.168.1.10:3000
```

Usar esa dirección también en Juan para mantener el mismo origen configurado.
No usar `localhost` desde los otros equipos: apuntaría a su propia computadora.

El frontend usa rutas del mismo origen. Su Nginx reenvía `/api` y `/ws/mensajes`
al Gateway por la red Docker de Juan. El recorrido real es:

```text
Navegador → frontend/Nginx de Juan → Gateway de Juan
                                      ├─ autenticación de Juan
                                      ├─ conversaciones de Josibel
                                      ├─ mensajes de Wagner → RabbitMQ de Juan
                                      └─ WebSocket de Emilio ← RabbitMQ de Juan
```

### Paso 3: probar el chat

1. Registrar dos usuarios mediante la interfaz.
2. Iniciar sesión con uno en un navegador y con el otro en otro equipo o sesión privada.
3. Crear o seleccionar una conversación con las opciones disponibles en la interfaz.
4. Enviar un mensaje y comprobar que el otro usuario lo recibe sin recargar.
5. Responder desde el segundo usuario.
6. Recargar y comprobar que los mensajes siguen disponibles.

Si los mensajes se guardan pero no llegan en vivo, revisar tiempo real, su conexión
a RabbitMQ y el WebSocket. En las herramientas de desarrollo del navegador, la
pestaña Red/Network permite inspeccionar la conexión WS; una negociación correcta
normalmente muestra el estado `101 Switching Protocols`.

## 8. Cómo detener y volver a iniciar

Cada persona debe usar **su propio archivo Compose**. Ejemplo para Juan:

```bash
# Detener y retirar contenedores sin borrar los volúmenes de datos.
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml down

# Volver a iniciar.
docker compose --env-file backend/infraestructura/.env -f backend/infraestructura/docker-compose-juan.yml up -d --build
```

Wagner, Josibel y Emilio deben sustituir `juan` por su nombre en el archivo.
**No usar `down -v`** a menos que quieran eliminar los datos almacenados.

Si cambian las IP o variables del `.env`, volver a ejecutar `up -d --build`
en los equipos afectados para aplicar la configuración. `restart` por sí solo
no aplica nuevas variables de entorno a los contenedores existentes.

## 9. Problemas frecuentes

| Problema | Qué revisar |
| --- | --- |
| `Connection refused` | El servicio no inició, el puerto es incorrecto o aún está arrancando. Revisar `ps` y `logs`. |
| La conexión queda esperando | IP incorrecta, firewall, aislamiento Wi-Fi o equipo desconectado. |
| `port is already allocated` | Otro contenedor o proceso ocupa el puerto. No ejecutar el despliegue completo ni Maven en paralelo. |
| Error de autenticación RabbitMQ | Usuario/contraseña distintos a los de Juan, o volumen inicializado con otras credenciales. |
| Error de autenticación PostgreSQL | Credenciales del servicio distintas de las guardadas en su base de datos. |
| Error JWT / `401` | Igualar `JWT_SECRET`, volver a iniciar los servicios afectados y volver a iniciar sesión. |
| Fallan conversaciones o mensajes desde la interfaz | Revisar IP remota en el `.env` de Juan y probar el healthcheck remoto desde Juan. |
| Chat sin actualización en vivo | Revisar RabbitMQ, logs de Emilio y negociación WebSocket del navegador. |
| Error de CORS | Abrir el frontend con `http://IP_DE_JUAN:3000`; comprobar `JUAN_IP` y recrear el Gateway. |

Cambiar las contraseñas en `.env` no cambia automáticamente las de bases de
datos o RabbitMQ ya inicializados. No borrar volúmenes para resolver esto sin
respaldar primero los datos y acordar si se pueden perder.

Los archivos por persona reutilizan partes de `docker-compose.yml` con `extends`:
conservar el archivo original junto a ellos. Esto no inicia sus demás servicios.
No ejecutar el Compose completo en las cuatro computadoras ni combinar los
cuatro archivos individuales en una sola ejecución.

Si antes usaban el despliegue completo, detenerlo con su comando original sin
`-v` antes de iniciar esta distribución. Los datos no se copian automáticamente
entre equipos: si necesitan conservarlos, realizar backup y restauración.
