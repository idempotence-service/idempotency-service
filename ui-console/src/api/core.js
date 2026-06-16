import { coreClient } from './index.js'

export const coreApi = {
  getErrorEvents: (params) =>
    coreClient.get('/core/get-error-events', { params }),
  getDuplicateEvents: (page = 0, limit = 100) =>
    coreClient.get('/core/get-duplicate-events', { params: { page, limit } }),
  getDuplicateCount: () =>
    coreClient.get('/core/get-duplicate-count'),
  getTimeoutCount: () =>
    coreClient.get('/core/get-timeout-count'),
  getAuditActivity: (since) =>
    coreClient.get('/core/get-audit-activity', { params: { since } }),
  restartEvent: (globalKey) =>
    coreClient.post('/core/restart-event', { globalKey }),
  getEventById: (globalKey) =>
    coreClient.get('/core/get-event-by-id', { params: { globalKey } }),
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
