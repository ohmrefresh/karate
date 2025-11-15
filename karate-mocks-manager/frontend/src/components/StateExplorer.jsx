import { useState, useEffect } from 'react'
import { Database, Edit2, Plus, RefreshCw, Save, X } from 'lucide-react'
import { serverApi } from '../api'

export default function StateExplorer() {
  const [servers, setServers] = useState([])
  const [selectedServer, setSelectedServer] = useState(null)
  const [variables, setVariables] = useState({})
  const [editingKey, setEditingKey] = useState(null)
  const [editValue, setEditValue] = useState('')
  const [showAddModal, setShowAddModal] = useState(false)
  const [newKey, setNewKey] = useState('')
  const [newValue, setNewValue] = useState('')

  useEffect(() => {
    loadServers()
  }, [])

  useEffect(() => {
    if (selectedServer) {
      loadVariables()
    }
  }, [selectedServer])

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

  const loadVariables = async () => {
    if (!selectedServer) return

    try {
      const response = await serverApi.getVariables(selectedServer)
      setVariables(response.data)
    } catch (error) {
      console.error('Failed to load variables:', error)
    }
  }

  const handleEdit = (key) => {
    setEditingKey(key)
    setEditValue(JSON.stringify(variables[key], null, 2))
  }

  const handleSave = async (key) => {
    try {
      const value = JSON.parse(editValue)
      await serverApi.setVariable(selectedServer, key, value)
      setEditingKey(null)
      await loadVariables()
    } catch (error) {
      alert('Failed to save variable: ' + error.message)
    }
  }

  const handleAdd = async (e) => {
    e.preventDefault()
    try {
      const value = JSON.parse(newValue)
      await serverApi.setVariable(selectedServer, newKey, value)
      setShowAddModal(false)
      setNewKey('')
      setNewValue('')
      await loadVariables()
    } catch (error) {
      alert('Failed to add variable: ' + error.message)
    }
  }

  const renderValue = (value) => {
    if (typeof value === 'object') {
      return <pre style={{ margin: 0 }}>{JSON.stringify(value, null, 2)}</pre>
    }
    return String(value)
  }

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
        </div>

        <div className="flex flex-gap">
          <button className="btn btn-secondary" onClick={loadVariables} disabled={!selectedServer}>
            <RefreshCw size={16} /> Refresh
          </button>
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)} disabled={!selectedServer}>
            <Plus size={16} /> Add Variable
          </button>
        </div>
      </div>

      <div className="card">
        <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
          Global Variables
        </h3>

        {Object.keys(variables).length === 0 ? (
          <div className="empty-state">
            <Database size={48} />
            <p>No variables set</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="table">
              <thead>
                <tr>
                  <th style={{ width: '30%' }}>Key</th>
                  <th>Value</th>
                  <th style={{ width: '100px', textAlign: 'center' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(variables).map(([key, value]) => (
                  <tr key={key}>
                    <td>
                      <code style={{ fontSize: '0.875rem', color: '#60a5fa' }}>{key}</code>
                    </td>
                    <td>
                      {editingKey === key ? (
                        <textarea
                          className="input"
                          value={editValue}
                          onChange={e => setEditValue(e.target.value)}
                          rows={5}
                          style={{ fontFamily: 'monospace', fontSize: '0.75rem' }}
                        />
                      ) : (
                        <div style={{ fontSize: '0.875rem', fontFamily: 'monospace' }}>
                          {renderValue(value)}
                        </div>
                      )}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      {editingKey === key ? (
                        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                          <button
                            className="btn btn-primary"
                            onClick={() => handleSave(key)}
                            style={{ padding: '0.5rem' }}
                          >
                            <Save size={14} />
                          </button>
                          <button
                            className="btn btn-secondary"
                            onClick={() => setEditingKey(null)}
                            style={{ padding: '0.5rem' }}
                          >
                            <X size={14} />
                          </button>
                        </div>
                      ) : (
                        <button
                          className="btn btn-secondary"
                          onClick={() => handleEdit(key)}
                          style={{ padding: '0.5rem' }}
                        >
                          <Edit2 size={14} />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>Add Variable</h2>
            <form onSubmit={handleAdd}>
              <div className="form-group">
                <label>Key *</label>
                <input
                  type="text"
                  className="input"
                  value={newKey}
                  onChange={e => setNewKey(e.target.value)}
                  placeholder="myVariable"
                  required
                />
              </div>

              <div className="form-group">
                <label>Value (JSON) *</label>
                <textarea
                  className="input"
                  value={newValue}
                  onChange={e => setNewValue(e.target.value)}
                  placeholder='{"key": "value"} or "string" or 123'
                  rows={5}
                  style={{ fontFamily: 'monospace' }}
                  required
                />
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowAddModal(false)}
                >
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Add
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
