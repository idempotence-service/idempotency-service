import axios from 'axios'

export const coreClient = axios.create({
  baseURL: '/core',
  headers: { Authorization: 'Bearer operator-token' },
})
export const senderClient = axios.create({ baseURL: '' })
export const receiverClient = axios.create({ baseURL: '' })
