import axios from 'axios'
import { useAuthStore } from '../stores/auth.js'

export const coreClient = axios.create({ baseURL: '/core' })
export const senderClient = axios.create({ baseURL: '' })
export const receiverClient = axios.create({ baseURL: '' })

coreClient.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})
