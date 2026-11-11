# Contrato del dominio: conversaciones y participantes

Este documento define las reglas de dominio de la primera etapa. Los UUID son
identificadores internos; la interfaz no debe pedirlos ni mostrarlos como nombres
de personas. La búsqueda de contactos y las pantallas de invitaciones se implementan
en etapas posteriores.

## Datos

- Una conversación tiene uno o más registros en `conversacion_participantes`.
- El creador se registra `ACCEPTED` y `administrador = true`.
- Cada invitado se registra `PENDING`, sin privilegios administrativos.
- La pareja `(conversacion_id, usuario_id)` es única.
- `creado_por` permanece como dato de auditoría/compatibilidad; la autorización
  se decide exclusivamente con el registro de participante y su estado/rol.
- `destinatario_id` se conserva temporalmente para compatibilidad y backfill. No
  es la fuente autoritativa para listar o autorizar conversaciones. El flujo actual
  de mensajes aún lo consume y debe migrarse a participantes aceptados antes de
  retirar la columna o dar por terminada la transición.

## Transiciones permitidas

| Estado actual | Estado siguiente | Actor/regla |
| --- | --- | --- |
| `PENDING` | `ACCEPTED` | El invitado acepta |
| `PENDING` | `DECLINED` | El invitado rechaza |
| `PENDING` | `REMOVED` | Un administrador cancela la invitación |
| `ACCEPTED` | `LEFT` | El participante sale; el último administrador no puede salir |
| `ACCEPTED` | `REMOVED` | Un administrador retira al participante; no se permite retirar al último administrador |
| `DECLINED`, `LEFT`, `REMOVED` | `PENDING` | Un administrador reinvita; se quitan privilegios previos |

Las demás transiciones se rechazan. Una invitación `PENDING` o una membresía
`ACCEPTED` no se duplica. Un usuario `PENDING`, `DECLINED`, `LEFT` o `REMOVED`
no puede acceder a la conversación ni a sus mensajes.

## Administración y ciclo de vida

- El creador es el primer administrador.
- Solo un administrador `ACCEPTED` puede invitar, cancelar invitaciones, retirar
  participantes o eliminar la conversación.
- Solo el usuario invitado puede aceptar o rechazar su invitación.
- Un participante `ACCEPTED` puede salir, con la restricción del último administrador.
- La transferencia/promoción de administradores no está disponible todavía; por
  eso se impide dejar una conversación sin administrador activo.
- Los estados terminales se conservan para auditoría; reinvitar reactiva el mismo
  registro como `PENDING` en vez de crear duplicados.

## Límite de esta etapa

Las reglas anteriores ya están representadas en el modelo y en el servicio. Los
endpoints, la consulta de invitaciones pendientes, la búsqueda por correo/nombre,
y la autorización de historial/mensajes/WebSocket deben completarse antes de usar
este contrato en un despliegue de producción.
