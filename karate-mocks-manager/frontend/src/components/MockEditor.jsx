import { useState, useEffect } from 'react'
import Editor from '@monaco-editor/react'
import { Save, Upload, FileText, Trash2, RefreshCw } from 'lucide-react'
import { fileApi } from '../api'

export default function MockEditor() {
  const [files, setFiles] = useState([])
  const [selectedFile, setSelectedFile] = useState(null)
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)

  const loadFiles = async () => {
    try {
      const response = await fileApi.getAll()
      setFiles(response.data)
    } catch (error) {
      console.error('Failed to load files:', error)
    }
  }

  useEffect(() => {
    loadFiles()
  }, [])

  const handleFileSelect = async (filename) => {
    setLoading(true)
    try {
      const response = await fileApi.get(filename)
      setSelectedFile(filename)
      setContent(response.data.content)
    } catch (error) {
      alert('Failed to load file: ' + error.message)
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    if (!selectedFile) return

    setLoading(true)
    try {
      await fileApi.update(selectedFile, content)
      alert('File saved successfully!')
    } catch (error) {
      alert('Failed to save file: ' + error.message)
    } finally {
      setLoading(false)
    }
  }

  const handleUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    setLoading(true)
    try {
      await fileApi.upload(file)
      await loadFiles()
      alert('File uploaded successfully!')
    } catch (error) {
      alert('Failed to upload file: ' + error.message)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (filename) => {
    if (!confirm(`Delete ${filename}?`)) return

    try {
      await fileApi.delete(filename)
      if (selectedFile === filename) {
        setSelectedFile(null)
        setContent('')
      }
      await loadFiles()
    } catch (error) {
      alert('Failed to delete file: ' + error.message)
    }
  }

  return (
    <div className="grid grid-3">
      <div className="card" style={{ gridColumn: '1' }}>
        <div className="flex flex-between mb-3">
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Mock Files</h3>
          <button className="btn btn-secondary" onClick={loadFiles} style={{ padding: '0.5rem' }}>
            <RefreshCw size={16} />
          </button>
        </div>

        <label className="btn btn-primary mb-3" style={{ width: '100%', justifyContent: 'center' }}>
          <Upload size={16} /> Upload .feature
          <input type="file" accept=".feature" onChange={handleUpload} style={{ display: 'none' }} />
        </label>

        <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
          {files.length === 0 ? (
            <div className="empty-state">
              <FileText size={32} />
              <p>No mock files</p>
            </div>
          ) : (
            files.map(file => (
              <div
                key={file.name}
                style={{
                  padding: '0.75rem',
                  marginBottom: '0.5rem',
                  background: selectedFile === file.name ? '#334155' : '#0f172a',
                  borderRadius: '0.5rem',
                  cursor: 'pointer',
                  border: '1px solid #475569'
                }}
              >
                <div
                  onClick={() => handleFileSelect(file.name)}
                  style={{ marginBottom: '0.5rem' }}
                >
                  <div style={{ fontWeight: 600, marginBottom: '0.25rem' }}>{file.name}</div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                    {(file.size / 1024).toFixed(2)} KB
                  </div>
                </div>
                <button
                  className="btn btn-danger"
                  onClick={() => handleDelete(file.name)}
                  style={{ width: '100%', padding: '0.5rem', fontSize: '0.75rem' }}
                >
                  <Trash2 size={14} /> Delete
                </button>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="card" style={{ gridColumn: '2 / 4' }}>
        <div className="flex flex-between mb-3">
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>
            {selectedFile || 'Select a file to edit'}
          </h3>
          {selectedFile && (
            <button className="btn btn-primary" onClick={handleSave} disabled={loading}>
              <Save size={16} /> Save
            </button>
          )}
        </div>

        {selectedFile ? (
          <div className="monaco-container">
            <Editor
              height="500px"
              defaultLanguage="gherkin"
              theme="vs-dark"
              value={content}
              onChange={setContent}
              options={{
                minimap: { enabled: false },
                fontSize: 14,
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true
              }}
            />
          </div>
        ) : (
          <div className="empty-state" style={{ height: '500px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
            <FileText size={64} />
            <p>Select a file from the list to edit</p>
          </div>
        )}
      </div>
    </div>
  )
}
