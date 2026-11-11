import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';

const apiBase = process.env.SYNCCHAT_API_BASE || 'http://localhost:8080/api';
const parsedBase = new URL(apiBase);
if (!['localhost', '127.0.0.1', '::1'].includes(parsedBase.hostname)) {
  throw new Error('La prueba solo se permite contra localhost; no apunta a laptops remotas.');
}
const root = `${parsedBase.protocol}//${parsedBase.host}`;
const websocketBase = process.env.SYNCCHAT_WS_BASE || root;
const originFrontend = process.env.SYNCCHAT_FRONTEND_ORIGIN || 'http://localhost:13000';
const sufijo = `${Date.now()}-${randomUUID().slice(0, 8)}`;
const cuentas = [];
const conversaciones = [];
const sockets = [];

async function solicitud(ruta, token, opciones = {}) {
  const respuesta = await fetch(`${apiBase}${ruta}`, {
    ...opciones,
    signal: AbortSignal.timeout(12_000),
    headers: {
      ...(opciones.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...opciones.headers,
    },
  });
  const texto = await respuesta.text();
  let body = null;
  try { body = texto ? JSON.parse(texto) : null; } catch { body = texto; }
  return { status: respuesta.status, body, headers: respuesta.headers };
}

function exigir(actual, esperado, operacion) {
  assert.equal(actual.status, esperado,
    `${operacion}: esperado HTTP ${esperado}, recibido ${actual.status}; cuerpo=${JSON.stringify(actual.body)}`);
  return actual.body;
}

async function registrar(nombre) {
  const correo = `fase8-${nombre.toLowerCase()}-${sufijo}@example.com`;
  cuentas.push(correo);
  return exigir(await solicitud('/auth/register', null, {
    method: 'POST', body: JSON.stringify({ nombre, correo, password: 'FaseOcho-2026!' }),
  }), 200, `registro ${nombre}`);
}

async function crearConversacion(token, nombre) {
  const cuerpo = exigir(await solicitud('/conversaciones', token, {
    method: 'POST', body: JSON.stringify({ nombre }),
  }), 201, `crear conversación ${nombre}`);
  conversaciones.push(cuerpo.id);
  return cuerpo;
}

async function invitar(token, conversacionId, correo) {
  return solicitud(`/conversaciones/${conversacionId}/invitaciones`, token, {
    method: 'POST', body: JSON.stringify({ correo }),
  });
}

async function aceptar(token, conversacionId) {
  return solicitud(`/conversaciones/invitaciones/${conversacionId}/aceptar`, token, { method: 'POST' });
}

function conectar(token) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(`${websocketBase.replace(/^http/, 'ws')}/ws/mensajes?token=${encodeURIComponent(token)}`);
    sockets.push(socket);
    const errorTimer = setTimeout(() => reject(new Error('Timeout al abrir WebSocket')), 8000);
    socket.addEventListener('open', () => { clearTimeout(errorTimer); resolve(socket); }, { once: true });
    socket.addEventListener('error', () => { clearTimeout(errorTimer); reject(new Error('Falló la conexión WebSocket')); }, { once: true });
  });
}

function recolectar(socket) {
  const eventos = [];
  socket.addEventListener('message', (evento) => {
    try { eventos.push(JSON.parse(evento.data)); } catch { /* Ignora tramas no JSON. */ }
  });
  return eventos;
}

async function esperarEvento(eventos, id, etiqueta) {
  const fin = Date.now() + 8000;
  while (Date.now() < fin) {
    const encontrado = eventos.find((evento) => evento.id === id);
    if (encontrado) return encontrado;
    await new Promise((resolver) => setTimeout(resolver, 40));
  }
  throw new Error(`No llegó por WebSocket el mensaje ${id} a ${etiqueta}`);
}

async function esperarEscritura(eventos, conversacionId, usuarioId) {
  const fin = Date.now() + 8000;
  while (Date.now() < fin) {
    const evento = eventos.find((item) => item.tipo === 'typing'
      && item.conversacionId === conversacionId && item.usuarioId === usuarioId && item.escribiendo);
    if (evento) return evento;
    await new Promise((resolver) => setTimeout(resolver, 40));
  }
  throw new Error('No se recibió el indicador de escritura autorizado por WebSocket');
}

function enviar(token, conversacionId, contenido, idempotencyKey, camposExtra = {}) {
  return solicitud('/mensajes', token, {
    method: 'POST',
    body: JSON.stringify({ conversacionId, contenido, idempotencyKey, ...camposExtra }),
  });
}

