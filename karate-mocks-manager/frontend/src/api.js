import axios from 'axios'

const api = axios.create({
  baseURL: '/api'
})

export const serverApi = {
  getAll: () => api.get('/servers'),
  get: (id) => api.get(`/servers/${id}`),
  start: (config) => api.post('/servers', config),
  stop: (id) => api.delete(`/servers/${id}`),
  getRequests: (id, limit = 100) => api.get(`/servers/${id}/requests?limit=${limit}`),
  getVariables: (id) => api.get(`/servers/${id}/variables`),
  setVariable: (id, key, value) => api.put(`/servers/${id}/variables/${key}`, { value })
}

export const fileApi = {
  getAll: () => api.get('/files'),
  get: (filename) => api.get(`/files/${filename}`),
  upload: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  update: (filename, content) => api.put(`/files/${filename}`, { content }),
  delete: (filename) => api.delete(`/files/${filename}`)
}

export default api
