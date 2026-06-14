import { coreClient } from './index.js'

export const coreApi = {
  getErrorEvents: (params) =>
    coreClient.get('/get-error-events', { params }),
  getDuplicateEvents: () =>
    coreClient.get('/get-duplicate-events'),
  getDuplicateCount: () =>
    coreClient.get('/get-duplicate-count'),
  getTimeoutCount: () =>
    coreClient.get('/get-timeout-count'),
  restartEvent: (globalKey) =>
    coreClient.post('/restart-event', { globalKey }),
  getEventById: (globalKey) =>
    coreClient.get('/get-event-by-id', { params: { globalKey } }),
  getConfig: () =>
    coreClient.get('/config'),
  updateScheduler: (data) =>
    coreClient.put('/config/scheduler', data),
  updateResilience: (data) =>
    coreClient.put('/config/resilience', data),
  updateCleanup: (data) =>
    coreClient.put('/config/cleanup', data),
  updateListener: (data) =>
    coreClient.put('/config/listener', data),
  getIntegrations: () =>
    coreClient.get('/config/integrations'),
  getEnabledIntegrations: () =>
    coreClient.get('/config/integrations/enabled'),
}