function limpiar() {
  const ids = conversaciones.filter((id) => /^[0-9a-f-]{36}$/i.test(id));
  if (ids.length) {
    const lista = ids.map((id) => `'${id}'`).join(',');
    execFileSync('docker', ['exec', 'syncchat-postgres-mensajes', 'psql', '-U', 'syncchat', '-d', 'mensajes',
      '-v', 'ON_ERROR_STOP=1', '-c', `DELETE FROM mensajes WHERE conversacion_id IN (${lista})`], { stdio: 'ignore' });
    execFileSync('docker', ['exec', 'syncchat-postgres-conversaciones', 'psql', '-U', 'syncchat', '-d', 'conversaciones',
      '-v', 'ON_ERROR_STOP=1', '-c', `DELETE FROM conversaciones WHERE id IN (${lista})`], { stdio: 'ignore' });
  }
  if (cuentas.length) {
    const lista = cuentas.map((correo) => `'${correo}'`).join(',');
    execFileSync('docker', ['exec', 'syncchat-postgres-usuarios', 'psql', '-U', 'syncchat', '-d', 'usuarios',
      '-v', 'ON_ERROR_STOP=1', '-c', `DELETE FROM usuarios WHERE correo IN (${lista})`], { stdio: 'ignore' });
  }
}

