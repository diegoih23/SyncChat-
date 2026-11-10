import { solicitar } from '../../../servicios/api.js';

export const listarConversaciones = () => solicitar('/conversaciones');
