export function truncate(s, n) {
  if (!s) return '—'
  return s.length > n ? s.slice(0, 8) + '…' + s.slice(-8) : s
}

export function getAdaptiveTimeRange(startTime, endTime) {
  const diffMs = endTime.getTime() - startTime.getTime()
  const diffHours = diffMs / (1000 * 60 * 60)
  const diffDays = diffHours / 24
  
  if (diffHours <= 24) {
    return { interval: 60 * 60 * 1000, format: (d) => d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' }) }
  } else if (diffDays <= 7) {
    return { interval: 24 * 60 * 60 * 1000, format: (d) => d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' }) }
  } else if (diffDays <= 30) {
    return { interval: 7 * 24 * 60 * 60 * 1000, format: (d) => d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' }) }
  } else {
    return { interval: 30 * 24 * 60 * 60 * 1000, format: (d) => d.toLocaleDateString('ru-RU', { month: 'short', year: '2-digit' }) }
  }
}

export function getAdaptiveTimeRangeForData(minTime, maxTime, targetSlots = 12) {
  const diffMs = maxTime - minTime
  const targetInterval = diffMs / targetSlots
  
  // Round to nice intervals (1 min, 5 min, 15 min, 1 hour, 6 hours, 1 day, etc.)
  const intervals = [
    60 * 1000,              // 1 minute
    5 * 60 * 1000,          // 5 minutes
    15 * 60 * 1000,         // 15 minutes
    60 * 60 * 1000,         // 1 hour
    6 * 60 * 60 * 1000,     // 6 hours
    24 * 60 * 60 * 1000,    // 1 day
    7 * 24 * 60 * 60 * 1000 // 1 week
  ]
  
  let interval = intervals[intervals.length - 1]
  for (const i of intervals) {
    if (i >= targetInterval) {
      interval = i
      break
    }
  }
  
  // Determine format based on interval
  let format
  if (interval < 60 * 60 * 1000) {
    format = (d) => d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
  } else if (interval < 24 * 60 * 60 * 1000) {
    format = (d) => d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
  } else if (interval < 7 * 24 * 60 * 60 * 1000) {
    format = (d) => d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })
  } else {
    format = (d) => d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })
  }
  
  return { interval, format }
}

export function calculateSuccessRate(totalMessages, totalErrors, timeoutCount) {
  if (totalMessages === 0) return 0
  const successfulMessages = totalMessages - (totalErrors + timeoutCount)
  return (successfulMessages / totalMessages) * 100
}

export function formatSuccessRate(totalMessages, totalErrors, timeoutCount) {
  if (totalMessages === 0) return '—'
  const rate = calculateSuccessRate(totalMessages, totalErrors, timeoutCount)
  return `${rate.toFixed(1)}%`
}

export function calculateThroughput(sentCount, timeRange) {
  const timeWindowSeconds = {
    'minute': 60,
    'hour': 3600,
    'day': 86400,
    'all': null
  }
  const seconds = timeWindowSeconds[timeRange]
  
  if (!seconds || sentCount === 0) {
    return sentCount
  }
  
  const messagesPerSecond = sentCount / seconds
  if (messagesPerSecond < 1) {
    return messagesPerSecond.toFixed(2)
  }
  return Math.round(messagesPerSecond)
}

export function getThroughputLabel(timeRange) {
  const timeWindowSeconds = {
    'minute': 60,
    'hour': 3600,
    'day': 86400,
    'all': null
  }
  const seconds = timeWindowSeconds[timeRange]
  
  if (!seconds) {
    return 'Всего сообщений'
  }
  return 'сообщений/сек'
}

export function calculateSystemHealth(totalMessages, totalErrors, timeoutCount) {
  const errorRate = totalMessages > 0 
    ? ((totalErrors + timeoutCount) / totalMessages) * 100 
    : 0
  
  if (errorRate === 0 && totalMessages > 0) {
    return {
      status: 'healthy',
      label: 'Отлично',
      message: 'Все системы работают нормально'
    }
  } else if (errorRate < 5) {
    return {
      status: 'healthy',
      label: 'Здорова',
      message: `Уровень ошибок: ${errorRate.toFixed(1)}% (в пределах нормы)`
    }
  } else if (errorRate < 15) {
    return {
      status: 'degraded',
      label: 'Деградация',
      message: `Уровень ошибок: ${errorRate.toFixed(1)}% (требует внимания)`
    }
  } else {
    return {
      status: 'critical',
      label: 'Критично',
      message: `Уровень ошибок: ${errorRate.toFixed(1)}% (требуется вмешательство)`
    }
  }
}