try {
  const sinToken = await solicitud('/conversaciones');
  exigir(sinToken, 401, 'JWT requerido por Gateway');
  const cors = await fetch(`${apiBase}/conversaciones`, {
    method: 'OPTIONS',
    headers: { Origin: originFrontend, 'Access-Control-Request-Method': 'GET' },
  });
  assert.equal(cors.headers.get('access-control-allow-origin'), originFrontend, 'CORS debe autorizar el frontend configurado');

  const owner = await registrar('Owner');
  const alice = await registrar('Alice');
  const bob = await registrar('Bob');
  const charlie = await registrar('Charlie');
  const intruso = await registrar('Intruso');

  // Chat de dos usuarios: pending no puede abrir, aceptar concede acceso e historial.
  const directo = await crearConversacion(owner.token, 'Prueba local de dos usuarios');
  exigir(await invitar(owner.token, directo.id, alice.usuario.correo), 201, 'invitar en chat de dos personas');
  const pendientesAlice = exigir(await solicitud('/conversaciones/invitaciones/pendientes', alice.token), 200, 'pendientes de Alice');
  assert(pendientesAlice.some((item) => item.conversacionId === directo.id));
  exigir(await solicitud(`/conversaciones/${directo.id}`, alice.token), 403, 'pendiente no abre conversación');
  exigir(await enviar(alice.token, directo.id, 'bloqueado', randomUUID()), 403, 'pendiente no envía mensajes');
  exigir(await aceptar(alice.token, directo.id), 204, 'aceptar invitación de chat directo');
  exigir(await invitar(alice.token, directo.id, bob.usuario.correo), 403, 'miembro no administrador no invita');

  const wsOwner = await conectar(owner.token);
  const wsAlice = await conectar(alice.token);
  const wsAjeno = await conectar(intruso.token);
  await new Promise((resolver) => setTimeout(resolver, 150));
  const ownerDirectEvents = recolectar(wsOwner);
  const aliceDirectEvents = recolectar(wsAlice);
  const outsiderEvents = recolectar(wsAjeno);
  wsOwner.send(JSON.stringify({ tipo: 'typing', conversacionId: directo.id, escribiendo: true }));
  await esperarEscritura(aliceDirectEvents, directo.id, owner.usuario.id);
  assert.equal(outsiderEvents.filter((evento) => evento.tipo === 'typing').length, 0,
    'los indicadores de escritura no se envían a usuarios ajenos');
  const claveDirecta = randomUUID();
  const textoEmoji = 'Hola 👋 😀 ❤️ 👍🏽 👨‍👩‍👧‍👦';
  const msgDirecto = exigir(await enviar(owner.token, directo.id, textoEmoji, claveDirecta,
    { remitenteId: intruso.usuario.id }), 201, 'envío del chat directo');
  assert.equal(msgDirecto.remitenteId, owner.usuario.id, 'el remitente debe salir del JWT, no del JSON');
  assert.equal(msgDirecto.remitenteNombre, owner.usuario.nombre, 'la respuesta incluye el nombre registrado del remitente');
  assert.equal(msgDirecto.contenido, textoEmoji, 'el backend conserva emojis y secuencias Unicode compuestas');
  await Promise.all([
    esperarEvento(ownerDirectEvents, msgDirecto.id, 'owner'),
    esperarEvento(aliceDirectEvents, msgDirecto.id, 'Alice'),
  ]);
  assert.equal(ownerDirectEvents.find((evento) => evento.id === msgDirecto.id)?.remitenteNombre,
    owner.usuario.nombre, 'WebSocket incluye el nombre registrado del remitente');
  assert.equal(ownerDirectEvents.find((evento) => evento.id === msgDirecto.id)?.contenido, textoEmoji,
    'WebSocket entrega emojis y secuencias Unicode sin alterarlos');
  assert.equal(aliceDirectEvents.find((evento) => evento.id === msgDirecto.id)?.contenido, textoEmoji,
    'los demás participantes reciben el texto Unicode completo');
  const repetido = exigir(await enviar(owner.token, directo.id, textoEmoji, claveDirecta), 201,
    'reintento idempotente');
  assert.equal(repetido.id, msgDirecto.id, 'el reintento debe devolver el mismo mensaje persistido');
  await new Promise((resolver) => setTimeout(resolver, 900));
  assert.equal(ownerDirectEvents.filter((evento) => evento.id === msgDirecto.id).length, 1, 'sin duplicado para owner');
  assert.equal(aliceDirectEvents.filter((evento) => evento.id === msgDirecto.id).length, 1, 'sin duplicado para Alice');
  assert.equal(outsiderEvents.filter((evento) => evento.id === msgDirecto.id).length, 0, 'ajeno no recibe el evento');
  exigir(await solicitud(`/mensajes/conversacion/${directo.id}`, intruso.token), 403, 'ajeno no lee historial');
  exigir(await enviar(intruso.token, directo.id, 'intrusión', randomUUID()), 403, 'ajeno no envía mensajes');

  // Grupo de cuatro personas: roster privado, tres WebSockets autorizados y un miembro offline.
  const grupo = await crearConversacion(owner.token, 'Prueba local de grupo');
  for (const cuenta of [alice, bob, charlie]) {
    exigir(await invitar(owner.token, grupo.id, cuenta.usuario.correo), 201, `invitar ${cuenta.usuario.nombre} al grupo`);
  }
  const rosterPendiente = exigir(await solicitud(`/conversaciones/${grupo.id}/participantes`, owner.token), 200,
    'listar roster con estados');
  assert.equal(rosterPendiente.filter((p) => p.estado === 'PENDING').length, 3);
  assert(rosterPendiente.every((p) => !('id' in p) && !('usuarioId' in p) && !('correo' in p)),
    'el roster no expone UUID ni correo');
  for (const cuenta of [alice, bob, charlie]) exigir(await aceptar(cuenta.token, grupo.id), 204, `aceptación de ${cuenta.usuario.nombre}`);
  const rosterAceptado = exigir(await solicitud(`/conversaciones/${grupo.id}/participantes`, owner.token), 200,
    'listar roster aceptado');
  assert.equal(rosterAceptado.filter((p) => p.estado === 'ACCEPTED').length, 4);

  const wsBob = await conectar(bob.token);
  await new Promise((resolver) => setTimeout(resolver, 150));
  const groupSockets = [wsOwner, wsAlice, wsBob];
  const groupEvents = groupSockets.map(recolectar);
  const claveGrupo = randomUUID();
  const msgGrupo = exigir(await enviar(alice.token, grupo.id, 'Mensaje para todos los miembros aceptados', claveGrupo),
    201, 'mensaje de grupo');
  assert.equal(msgGrupo.remitenteNombre, alice.usuario.nombre, 'el grupo conserva el nombre real de quien envía');
  await Promise.all(groupEvents.map((eventos, indice) => esperarEvento(eventos, msgGrupo.id, `miembro ${indice + 1}`)));
  await new Promise((resolver) => setTimeout(resolver, 500));
  assert.equal(outsiderEvents.filter((evento) => evento.id === msgGrupo.id).length, 0, 'ajeno no recibe mensajes de grupo');
  const historialOffline = exigir(await solicitud(`/mensajes/conversacion/${grupo.id}`, charlie.token), 200,
    'miembro desconectado recupera historial');
  assert(historialOffline.some((mensaje) => mensaje.id === msgGrupo.id));
  assert.equal(historialOffline.find((mensaje) => mensaje.id === msgGrupo.id)?.remitenteNombre,
    alice.usuario.nombre, 'el historial conserva el nombre registrado del remitente');

  const directoDetalle = exigir(await solicitud(`/conversaciones/${directo.id}`, owner.token), 200, 'reabrir chat directo');
  assert.equal(directoDetalle.id, directo.id);
  console.log('Prueba local completa: dos usuarios, grupo, permisos, privacidad, CORS, JWT, WebSocket, idempotencia e historial: OK.');
} finally {
  for (const socket of sockets) {
    try { socket.close(); } catch { /* puede haberse cerrado durante la prueba */ }
  }
  try {
    limpiar();
    console.log('Datos temporales de prueba eliminados.');
  } catch (error) {
    console.error('No se pudieron limpiar todas las filas temporales; revisar y borrar solo los correos fase8-* y conversaciones creadas por esta ejecución.', error.message);
    process.exitCode = 1;
  }
}
