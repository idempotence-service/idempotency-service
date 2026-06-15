import express from 'express'
import axios from 'axios'

const app = express()
const PORT = process.env.PORT || 9090

// Sender instances (configurable via env)
const SENDER_INSTANCES = (process.env.SENDER_INSTANCES || 'sender-simulator:8081').split(',')
const RECEIVER_INSTANCES = (process.env.RECEIVER_INSTANCES || 'receiver-simulator:8082').split(',')

app.use(express.json())

// Health check
app.get('/health', (req, res) => res.json({ status: 'ok' }))

// Aggregate sender stats
app.get('/api/sender/stats', async (req, res) => {
  try {
    const results = await Promise.allSettled(
      SENDER_INSTANCES.map(host => axios.get(`http://${host}/api/sender/stats`, { params: req.query }))
    )
    
    const aggregated = {
      totalSent: 0,
      totalReplies: 0,
      sentHistorySize: 0,
      replyHistorySize: 0,
      historyEnabled: true,
      historyLimit: 1000
    }
    
    results.forEach(result => {
      if (result.status === 'fulfilled' && result.value.data?.success) {
        const data = result.value.data.data
        aggregated.totalSent += data.totalSent || 0
        aggregated.totalReplies += data.totalReplies || 0
        aggregated.sentHistorySize += data.sentHistorySize || 0
        aggregated.replyHistorySize += data.replyHistorySize || 0
      }
    })
    
    res.json({ success: true, data: aggregated })
  } catch (error) {
    res.status(500).json({ success: false, error: error.message })
  }
})

// Aggregate sent messages
app.get('/api/sender/sent', async (req, res) => {
  try {
    const results = await Promise.allSettled(
      SENDER_INSTANCES.map(host => axios.get(`http://${host}/api/sender/sent`, { params: req.query }))
    )
    
    const allMessages = []
    results.forEach(result => {
      if (result.status === 'fulfilled' && result.value.data?.success) {
        const data = result.value.data.data
        if (Array.isArray(data)) allMessages.push(...data)
        else if (data) allMessages.push(data)
      }
    })
    
    res.json({ success: true, data: allMessages })
  } catch (error) {
    res.status(500).json({ success: false, error: error.message })
  }
})

// Aggregate sender replies
app.get('/api/sender/replies', async (req, res) => {
  try {
    const results = await Promise.allSettled(
      SENDER_INSTANCES.map(host => axios.get(`http://${host}/api/sender/replies`))
    )
    
    const allReplies = []
    results.forEach(result => {
      if (result.status === 'fulfilled' && result.value.data?.success) {
        const data = result.value.data.data
        if (Array.isArray(data)) allReplies.push(...data)
        else if (data) allReplies.push(data)
      }
    })
    
    res.json({ success: true, data: allReplies })
  } catch (error) {
    res.status(500).json({ success: false, error: error.message })
  }
})

// Aggregate receiver stats
app.get('/api/receiver/stats', async (req, res) => {
  try {
    const results = await Promise.allSettled(
      RECEIVER_INSTANCES.map(host => axios.get(`http://${host}/api/receiver/stats`, { params: req.query }))
    )
    
    const aggregated = {
      totalReceived: 0,
      totalDuplicates: 0,
      messageHistorySize: 0,
      trackedKeyCount: 0,
      historyEnabled: true,
      historyLimit: 1000
    }
    
    results.forEach(result => {
      if (result.status === 'fulfilled' && result.value.data?.success) {
        const data = result.value.data.data
        aggregated.totalReceived += data.totalReceived || 0
        aggregated.totalDuplicates += data.totalDuplicates || 0
        aggregated.messageHistorySize += data.messageHistorySize || 0
        aggregated.trackedKeyCount += data.trackedKeyCount || 0
      }
    })
    
    res.json({ success: true, data: aggregated })
  } catch (error) {
    res.status(500).json({ success: false, error: error.message })
  }
})

// Aggregate receiver events
app.get('/api/receiver/events', async (req, res) => {
  try {
    const results = await Promise.allSettled(
      RECEIVER_INSTANCES.map(host => axios.get(`http://${host}/api/receiver/events`, { params: req.query }))
    )
    
    const allEvents = []
    results.forEach(result => {
      if (result.status === 'fulfilled' && result.value.data?.success) {
        const data = result.value.data.data
        if (Array.isArray(data)) allEvents.push(...data)
        else if (data) allEvents.push(data)
      }
    })
    
    res.json({ success: true, data: allEvents })
  } catch (error) {
    res.status(500).json({ success: false, error: error.message })
  }
})

// Forward sender config (read-only, first instance)
app.get('/api/sender/config', async (req, res) => {
  try {
    const response = await axios.get(`http://${SENDER_INSTANCES[0]}/api/sender/config`)
    res.json(response.data)
  } catch (error) {
    res.status(500).json({ success: false, error: error.message })
  }
})

// Forward sender config update (broadcast to all)
app.put('/api/sender/config', async (req, res) => {
  try {
    const results = await Promise.allSettled(
      SENDER_INSTANCES.map(host => axios.put(`http://${host}/api/sender/config`, req.body))
    )
    
    const failed = results.filter(r => r.status === 'rejected')
    if (failed.length === results.length) {
      res.status(500).json({ success: false, error: 'All instances failed' })
    } else {
      res.json({ success: true, data: 'ok' })
    }
  } catch (error) {
    res.status(500).json({ success: false, error: error.message })
  }
})

// Forward receiver mode (broadcast to all)
app.post('/api/receiver/mode', async (req, res) => {
  try {
    const results = await Promise.allSettled(
      RECEIVER_INSTANCES.map(host => axios.post(`http://${host}/api/receiver/mode`, req.body))
    )
    
    const failed = results.filter(r => r.status === 'rejected')
    if (failed.length === results.length) {
      res.status(500).json({ success: false, error: 'All instances failed' })
    } else {
      res.json({ success: true, data: 'ok' })
    }
  } catch (error) {
    res.status(500).json({ success: false, error: error.message })
  }
})

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Metrics aggregator listening on port ${PORT}`)
  console.log(`Sender instances: ${SENDER_INSTANCES.join(', ')}`)
  console.log(`Receiver instances: ${RECEIVER_INSTANCES.join(', ')}`)
})
