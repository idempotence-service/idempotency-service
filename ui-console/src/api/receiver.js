import { receiverClient } from './index.js'

export const receiverApi = {
  getReceivedEvents: (since) => receiverClient.get('/api/receiver/events', { params: since ? { since } : {} }),
  getStats: (since) => receiverClient.get('/api/receiver/stats', { params: since ? { since } : {} }),
  setMode: (data) => receiverClient.post('/api/receiver/mode', data),
  sendManualReply: (data) => receiverClient.post('/api/receiver/reply', data),
  resetState: () => receiverClient.delete('/api/receiver/state'),
}