export function buildActivityChartData(sentMessages, receivedMessages, auditActivity, activityTimeRange, getAdaptiveTimeRange) {
  const labels = []
  const sentData = []
  const receivedData = []

  const now = new Date()
  let slots, intervalMs, formatLabel

  if (activityTimeRange === 'all') {
    const totalSlots = auditActivity.length
    if (totalSlots === 0) {
      return { labels: [], datasets: [] }
    }
    
    const oldestSlotTime = new Date(now.getTime() - (totalSlots - 1) * 5 * 60000)
    const timeRange = getAdaptiveTimeRange(oldestSlotTime, now)
    
    slots = Math.min(totalSlots, 48)
    intervalMs = timeRange.interval
    formatLabel = timeRange.format
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(now.getTime() - (slots - 1 - i) * intervalMs)
      labels.push(formatLabel(slotTime))
      const slotIndex = i
      const slotActivity = auditActivity[slotIndex] || {}
      sentData.push(slotActivity['Отправлено'] || 0)
      receivedData.push(slotActivity['Получено'] || 0)
    }
  } else if (activityTimeRange === 'minute') {
    slots = 6
    intervalMs = 10 * 1000
    const minuteStart = new Date(now)
    minuteStart.setSeconds(0, 0)
    formatLabel = (d, i) => `${i * 10}с`
    
    for (let i = 0; i < slots; i++) {
      const slotStart = new Date(minuteStart.getTime() + i * intervalMs)
      const slotEnd = new Date(minuteStart.getTime() + (i + 1) * intervalMs)
      labels.push(formatLabel(slotEnd, i))

      const sentInSlot = sentMessages.filter(m => {
        if (!m.timestamp) return false
        const msgTime = new Date(m.timestamp)
        return msgTime >= slotStart && msgTime < slotEnd
      }).length
      sentData.push(sentInSlot)

      const receivedInSlot = receivedMessages.filter(m => {
        if (!m.timestamp) return false
        const msgTime = new Date(m.timestamp)
        return msgTime >= slotStart && msgTime < slotEnd
      }).length
      receivedData.push(receivedInSlot)
    }
  } else if (activityTimeRange === 'hour') {
    slots = 12
    intervalMs = 5 * 60000
    formatLabel = (d) => `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
    
    for (let i = 0; i < slots; i++) {
      const slotStart = new Date(now.getTime() - (slots - i) * intervalMs)
      const slotEnd = new Date(now.getTime() - (slots - i - 1) * intervalMs)
      labels.push(formatLabel(slotEnd))

      const sentInSlot = sentMessages.filter(m => {
        if (!m.timestamp) return false
        const msgTime = new Date(m.timestamp)
        return msgTime >= slotStart && msgTime < slotEnd
      }).length
      sentData.push(sentInSlot)

      const receivedInSlot = receivedMessages.filter(m => {
        if (!m.timestamp) return false
        const msgTime = new Date(m.timestamp)
        return msgTime >= slotStart && msgTime < slotEnd
      }).length
      receivedData.push(receivedInSlot)
    }
  } else {
    slots = 24
    intervalMs = 60 * 60000
    formatLabel = (d) => `${d.getHours().toString().padStart(2, '0')}:00`
    
    const startOfDay = new Date(now)
    startOfDay.setHours(0, 0, 0, 0)
    
    for (let i = 0; i < slots; i++) {
      const slotStart = new Date(startOfDay.getTime() + i * intervalMs)
      const slotEnd = new Date(startOfDay.getTime() + (i + 1) * intervalMs)
      labels.push(formatLabel(slotStart))

      const sentInSlot = sentMessages.filter(m => {
        if (!m.timestamp) return false
        const msgTime = new Date(m.timestamp)
        return msgTime >= slotStart && msgTime < slotEnd
      }).length
      sentData.push(sentInSlot)

      const receivedInSlot = receivedMessages.filter(m => {
        if (!m.timestamp) return false
        const msgTime = new Date(m.timestamp)
        return msgTime >= slotStart && msgTime < slotEnd
      }).length
      receivedData.push(receivedInSlot)
    }
  }

  return {
    labels,
    datasets: [
      {
        label: 'Отправлено',
        data: sentData,
        borderColor: '#82b1ff',
        backgroundColor: 'rgba(130,177,255,0.1)',
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Получено',
        data: receivedData,
        borderColor: '#6dd58c',
        backgroundColor: 'rgba(109,213,140,0.1)',
        tension: 0.4,
        fill: true,
      },
    ],
  }
}

export function buildDuplicatesChartData(auditActivity, activityTimeRange, getAdaptiveTimeRange) {
  const labels = []
  const duplicateData = []

  const now = new Date()
  let slots, intervalMs, formatLabel

  if (activityTimeRange === 'all') {
    const totalSlots = auditActivity.length
    if (totalSlots === 0) {
      return { labels: [], datasets: [] }
    }
    
    const oldestSlotTime = new Date(now.getTime() - (totalSlots - 1) * 5 * 60000)
    const timeRange = getAdaptiveTimeRange(oldestSlotTime, now)
    
    slots = Math.min(totalSlots, 48)
    intervalMs = timeRange.interval
    formatLabel = timeRange.format
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(now.getTime() - (slots - 1 - i) * intervalMs)
      labels.push(formatLabel(slotTime))
      const slotIndex = i
      const slotActivity = auditActivity[slotIndex] || {}
      duplicateData.push(slotActivity['Событие не прошло проверку на идемпотентность'] || 0)
    }
  } else if (activityTimeRange === 'minute') {
    slots = 6
    intervalMs = 10 * 1000
    const minuteStart = new Date(now)
    minuteStart.setSeconds(0, 0)
    formatLabel = (d, i) => `${i * 10}с`
    
    for (let i = 0; i < slots; i++) {
      labels.push(formatLabel(now, i))
      const slotActivity = auditActivity[i] || {}
      duplicateData.push(slotActivity['Событие не прошло проверку на идемпотентность'] || 0)
    }
  } else if (activityTimeRange === 'hour') {
    slots = 12
    intervalMs = 5 * 60000
    formatLabel = (d) => `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(now.getTime() - (slots - 1 - i) * intervalMs)
      labels.push(formatLabel(slotTime))

      const slotIndex = i
      const slotActivity = auditActivity[slotIndex] || {}
      duplicateData.push(slotActivity['Событие не прошло проверку на идемпотентность'] || 0)
    }
  } else {
    slots = 24
    intervalMs = 60 * 60000
    formatLabel = (d) => `${d.getHours().toString().padStart(2, '0')}:00`
    
    const startOfDay = new Date(now)
    startOfDay.setHours(0, 0, 0, 0)
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(startOfDay.getTime() + i * intervalMs)
      labels.push(formatLabel(slotTime))

      const slotActivity = auditActivity[i] || {}
      duplicateData.push(slotActivity['Событие не прошло проверку на идемпотентность'] || 0)
    }
  }

  return {
    labels,
    datasets: [
      {
        label: 'Дубли',
        data: duplicateData,
        borderColor: '#f6c142',
        backgroundColor: 'rgba(246,193,66,0.1)',
        tension: 0.4,
        fill: true,
      },
    ],
  }
}

