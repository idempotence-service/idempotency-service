import { senderClient } from './index.js'

export const senderApi = {
  sendEvent: (data) => senderClient.post('/api/sender/send', data),
  getSentMessages: (since) => senderClient.get('/api/sender/sent', { params: since ? { since } : {} }),
  getReplies: () => senderClient.get('/api/sender/replies'),
  getStats: (since) => senderClient.get('/api/sender/stats', { params: since ? { since } : {} }),
  resetState: () => senderClient.delete('/api/sender/state'),
  getSimulationConfig: () => senderClient.get('/api/sender/config'),
  updateSimulationConfig: (data) => senderClient.put('/api/sender/config', data),
}
