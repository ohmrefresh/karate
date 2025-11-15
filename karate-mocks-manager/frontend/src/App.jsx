import { useState } from 'react'
import { Server } from 'lucide-react'
import Dashboard from './components/Dashboard'
import MockEditor from './components/MockEditor'
import RequestMonitor from './components/RequestMonitor'
import StateExplorer from './components/StateExplorer'

function App() {
  const [activeTab, setActiveTab] = useState('dashboard')

  return (
    <div className="app">
      <header className="header">
        <h1>
          <Server size={32} />
          Karate API Mocks Manager
        </h1>
      </header>

      <div className="container">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            Dashboard
          </button>
          <button
            className={`tab ${activeTab === 'editor' ? 'active' : ''}`}
            onClick={() => setActiveTab('editor')}
          >
            Mock Editor
          </button>
          <button
            className={`tab ${activeTab === 'monitor' ? 'active' : ''}`}
            onClick={() => setActiveTab('monitor')}
          >
            Request Monitor
          </button>
          <button
            className={`tab ${activeTab === 'state' ? 'active' : ''}`}
            onClick={() => setActiveTab('state')}
          >
            State Explorer
          </button>
        </div>

        {activeTab === 'dashboard' && <Dashboard />}
        {activeTab === 'editor' && <MockEditor />}
        {activeTab === 'monitor' && <RequestMonitor />}
        {activeTab === 'state' && <StateExplorer />}
      </div>
    </div>
  )
}

export default App
