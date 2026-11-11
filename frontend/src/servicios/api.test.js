import assert from 'node:assert/strict';
import test from 'node:test';
import { solicitar } from './api.js';

globalThis.localStorage = { getItem: () => null };
globalThis.sessionStorage = { getItem: () => null };

test('acepta una respuesta 201 sin cuerpo para invitaciones', async () => {
  globalThis.fetch = async () => new Response(null, { status: 201 });
  assert.equal(await solicitar('/conversaciones/123/invitaciones', { method: 'POST' }), null);
});

test('acepta respuestas 204 sin cuerpo', async () => {
  globalThis.fetch = async () => new Response(null, { status: 204 });
  assert.equal(await solicitar('/conversaciones/invitaciones/123/aceptar', { method: 'POST' }), null);
});

test('interpreta respuestas JSON normales', async () => {
  globalThis.fetch = async () => Response.json({ registrado: true }, { status: 200 });
  assert.deepEqual(await solicitar('/auth/check-email'), { registrado: true });
});
