import { senderClient } from './index.js'

export const senderApi = {
  sendEvent: (data) => senderClient.post('/api/sender/send', data),
  getSentMessages: () => senderClient.get('/api/sender/sent'),
  getReplies: () => senderClient.get('/api/sender/replies'),
  resetState: () => senderClient.delete('/api/sender/state'),
}
