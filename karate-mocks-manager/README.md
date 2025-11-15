# Karate API Mocks Manager

A modern web application to manage Karate API Mock servers with real-time monitoring, visual editor, and state management.

## Features

### Phase 1 MVP (Complete)

- **Start/Stop Mock Servers**: Launch and manage multiple mock servers from a web UI
- **Upload & Edit .feature Files**: Visual editor with syntax highlighting for mock definitions
- **Live Request/Response Monitoring**: Real-time traffic inspection via WebSocket
- **State Management**: View and edit global variables in running mocks
- **Server Configuration**: Configure ports, path prefixes, SSL, and hot-reload

## Architecture

```
┌─────────────────────────────────────┐
│   React Frontend (Vite)             │
│   - Dashboard                       │
│   - Mock Editor (Monaco)            │
│   - Request Monitor                 │
│   - State Explorer                  │
└──────────────┬──────────────────────┘
               │ REST + WebSocket
┌──────────────▼──────────────────────┐
│   Spring Boot Backend               │
│   - Mock Server Management API      │
│   - File Upload/Management          │
│   - WebSocket for live updates      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Enhanced Karate MockServer        │
│   - Request/Response logging        │
│   - Variable access                 │
│   - Performance tracking            │
└─────────────────────────────────────┘
```

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Maven

### Backend Setup

```bash
cd karate-mocks-manager

# Build the backend
mvn clean install

# Run the backend
mvn spring-boot:run
```

The backend will start on http://localhost:9090

### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Run the development server
npm run dev
```

The frontend will start on http://localhost:3000

### Quick Start

1. **Create a mock file** or use the example `mocks/demo.feature`
2. **Open the UI** at http://localhost:3000
3. **Start a mock server**:
   - Go to Dashboard tab
   - Click "Start New Server"
   - Fill in:
     - Server ID: `my-first-mock`
     - Port: `8080`
     - Feature File: `mocks/demo.feature`
   - Click "Start Server"
4. **Test your mock**:
   ```bash
   curl http://localhost:8080/api/health
   ```
5. **Monitor requests** in the "Request Monitor" tab

## API Documentation

### REST API Endpoints

#### Mock Server Management

```
GET    /api/servers          - List all running servers
POST   /api/servers          - Start a new server
GET    /api/servers/{id}     - Get server details
DELETE /api/servers/{id}     - Stop a server
GET    /api/servers/{id}/requests?limit=100  - Get request logs
GET    /api/servers/{id}/variables           - Get global variables
PUT    /api/servers/{id}/variables/{key}     - Set a variable
```

#### Mock File Management

```
GET    /api/files            - List all mock files
GET    /api/files/{name}     - Get file content
POST   /api/files/upload     - Upload a .feature file
PUT    /api/files/{name}     - Update file content
DELETE /api/files/{name}     - Delete a file
```

### WebSocket

Connect to `/ws` for real-time updates:

```javascript
// Subscribe to request logs for a specific server
SUBSCRIBE /topic/requests/{serverId}
```

## Usage Examples

### Example 1: Simple REST API Mock

Create `mocks/users.feature`:

```gherkin
Feature: Users API

Background:
  * def users = [{ id: 1, name: 'John' }, { id: 2, name: 'Jane' }]

Scenario: pathMatches('/users') && methodIs('get')
  * def response = users
  * def responseStatus = 200

Scenario: pathMatches('/users/{id}') && methodIs('get')
  * def id = parseInt(pathParams.id)
  * def user = users.find(u => u.id == id)
  * def response = user || { error: 'Not found' }
  * def responseStatus = user ? 200 : 404
```

Start the server via UI, then test:

```bash
curl http://localhost:8080/users
curl http://localhost:8080/users/1
```

### Example 2: Stateful Mock with Variables

```gherkin
Feature: Counter API

Background:
  * def count = 0

Scenario: pathMatches('/increment')
  * eval count = count + 1
  * def response = { count: count }
  * def responseStatus = 200

Scenario: pathMatches('/reset')
  * eval count = 0
  * def response = { count: count }
  * def responseStatus = 200
```

You can view and edit the `count` variable in real-time via the State Explorer tab!

### Example 3: Dynamic Response Based on Headers

```gherkin
Scenario: pathMatches('/api/data')
  * def apiKey = headerValues('X-API-Key')[0]
  * def authorized = apiKey == 'secret123'
  * def responseStatus = authorized ? 200 : 401
  * def response = authorized ? { data: 'sensitive info' } : { error: 'Unauthorized' }
```

## UI Components

### Dashboard
- View all running mock servers
- Start new servers with custom configuration
- Stop running servers
- Monitor server metrics (requests, uptime)

### Mock Editor
- Upload `.feature` files
- Edit mocks with Monaco editor (VS Code's editor)
- Syntax highlighting for Gherkin
- Save changes with hot-reload

### Request Monitor
- Real-time request/response logging
- Filter by server
- View request details (headers, body, matched scenario)
- Performance metrics (response time)
- Live mode with WebSocket updates

### State Explorer
- View global variables for each server
- Edit variables in real-time
- Add new variables
- JSON editing with validation

## Configuration

### application.yml

```yaml
server:
  port: 9090

spring:
  servlet:
    multipart:
      max-file-size: 10MB

logging:
  level:
    com.karate.mockmgr: DEBUG
```

## Development

### Project Structure

```
karate-mocks-manager/
├── src/main/java/com/karate/mockmgr/
│   ├── controller/          # REST controllers
│   ├── service/             # Business logic
│   ├── model/               # Data models
│   └── config/              # Spring configuration
├── frontend/
│   └── src/
│       ├── components/      # React components
│       ├── api.js          # API client
│       └── websocket.js    # WebSocket client
└── mocks/                   # Example mock files
```

### Building for Production

Backend:
```bash
mvn clean package
java -jar target/karate-mocks-manager-1.0.0.jar
```

Frontend:
```bash
cd frontend
npm run build
# Serve the dist/ folder with any static server
```

## Troubleshooting

### Port Already in Use
If you get "Address already in use" error, either:
- Stop the process using that port
- Choose a different port in the configuration

### Feature File Not Found
Ensure the path is relative to where you run the backend, or use absolute paths.

### WebSocket Connection Failed
Check that:
- Backend is running on port 9090
- CORS is properly configured
- No firewall blocking WebSocket connections

## Future Enhancements (Phase 2+)

- [ ] OpenAPI spec import
- [ ] Mock recording from real APIs
- [ ] Performance testing integration
- [ ] Docker/Kubernetes deployment
- [ ] Multi-user authentication
- [ ] Mock versioning and history
- [ ] Export/import mock collections
- [ ] Scenario debugging tools
- [ ] AI-powered mock generation

## Contributing

Contributions welcome! Please feel free to submit issues or pull requests.

## License

This project is part of the Karate ecosystem.

## Support

For questions about Karate's mock feature, see: https://github.com/karatelabs/karate/tree/master/karate-netty
