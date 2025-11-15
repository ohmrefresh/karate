import { useState, useEffect } from 'react'
import { Zap, Play, Trash2, BarChart3, RefreshCw } from 'lucide-react'
import { performanceApi, fileApi } from '../api'

export default function PerformanceTesting() {
  const [tests, setTests] = useState([])
  const [files, setFiles] = useState([])
  const [selectedTest, setSelectedTest] = useState(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [config, setConfig] = useState({
    name: '',
    featureFile: '',
    users: 10,
    rampUpSeconds: 10,
    durationSeconds: 60
  })

  useEffect(() => {
    loadTests()
    loadFiles()
  }, [])

  useEffect(() => {
    if (selectedTest) {
      const interval = setInterval(loadTestDetails, 3000)
      return () => clearInterval(interval)
    }
  }, [selectedTest])

  const loadTests = async () => {
    try {
      const response = await performanceApi.getAllTests()
      setTests(response.data)
    } catch (error) {
      console.error('Failed to load tests:', error)
    }
  }

  const loadFiles = async () => {
    try {
      const response = await fileApi.getAll()
      setFiles(response.data)
    } catch (error) {
      console.error('Failed to load files:', error)
    }
  }

  const loadTestDetails = async () => {
    if (!selectedTest) return

    try {
      const response = await performanceApi.getTest(selectedTest)
      const updated = tests.map(t => t.id === selectedTest ? response.data : t)
      setTests(updated)
    } catch (error) {
      console.error('Failed to load test details:', error)
    }
  }

  const handleCreateTest = async (e) => {
    e.preventDefault()
    try {
      await performanceApi.createTest(
        config.name,
        config.featureFile,
        config.users,
        config.rampUpSeconds,
        config.durationSeconds
      )
      setShowCreateModal(false)
      setConfig({ name: '', featureFile: '', users: 10, rampUpSeconds: 10, durationSeconds: 60 })
      await loadTests()
    } catch (error) {
      alert('Failed to create test: ' + (error.response?.data?.error || error.message))
    }
  }

  const handleStartTest = async (testId) => {
    try {
      await performanceApi.startTest(testId)
      await loadTests()
    } catch (error) {
      alert('Failed to start test: ' + error.message)
    }
  }

  const handleDeleteTest = async (testId) => {
    if (!confirm('Delete this test?')) return

    try {
      await performanceApi.deleteTest(testId)
      if (selectedTest === testId) setSelectedTest(null)
      await loadTests()
    } catch (error) {
      alert('Failed to delete test: ' + error.message)
    }
  }

  const currentTest = tests.find(t => t.id === selectedTest)

  return (
    <div>
      <div className="flex flex-between mb-4">
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 600, marginBottom: '0.5rem' }}>
            Performance Testing
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '0.875rem' }}>
            Load test your mocks and analyze performance metrics
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
          <Zap size={16} /> Create Test
        </button>
      </div>

      <div className="grid grid-3">
        <div className="card">
          <div className="flex flex-between mb-3">
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>
              Tests
            </h3>
            <button className="btn btn-secondary" onClick={loadTests} style={{ padding: '0.5rem' }}>
              <RefreshCw size={16} />
            </button>
          </div>

          {tests.length === 0 ? (
            <div className="empty-state">
              <Zap size={32} />
              <p>No tests created</p>
            </div>
          ) : (
            tests.map(test => (
              <div
                key={test.id}
                onClick={() => setSelectedTest(test.id)}
                style={{
                  padding: '1rem',
                  marginBottom: '0.5rem',
                  background: selectedTest === test.id ? '#334155' : '#0f172a',
                  borderRadius: '0.5rem',
                  cursor: 'pointer',
                  border: '1px solid #475569'
                }}
              >
                <div className="flex flex-between mb-2">
                  <strong>{test.name}</strong>
                  <span className={`badge ${
                    test.status === 'COMPLETED' ? 'badge-success' :
                    test.status === 'RUNNING' ? 'badge-success' :
                    test.status === 'FAILED' ? 'badge-error' : ''
                  }`}>
                    {test.status}
                  </span>
                </div>
                <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.5rem' }}>
                  <div>{test.users} users • {test.durationSeconds}s</div>
                  <div>{test.featureFile}</div>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  {test.status === 'PENDING' && (
                    <button
                      className="btn btn-primary"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleStartTest(test.id)
                      }}
                      style={{ flex: 1, padding: '0.5rem', fontSize: '0.75rem' }}
                    >
                      <Play size={14} /> Start
                    </button>
                  )}
                  <button
                    className="btn btn-danger"
                    onClick={(e) => {
                      e.stopPropagation()
                      handleDeleteTest(test.id)
                    }}
                    style={{ flex: 1, padding: '0.5rem', fontSize: '0.75rem' }}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="card" style={{ gridColumn: '2 / 4' }}>
          {currentTest ? (
            <>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
                {currentTest.name}
              </h3>

              <div className="grid grid-2 mb-4">
                <div style={{ padding: '1rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Users</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{currentTest.users}</div>
                </div>
                <div style={{ padding: '1rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Duration</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{currentTest.durationSeconds}s</div>
                </div>
                <div style={{ padding: '1rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Ramp Up</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{currentTest.rampUpSeconds}s</div>
                </div>
                <div style={{ padding: '1rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Status</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{currentTest.status}</div>
                </div>
              </div>

              {currentTest.results && (
                <>
                  <h4 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', marginTop: '2rem' }}>
                    Performance Results
                  </h4>

                  <div className="grid grid-3 mb-4">
                    <div style={{ padding: '1rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Total Requests</div>
                      <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{currentTest.results.totalRequests}</div>
                    </div>
                    <div style={{ padding: '1rem', background: 'rgba(34, 197, 94, 0.1)', borderRadius: '0.5rem' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Success</div>
                      <div style={{ fontSize: '1.5rem', fontWeight: 600, color: '#22c55e' }}>
                        {currentTest.results.successfulRequests}
                      </div>
                    </div>
                    <div style={{ padding: '1rem', background: 'rgba(239, 68, 68, 0.1)', borderRadius: '0.5rem' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Failed</div>
                      <div style={{ fontSize: '1.5rem', fontWeight: 600, color: '#ef4444' }}>
                        {currentTest.results.failedRequests}
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-2 mb-4">
                    <div style={{ padding: '1rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Requests/Second</div>
                      <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>
                        {currentTest.results.requestsPerSecond.toFixed(2)}
                      </div>
                    </div>
                    <div style={{ padding: '1rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.25rem' }}>Avg Response Time</div>
                      <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>
                        {currentTest.results.avgResponseTime.toFixed(2)}ms
                      </div>
                    </div>
                  </div>

                  <h5 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Response Time Percentiles</h5>
                  <div className="grid grid-3 mb-4">
                    <div style={{ padding: '0.75rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>P50 (Median)</div>
                      <div style={{ fontWeight: 600 }}>{currentTest.results.p50ResponseTime.toFixed(2)}ms</div>
                    </div>
                    <div style={{ padding: '0.75rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>P95</div>
                      <div style={{ fontWeight: 600 }}>{currentTest.results.p95ResponseTime.toFixed(2)}ms</div>
                    </div>
                    <div style={{ padding: '0.75rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>P99</div>
                      <div style={{ fontWeight: 600 }}>{currentTest.results.p99ResponseTime.toFixed(2)}ms</div>
                    </div>
                  </div>

                  <h5 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Status Code Distribution</h5>
                  <div className="grid grid-2 mb-4">
                    {Object.entries(currentTest.results.statusCodeDistribution).map(([code, count]) => (
                      <div key={code} style={{ padding: '0.75rem', background: '#0f172a', borderRadius: '0.5rem' }}>
                        <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>HTTP {code}</div>
                        <div style={{ fontWeight: 600 }}>{count} requests</div>
                      </div>
                    ))}
                  </div>

                  <h5 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Scenario Stats</h5>
                  <div style={{ overflowX: 'auto' }}>
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Scenario</th>
                          <th>Count</th>
                          <th>Success</th>
                          <th>Failed</th>
                          <th>Avg (ms)</th>
                          <th>Min (ms)</th>
                          <th>Max (ms)</th>
                        </tr>
                      </thead>
                      <tbody>
                        {Object.entries(currentTest.results.scenarioStats).map(([name, stats]) => (
                          <tr key={name}>
                            <td><code>{stats.name}</code></td>
                            <td>{stats.count}</td>
                            <td style={{ color: '#22c55e' }}>{stats.success}</td>
                            <td style={{ color: '#ef4444' }}>{stats.failed}</td>
                            <td>{stats.avgDuration.toFixed(2)}</td>
                            <td>{stats.minDuration}</td>
                            <td>{stats.maxDuration}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              )}

              {currentTest.status === 'RUNNING' && !currentTest.results && (
                <div className="empty-state" style={{ padding: '3rem' }}>
                  <Zap size={48} style={{ animation: 'pulse 2s infinite' }} />
                  <p>Test is running...</p>
                  <p style={{ fontSize: '0.875rem', marginTop: '0.5rem' }}>
                    Results will appear when test completes
                  </p>
                </div>
              )}

              {currentTest.status === 'PENDING' && (
                <div className="empty-state" style={{ padding: '3rem' }}>
                  <BarChart3 size={48} />
                  <p>Test is pending</p>
                  <button className="btn btn-primary mt-3" onClick={() => handleStartTest(currentTest.id)}>
                    <Play size={16} /> Start Test
                  </button>
                </div>
              )}
            </>
          ) : (
            <div className="empty-state" style={{ height: '500px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
              <BarChart3 size={64} />
              <p>Select a test to view results</p>
            </div>
          )}
        </div>
      </div>

      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>Create Performance Test</h2>
            <form onSubmit={handleCreateTest}>
              <div className="form-group">
                <label>Test Name *</label>
                <input
                  type="text"
                  className="input"
                  value={config.name}
                  onChange={e => setConfig({ ...config, name: e.target.value })}
                  placeholder="My Load Test"
                  required
                />
              </div>

              <div className="form-group">
                <label>Feature File *</label>
                <select
                  className="select"
                  value={config.featureFile}
                  onChange={e => setConfig({ ...config, featureFile: e.target.value })}
                  required
                >
                  <option value="">Select a feature file</option>
                  {files.map(file => (
                    <option key={file.name} value={file.path}>{file.name}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-2">
                <div className="form-group">
                  <label>Concurrent Users</label>
                  <input
                    type="number"
                    className="input"
                    value={config.users}
                    onChange={e => setConfig({ ...config, users: parseInt(e.target.value) })}
                    min="1"
                  />
                </div>

                <div className="form-group">
                  <label>Duration (seconds)</label>
                  <input
                    type="number"
                    className="input"
                    value={config.durationSeconds}
                    onChange={e => setConfig({ ...config, durationSeconds: parseInt(e.target.value) })}
                    min="1"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Ramp Up (seconds)</label>
                <input
                  type="number"
                  className="input"
                  value={config.rampUpSeconds}
                  onChange={e => setConfig({ ...config, rampUpSeconds: parseInt(e.target.value) })}
                  min="1"
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Create Test
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
