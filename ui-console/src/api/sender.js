import { senderClient } from './index.js'

export const senderApi = {
  sendEvent: (data) => senderClient.post('/api/sender/send', data),
  getSentMessages: () => senderClient.get('/api/sender/sent'),
  getReplies: () => senderClient.get('/api/sender/replies'),
  getStats: () => senderClient.get('/api/sender/stats'),
  resetState: () => senderClient.delete('/api/sender/state'),
  getSimulationConfig: () => senderClient.get('/api/sender/config'),
  updateSimulationConfig: (data) => senderClient.put('/api/sender/config', data),
}
