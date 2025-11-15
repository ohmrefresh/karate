import { useState, useEffect } from 'react'
import { Activity, RefreshCw } from 'lucide-react'
import { serverApi } from '../api'
import { connectWebSocket, subscribeToRequests } from '../websocket'

export default function RequestMonitor() {
  const [servers, setServers] = useState([])
  const [selectedServer, setSelectedServer] = useState(null)
  const [requests, setRequests] = useState([])
  const [selectedRequest, setSelectedRequest] = useState(null)
  const [liveMode, setLiveMode] = useState(false)

  useEffect(() => {
    loadServers()
  }, [])

  useEffect(() => {
    let subscription = null

    if (liveMode && selectedServer) {
      const client = connectWebSocket(
        () => {
          subscription = subscribeToRequests(selectedServer, (newRequest) => {
            setRequests(prev => [newRequest, ...prev].slice(0, 100))
          })
        },
        (error) => console.error('WebSocket error:', error)
      )
    }

    return () => {
      if (subscription) {
        subscription.unsubscribe()
      }
    }
  }, [liveMode, selectedServer])

  const loadServers = async () => {
    try {
      const response = await serverApi.getAll()
      setServers(response.data)
      if (response.data.length > 0 && !selectedServer) {
        setSelectedServer(response.data[0].id)
      }
    } catch (error) {
      console.error('Failed to load servers:', error)
    }
  }

  const loadRequests = async () => {
    if (!selectedServer) return

    try {
      const response = await serverApi.getRequests(selectedServer, 100)
      setRequests(response.data)
    } catch (error) {
      console.error('Failed to load requests:', error)
    }
  }

  useEffect(() => {
    if (selectedServer && !liveMode) {
      loadRequests()
    }
  }, [selectedServer])

  return (
    <div>
      <div className="flex flex-between mb-4">
        <div className="flex flex-gap">
          <select
            className="select"
            value={selectedServer || ''}
            onChange={e => setSelectedServer(e.target.value)}
            style={{ width: 'auto', minWidth: '200px' }}
          >
            <option value="">Select Server</option>
            {servers.map(server => (
              <option key={server.id} value={server.id}>
                {server.id} (:{server.port})
              </option>
            ))}
          </select>

          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#cbd5e1' }}>
            <input
              type="checkbox"
              checked={liveMode}
              onChange={e => setLiveMode(e.target.checked)}
            />
            Live Mode
            {liveMode && <Activity size={16} style={{ color: '#22c55e' }} />}
          </label>
        </div>

        <button className="btn btn-secondary" onClick={loadRequests} disabled={!selectedServer || liveMode}>
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      <div className="grid grid-2">
        <div className="card">
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
            Requests ({requests.length})
          </h3>

          <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
            {requests.length === 0 ? (
              <div className="empty-state">
                <Activity size={32} />
                <p>No requests yet</p>
              </div>
            ) : (
              requests.map(req => (
                <div
                  key={req.id}
                  onClick={() => setSelectedRequest(req)}
                  style={{
                    padding: '0.75rem',
                    marginBottom: '0.5rem',
                    background: selectedRequest?.id === req.id ? '#334155' : '#0f172a',
                    borderRadius: '0.5rem',
                    cursor: 'pointer',
                    border: '1px solid #475569'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <span className={`method-badge method-${req.method}`}>{req.method}</span>
                    <code style={{ flex: 1, fontSize: '0.875rem' }}>{req.path}</code>
                    <span style={{
                      padding: '0.25rem 0.5rem',
                      borderRadius: '0.25rem',
                      fontSize: '0.75rem',
                      background: req.responseStatus < 400 ? 'rgba(34, 197, 94, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                      color: req.responseStatus < 400 ? '#22c55e' : '#ef4444'
                    }}>
                      {req.responseStatus}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                    {new Date(req.timestamp).toLocaleTimeString()} • {req.durationMs}ms
                    {req.matchedScenario && ` • ${req.matchedScenario}`}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="card">
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
            Request Details
          </h3>

          {selectedRequest ? (
            <div>
              <div className="mb-3">
                <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: '#94a3b8', marginBottom: '0.5rem' }}>
                  REQUEST
                </h4>
                <div className="log-entry">
                  <div><strong>{selectedRequest.method}</strong> {selectedRequest.path}</div>
                  <div style={{ marginTop: '0.5rem', color: '#94a3b8' }}>Headers:</div>
                  <pre>{JSON.stringify(selectedRequest.headers, null, 2)}</pre>
                  {selectedRequest.body && (
                    <>
                      <div style={{ marginTop: '0.5rem', color: '#94a3b8' }}>Body:</div>
                      <pre>{selectedRequest.body}</pre>
                    </>
                  )}
                </div>
              </div>

              <div>
                <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: '#94a3b8', marginBottom: '0.5rem' }}>
                  RESPONSE ({selectedRequest.responseStatus})
                </h4>
                <div className="log-entry">
                  <pre>{selectedRequest.responseBody || '(empty)'}</pre>
                </div>
              </div>

              <div className="mt-3" style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                <div>Duration: {selectedRequest.durationMs}ms</div>
                <div>Timestamp: {new Date(selectedRequest.timestamp).toLocaleString()}</div>
                {selectedRequest.matchedScenario && (
                  <div>Matched Scenario: {selectedRequest.matchedScenario}</div>
                )}
              </div>
            </div>
          ) : (
            <div className="empty-state">
              <p>Select a request to view details</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
