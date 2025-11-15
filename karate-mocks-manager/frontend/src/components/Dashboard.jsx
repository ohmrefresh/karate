import { useState, useEffect } from 'react'
import { Play, Square, Trash2, Plus, RefreshCw } from 'lucide-react'
import { serverApi } from '../api'

export default function Dashboard() {
  const [servers, setServers] = useState([])
  const [showModal, setShowModal] = useState(false)
  const [loading, setLoading] = useState(false)
  const [config, setConfig] = useState({
    id: '',
    port: 8080,
    featureFile: '',
    pathPrefix: '',
    ssl: false,
    watch: true
  })

  const loadServers = async () => {
    try {
      const response = await serverApi.getAll()
      setServers(response.data)
    } catch (error) {
      console.error('Failed to load servers:', error)
    }
  }

  useEffect(() => {
    loadServers()
    const interval = setInterval(loadServers, 5000)
    return () => clearInterval(interval)
  }, [])

  const handleStartServer = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await serverApi.start(config)
      setShowModal(false)
      setConfig({ id: '', port: 8080, featureFile: '', pathPrefix: '', ssl: false, watch: true })
      await loadServers()
    } catch (error) {
      alert('Failed to start server: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleStopServer = async (id) => {
    if (!confirm('Are you sure you want to stop this server?')) return

    try {
      await serverApi.stop(id)
      await loadServers()
    } catch (error) {
      alert('Failed to stop server: ' + (error.response?.data?.message || error.message))
    }
  }

  return (
    <div>
      <div className="flex flex-between mb-4">
        <h2 style={{ fontSize: '1.5rem', fontWeight: 600 }}>Mock Servers</h2>
        <div className="flex flex-gap">
          <button className="btn btn-secondary" onClick={loadServers}>
            <RefreshCw size={16} /> Refresh
          </button>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>
            <Plus size={16} /> Start New Server
          </button>
        </div>
      </div>

      {servers.length === 0 ? (
        <div className="card empty-state">
          <Play size={48} style={{ margin: '0 auto 1rem', opacity: 0.5 }} />
          <p>No mock servers running</p>
          <p style={{ fontSize: '0.875rem', marginTop: '0.5rem' }}>
            Click "Start New Server" to create one
          </p>
        </div>
      ) : (
        <div className="grid grid-2">
          {servers.map(server => (
            <div key={server.id} className="card">
              <div className="flex flex-between mb-3">
                <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                  <span className="status-dot running"></span>
                  {server.id}
                </h3>
                <span className="badge badge-success">RUNNING</span>
              </div>

              <div style={{ marginBottom: '1rem', fontSize: '0.875rem' }}>
                <div style={{ marginBottom: '0.5rem' }}>
                  <strong>Port:</strong> {server.port}
                </div>
                <div style={{ marginBottom: '0.5rem' }}>
                  <strong>Requests:</strong> {server.requestCount}
                </div>
                <div style={{ marginBottom: '0.5rem' }}>
                  <strong>Started:</strong> {new Date(server.startedAt).toLocaleString()}
                </div>
                {server.pathPrefix && (
                  <div>
                    <strong>Path Prefix:</strong> {server.pathPrefix}
                  </div>
                )}
              </div>

              <button
                className="btn btn-danger"
                onClick={() => handleStopServer(server.id)}
                style={{ width: '100%' }}
              >
                <Square size={16} /> Stop Server
              </button>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>Start Mock Server</h2>
            <form onSubmit={handleStartServer}>
              <div className="form-group">
                <label>Server ID *</label>
                <input
                  type="text"
                  className="input"
                  value={config.id}
                  onChange={e => setConfig({ ...config, id: e.target.value })}
                  placeholder="my-mock-server"
                  required
                />
              </div>

              <div className="form-group">
                <label>Port *</label>
                <input
                  type="number"
                  className="input"
                  value={config.port}
                  onChange={e => setConfig({ ...config, port: parseInt(e.target.value) })}
                  required
                />
              </div>

              <div className="form-group">
                <label>Feature File Path *</label>
                <input
                  type="text"
                  className="input"
                  value={config.featureFile}
                  onChange={e => setConfig({ ...config, featureFile: e.target.value })}
                  placeholder="mocks/demo.feature"
                  required
                />
              </div>

              <div className="form-group">
                <label>Path Prefix (optional)</label>
                <input
                  type="text"
                  className="input"
                  value={config.pathPrefix}
                  onChange={e => setConfig({ ...config, pathPrefix: e.target.value })}
                  placeholder="/api/v1"
                />
              </div>

              <div className="form-group">
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <input
                    type="checkbox"
                    checked={config.watch}
                    onChange={e => setConfig({ ...config, watch: e.target.checked })}
                  />
                  Watch for file changes (hot reload)
                </label>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowModal(false)}
                >
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'Starting...' : 'Start Server'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
