import { useState, useEffect } from 'react'
import Editor from '@monaco-editor/react'
import { Radio, Square, Play, Download, Trash2, RefreshCw } from 'lucide-react'
import { recordingApi } from '../api'

export default function RecordingProxy() {
  const [sessions, setSessions] = useState([])
  const [selectedSession, setSelectedSession] = useState(null)
  const [recordings, setRecordings] = useState([])
  const [selectedRecording, setSelectedRecording] = useState(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [generatedFeature, setGeneratedFeature] = useState(null)
  const [config, setConfig] = useState({
    sessionId: '',
    targetBaseUrl: '',
    proxyPort: 9999
  })

  useEffect(() => {
    loadSessions()
  }, [])

  useEffect(() => {
    if (selectedSession) {
      loadRecordings()
      const interval = setInterval(loadRecordings, 3000)
      return () => clearInterval(interval)
    }
  }, [selectedSession])

  const loadSessions = async () => {
    try {
      const response = await recordingApi.getAllSessions()
      setSessions(response.data)
    } catch (error) {
      console.error('Failed to load sessions:', error)
    }
  }

  const loadRecordings = async () => {
    if (!selectedSession) return

    try {
      const response = await recordingApi.getRecordings(selectedSession)
      setRecordings(response.data)
    } catch (error) {
      console.error('Failed to load recordings:', error)
    }
  }

  const handleCreateSession = async (e) => {
    e.preventDefault()
    try {
      await recordingApi.startSession(config.sessionId, config.targetBaseUrl, config.proxyPort)
      setShowCreateModal(false)
      setConfig({ sessionId: '', targetBaseUrl: '', proxyPort: 9999 })
      await loadSessions()
    } catch (error) {
      alert('Failed to create session: ' + (error.response?.data?.error || error.message))
    }
  }

  const handleStopSession = async (sessionId) => {
    if (!confirm('Stop this recording session?')) return

    try {
      await recordingApi.stopSession(sessionId)
      await loadSessions()
    } catch (error) {
      alert('Failed to stop session: ' + error.message)
    }
  }

  const handleGenerateFeature = async () => {
    if (!selectedSession) return

    const featureName = prompt('Enter feature name:')
    if (!featureName) return

    try {
      const response = await recordingApi.generateFeature(selectedSession, featureName)
      setGeneratedFeature(response.data.content)
      alert(`Feature saved as ${response.data.filename}`)
    } catch (error) {
      alert('Failed to generate feature: ' + (error.response?.data?.error || error.message))
    }
  }

  const handleClearRecordings = async () => {
    if (!selectedSession || !confirm('Clear all recordings?')) return

    try {
      await recordingApi.clearRecordings(selectedSession)
      setRecordings([])
      setSelectedSession(null)
      await loadSessions()
    } catch (error) {
      alert('Failed to clear recordings: ' + error.message)
    }
  }

  const currentSession = sessions.find(s => s.id === selectedSession)

  return (
    <div>
      <div className="flex flex-between mb-4">
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 600, marginBottom: '0.5rem' }}>
            API Recording Proxy
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '0.875rem' }}>
            Record real API traffic and generate mock features automatically
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
          <Play size={16} /> Start Recording
        </button>
      </div>

      <div className="grid grid-3" style={{ marginBottom: '1.5rem' }}>
        <div className="card">
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
            Recording Sessions
          </h3>

          {sessions.length === 0 ? (
            <div className="empty-state">
              <Radio size={32} />
              <p>No active sessions</p>
            </div>
          ) : (
            sessions.map(session => (
              <div
                key={session.id}
                onClick={() => setSelectedSession(session.id)}
                style={{
                  padding: '1rem',
                  marginBottom: '0.5rem',
                  background: selectedSession === session.id ? '#334155' : '#0f172a',
                  borderRadius: '0.5rem',
                  cursor: 'pointer',
                  border: '1px solid #475569'
                }}
              >
                <div className="flex flex-between mb-2">
                  <strong>{session.id}</strong>
                  <span className={`badge ${session.status === 'ACTIVE' ? 'badge-success' : 'badge-error'}`}>
                    {session.status}
                  </span>
                </div>
                <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                  <div>Target: {session.targetBaseUrl}</div>
                  <div>Port: {session.proxyPort}</div>
                  <div>Recorded: {session.recordedCount} requests</div>
                </div>
                {session.status === 'ACTIVE' && (
                  <button
                    className="btn btn-danger"
                    onClick={(e) => {
                      e.stopPropagation()
                      handleStopSession(session.id)
                    }}
                    style={{ width: '100%', marginTop: '0.5rem', padding: '0.5rem' }}
                  >
                    <Square size={14} /> Stop
                  </button>
                )}
              </div>
            ))
          )}
        </div>

        <div className="card" style={{ gridColumn: '2 / 4' }}>
          {currentSession ? (
            <>
              <div className="flex flex-between mb-3">
                <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                  Recordings ({recordings.length})
                </h3>
                <div className="flex flex-gap">
                  <button className="btn btn-secondary" onClick={loadRecordings}>
                    <RefreshCw size={16} />
                  </button>
                  <button className="btn btn-primary" onClick={handleGenerateFeature} disabled={recordings.length === 0}>
                    <Download size={16} /> Generate Feature
                  </button>
                  <button className="btn btn-danger" onClick={handleClearRecordings}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>

              <div style={{ marginBottom: '1rem', padding: '1rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                <div style={{ fontSize: '0.875rem', marginBottom: '0.5rem' }}>
                  <strong>Proxy URL:</strong> http://localhost:9090/api/recording/proxy/{currentSession.id}
                </div>
                <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                  Send requests to this URL to record them. Example:
                  <code style={{ display: 'block', marginTop: '0.25rem', padding: '0.5rem', background: '#1e293b', borderRadius: '0.25rem' }}>
                    curl http://localhost:9090/api/recording/proxy/{currentSession.id}/users
                  </code>
                </div>
              </div>

              <div style={{ maxHeight: '500px', overflowY: 'auto' }}>
                {recordings.length === 0 ? (
                  <div className="empty-state">
                    <Radio size={32} />
                    <p>No requests recorded yet</p>
                    <p style={{ fontSize: '0.875rem', marginTop: '0.5rem' }}>
                      Send requests to the proxy URL above
                    </p>
                  </div>
                ) : (
                  recordings.map(rec => (
                    <div
                      key={rec.id}
                      onClick={() => setSelectedRecording(rec)}
                      style={{
                        padding: '0.75rem',
                        marginBottom: '0.5rem',
                        background: selectedRecording?.id === rec.id ? '#334155' : '#0f172a',
                        borderRadius: '0.5rem',
                        cursor: 'pointer',
                        border: '1px solid #475569'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                        <span className={`method-badge method-${rec.method}`}>{rec.method}</span>
                        <code style={{ flex: 1, fontSize: '0.875rem' }}>{rec.path}</code>
                        <span style={{
                          padding: '0.25rem 0.5rem',
                          borderRadius: '0.25rem',
                          fontSize: '0.75rem',
                          background: rec.responseStatus < 400 ? 'rgba(34, 197, 94, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                          color: rec.responseStatus < 400 ? '#22c55e' : '#ef4444'
                        }}>
                          {rec.responseStatus}
                        </span>
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                        {new Date(rec.timestamp).toLocaleTimeString()} • {rec.durationMs}ms
                      </div>
                    </div>
                  ))
                )}
              </div>
            </>
          ) : (
            <div className="empty-state" style={{ height: '500px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
              <Radio size={64} />
              <p>Select a recording session</p>
            </div>
          )}
        </div>
      </div>

      {selectedRecording && (
        <div className="card">
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
            Request Details
          </h3>
          <div className="grid grid-2">
            <div>
              <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: '#94a3b8', marginBottom: '0.5rem' }}>REQUEST</h4>
              <div className="log-entry">
                <div><strong>{selectedRecording.method}</strong> {selectedRecording.path}</div>
                {selectedRecording.body && (
                  <>
                    <div style={{ marginTop: '0.5rem', color: '#94a3b8' }}>Body:</div>
                    <pre>{selectedRecording.body}</pre>
                  </>
                )}
              </div>
            </div>
            <div>
              <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: '#94a3b8', marginBottom: '0.5rem' }}>
                RESPONSE ({selectedRecording.responseStatus})
              </h4>
              <div className="log-entry">
                <pre>{selectedRecording.responseBody || '(empty)'}</pre>
              </div>
            </div>
          </div>
        </div>
      )}

      {generatedFeature && (
        <div className="card mt-4">
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
            Generated Feature
          </h3>
          <div style={{ border: '1px solid #475569', borderRadius: '0.5rem', overflow: 'hidden', height: '400px' }}>
            <Editor
              height="400px"
              defaultLanguage="gherkin"
              theme="vs-dark"
              value={generatedFeature}
              options={{
                minimap: { enabled: false },
                fontSize: 13,
                readOnly: true
              }}
            />
          </div>
        </div>
      )}

      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>Start Recording Session</h2>
            <form onSubmit={handleCreateSession}>
              <div className="form-group">
                <label>Session ID *</label>
                <input
                  type="text"
                  className="input"
                  value={config.sessionId}
                  onChange={e => setConfig({ ...config, sessionId: e.target.value })}
                  placeholder="my-recording"
                  required
                />
              </div>

              <div className="form-group">
                <label>Target API Base URL *</label>
                <input
                  type="url"
                  className="input"
                  value={config.targetBaseUrl}
                  onChange={e => setConfig({ ...config, targetBaseUrl: e.target.value })}
                  placeholder="https://api.example.com"
                  required
                />
              </div>

              <div className="form-group">
                <label>Proxy Port</label>
                <input
                  type="number"
                  className="input"
                  value={config.proxyPort}
                  onChange={e => setConfig({ ...config, proxyPort: parseInt(e.target.value) })}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Start Recording
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