export function buildErrorsChartData(auditActivity, activityTimeRange, getAdaptiveTimeRange, getAdaptiveTimeRangeForData) {
  const labels = []
  const timeoutData = []
  const errorData = []

  const now = new Date()
  let slots, intervalMs, formatLabel

  if (activityTimeRange === 'all') {
    const totalSlots = auditActivity.length
    if (totalSlots === 0) {
      return { labels: [], datasets: [] }
    }
    
    const oldestSlotTime = new Date(now.getTime() - (totalSlots - 1) * 5 * 60000)
    const timeRange = getAdaptiveTimeRangeForData(oldestSlotTime.getTime(), now.getTime(), totalSlots)
    
    slots = Math.min(totalSlots, 48)
    intervalMs = timeRange.interval
    formatLabel = timeRange.format
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(now.getTime() - (slots - 1 - i) * intervalMs)
      labels.push(formatLabel(slotTime))
      const slotIndex = i
      const slotActivity = auditActivity[slotIndex] || {}

      timeoutData.push(slotActivity['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
      errorData.push(
        (slotActivity['Некорректное входящее событие'] || 0) +
        (slotActivity['Не найден маршрут для входящего события'] || 0) +
        (slotActivity['Некорректный ответ от системы-получателя'] || 0) +
        (slotActivity['Получен ответ без ожидающей операции'] || 0)
      )
    }
  } else if (activityTimeRange === 'minute') {
    slots = 6
    intervalMs = 10 * 1000
    const minuteStart = new Date(now)
    minuteStart.setSeconds(0, 0)
    formatLabel = (d, i) => `${i * 10}с`
    
    for (let i = 0; i < slots; i++) {
      labels.push(formatLabel(now, i))
      const slotActivity = auditActivity[i] || {}

      timeoutData.push(slotActivity['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
      errorData.push(
        (slotActivity['Некорректное входящее событие'] || 0) +
        (slotActivity['Не найден маршрут для входящего события'] || 0) +
        (slotActivity['Некорректный ответ от системы-получателя'] || 0) +
        (slotActivity['Получен ответ без ожидающей операции'] || 0)
      )
    }
  } else if (activityTimeRange === 'hour') {
    slots = 12
    intervalMs = 5 * 60000
    formatLabel = (d) => `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(now.getTime() - (slots - 1 - i) * intervalMs)
      labels.push(formatLabel(slotTime))

      const slotIndex = i
      const slotActivity = auditActivity[slotIndex] || {}

      timeoutData.push(slotActivity['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
      errorData.push(
        (slotActivity['Некорректное входящее событие'] || 0) +
        (slotActivity['Не найден маршрут для входящего события'] || 0) +
        (slotActivity['Некорректный ответ от системы-получателя'] || 0) +
        (slotActivity['Получен ответ без ожидающей операции'] || 0)
      )
    }
  } else {
    slots = 24
    intervalMs = 60 * 60000
    formatLabel = (d) => `${d.getHours().toString().padStart(2, '0')}:00`
    
    const startOfDay = new Date(now)
    startOfDay.setHours(0, 0, 0, 0)
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(startOfDay.getTime() + i * intervalMs)
      labels.push(formatLabel(slotTime))

      const slotActivity = auditActivity[i] || {}

      timeoutData.push(slotActivity['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
      errorData.push(
        (slotActivity['Некорректное входящее событие'] || 0) +
        (slotActivity['Не найден маршрут для входящего события'] || 0) +
        (slotActivity['Некорректный ответ от системы-получателя'] || 0) +
        (slotActivity['Получен ответ без ожидающей операции'] || 0)
      )
    }
  }

  return {
    labels,
    datasets: [
      {
        label: 'Таймауты',
        data: timeoutData,
        borderColor: '#f2b8b5',
        backgroundColor: 'rgba(242,184,181,0.1)',
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Ошибки',
        data: errorData,
        borderColor: '#82b1ff',
        backgroundColor: 'rgba(130,177,255,0.1)',
        tension: 0.4,
        fill: true,
      },
    ],
  }
}

export function calculateSinceTimestamp(activityTimeRange) {
  const now = new Date()
  if (activityTimeRange === 'minute') {
    return new Date(now.getTime() - 60 * 1000).toISOString()
  } else if (activityTimeRange === 'hour') {
    return new Date(now.getTime() - 60 * 60 * 1000).toISOString()
  } else if (activityTimeRange === 'day') {
    const startOfDay = new Date(now)
    startOfDay.setHours(0, 0, 0, 0)
    return startOfDay.toISOString()
  } else { // all - no time filter
    return undefined
  }
}

export function toggleIntegration(expandedIntegrations, name) {
  const s = new Set(expandedIntegrations)
  if (s.has(name)) { s.delete(name) } else { s.add(name) }
  return s
}

export function channelEntries(intg) {
  return [
    ['Inbound', intg.inbound],
    ['Request Out', intg.requestOut],
    ['Reply In', intg.replyIn],
    ['Reply Out', intg.replyOut],
  ].filter(([, ch]) => ch != null)
}
