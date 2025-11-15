import { useState } from 'react'
import Editor from '@monaco-editor/react'
import { Upload, FileJson, Download, Sparkles } from 'lucide-react'
import { openApiApi } from '../api'

export default function OpenApiImport() {
  const [openApiContent, setOpenApiContent] = useState('')
  const [featureName, setFeatureName] = useState('')
  const [generatedFeature, setGeneratedFeature] = useState('')
  const [loading, setLoading] = useState(false)

  const handleFileUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    const reader = new FileReader()
    reader.onload = (event) => {
      setOpenApiContent(event.target.result)
    }
    reader.readAsText(file)
  }

  const handleImport = async () => {
    if (!openApiContent) {
      alert('Please provide OpenAPI specification')
      return
    }

    setLoading(true)
    try {
      const response = await openApiApi.convert(openApiContent, featureName)
      setGeneratedFeature(response.data.content)
      alert('Successfully converted OpenAPI to Karate feature!')
    } catch (error) {
      alert('Failed to convert: ' + (error.response?.data?.error || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleImportAndSave = async () => {
    if (!openApiContent) {
      alert('Please provide OpenAPI specification')
      return
    }

    setLoading(true)
    try {
      // Create a blob and file from content
      const blob = new Blob([openApiContent], { type: 'application/json' })
      const file = new File([blob], 'openapi.json')

      const response = await openApiApi.import(file, featureName)
      setGeneratedFeature(response.data.content)
      alert(`Saved as ${response.data.filename}`)
    } catch (error) {
      alert('Failed to import: ' + (error.response?.data?.error || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleDownload = () => {
    const blob = new Blob([generatedFeature], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = (featureName || 'generated') + '.feature'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div>
      <div className="mb-4">
        <h2 style={{ fontSize: '1.5rem', fontWeight: 600, marginBottom: '0.5rem' }}>
          OpenAPI/Swagger Import
        </h2>
        <p style={{ color: '#94a3b8', fontSize: '0.875rem' }}>
          Convert OpenAPI/Swagger specifications to Karate mock features automatically
        </p>
      </div>

      <div className="grid grid-2" style={{ gap: '1.5rem' }}>
        <div className="card">
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
            OpenAPI Specification
          </h3>

          <div className="form-group">
            <label>Feature Name (optional)</label>
            <input
              type="text"
              className="input"
              value={featureName}
              onChange={e => setFeatureName(e.target.value)}
              placeholder="My API"
            />
          </div>

          <div className="form-group">
            <label className="btn btn-secondary" style={{ width: '100%', justifyContent: 'center', cursor: 'pointer' }}>
              <Upload size={16} /> Upload OpenAPI File (JSON/YAML)
              <input
                type="file"
                accept=".json,.yaml,.yml"
                onChange={handleFileUpload}
                style={{ display: 'none' }}
              />
            </label>
          </div>

          <div className="form-group">
            <label>Or Paste OpenAPI Content</label>
            <div style={{ border: '1px solid #475569', borderRadius: '0.5rem', overflow: 'hidden', height: '400px' }}>
              <Editor
                height="400px"
                defaultLanguage="json"
                theme="vs-dark"
                value={openApiContent}
                onChange={setOpenApiContent}
                options={{
                  minimap: { enabled: false },
                  fontSize: 13,
                  lineNumbers: 'on',
                  scrollBeyondLastLine: false
                }}
              />
            </div>
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <button
              className="btn btn-primary"
              onClick={handleImport}
              disabled={loading || !openApiContent}
              style={{ flex: 1 }}
            >
              <Sparkles size={16} /> Convert
            </button>
            <button
              className="btn btn-primary"
              onClick={handleImportAndSave}
              disabled={loading || !openApiContent}
              style={{ flex: 1 }}
            >
              <FileJson size={16} /> Convert & Save
            </button>
          </div>
        </div>

        <div className="card">
          <div className="flex flex-between mb-3">
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>
              Generated Karate Feature
            </h3>
            {generatedFeature && (
              <button className="btn btn-secondary" onClick={handleDownload}>
                <Download size={16} /> Download
              </button>
            )}
          </div>

          {generatedFeature ? (
            <div style={{ border: '1px solid #475569', borderRadius: '0.5rem', overflow: 'hidden', height: '500px' }}>
              <Editor
                height="500px"
                defaultLanguage="gherkin"
                theme="vs-dark"
                value={generatedFeature}
                options={{
                  minimap: { enabled: false },
                  fontSize: 13,
                  lineNumbers: 'on',
                  scrollBeyondLastLine: false,
                  readOnly: true
                }}
              />
            </div>
          ) : (
            <div className="empty-state" style={{ height: '500px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
              <Sparkles size={48} />
              <p>Generated feature will appear here</p>
            </div>
          )}
        </div>
      </div>

      <div className="card mt-4">
        <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
          How It Works
        </h3>
        <div style={{ fontSize: '0.875rem', color: '#cbd5e1', lineHeight: '1.6' }}>
          <ol style={{ marginLeft: '1.5rem' }}>
            <li style={{ marginBottom: '0.5rem' }}>
              <strong>Upload or paste</strong> your OpenAPI 3.0 specification (JSON or YAML format)
            </li>
            <li style={{ marginBottom: '0.5rem' }}>
              <strong>Click "Convert"</strong> to generate Karate mock scenarios from your API specification
            </li>
            <li style={{ marginBottom: '0.5rem' }}>
              The tool automatically creates:
              <ul style={{ marginLeft: '1.5rem', marginTop: '0.25rem' }}>
                <li>Scenarios for each endpoint and HTTP method</li>
                <li>Path and query parameter extraction</li>
                <li>Response examples from your spec</li>
                <li>Appropriate status codes</li>
              </ul>
            </li>
            <li>
              <strong>Review and customize</strong> the generated feature, then save or download
            </li>
          </ol>
        </div>
      </div>
    </div>
  )
}
