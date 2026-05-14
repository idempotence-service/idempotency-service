import { coreClient } from './index.js'

export const coreApi = {
  getErrorEvents: (params) =>
    coreClient.get('/get-error-events', { params }),
  getDuplicateEvents: () =>
    coreClient.get('/get-duplicate-events'),
  restartEvent: (globalKey) =>
    coreClient.post('/restart-event', { globalKey }),
  getEventById: (globalKey) =>
    coreClient.get('/get-event-by-id', { params: { globalKey } }),
}
