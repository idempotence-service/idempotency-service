import { receiverClient } from './index.js'

export const receiverApi = {
  getReceivedEvents: () => receiverClient.get('/api/receiver/events'),
  setMode: (data) => receiverClient.post('/api/receiver/mode', data),
  sendManualReply: (data) => receiverClient.post('/api/receiver/reply', data),
  resetState: () => receiverClient.delete('/api/receiver/state'),
}
