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

export const openApiApi = {
  import: (file, featureName) => {
    const formData = new FormData()
    formData.append('file', file)
    if (featureName) formData.append('featureName', featureName)
    return api.post('/openapi/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  convert: (content, featureName) => api.post('/openapi/convert', { content, featureName })
}

export const recordingApi = {
  startSession: (sessionId, targetBaseUrl, proxyPort) =>
    api.post('/recording/sessions', { sessionId, targetBaseUrl, proxyPort }),
  stopSession: (sessionId) => api.delete(`/recording/sessions/${sessionId}`),
  getAllSessions: () => api.get('/recording/sessions'),
  getSession: (sessionId) => api.get(`/recording/sessions/${sessionId}`),
  getRecordings: (sessionId) => api.get(`/recording/sessions/${sessionId}/recordings`),
  generateFeature: (sessionId, featureName) =>
    api.post(`/recording/sessions/${sessionId}/generate`, { featureName }),
  clearRecordings: (sessionId) => api.delete(`/recording/sessions/${sessionId}/recordings`)
}

export const performanceApi = {
  createTest: (name, featureFile, users, rampUpSeconds, durationSeconds) =>
    api.post('/performance/tests', { name, featureFile, users, rampUpSeconds, durationSeconds }),
  startTest: (testId) => api.post(`/performance/tests/${testId}/start`),
  getAllTests: () => api.get('/performance/tests'),
  getTest: (testId) => api.get(`/performance/tests/${testId}`),
  deleteTest: (testId) => api.delete(`/performance/tests/${testId}`),
  generateSimulation: (featureFile, simulationName) =>
    api.post('/performance/generate-simulation', { featureFile, simulationName })
}

export default api
