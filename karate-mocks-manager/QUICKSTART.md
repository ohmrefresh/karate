# Quick Start Guide - Karate API Mocks Manager

## 5-Minute Setup

### Step 1: Start the Backend (Terminal 1)

```bash
cd karate-mocks-manager
mvn spring-boot:run
```

Wait for: `Started MocksManagerApplication`

### Step 2: Start the Frontend (Terminal 2)

```bash
cd karate-mocks-manager/frontend
npm install
npm run dev
```

Open: http://localhost:3000

### Step 3: Create Your First Mock Server

1. Click **"Start New Server"**
2. Fill in:
   - **Server ID**: `demo`
   - **Port**: `8080`
   - **Feature File**: `mocks/demo.feature`
3. Click **"Start Server"**

### Step 4: Test the Mock

```bash
# Health check
curl http://localhost:8080/api/health

# Get all users (empty array initially)
curl http://localhost:8080/api/users

# Create a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@example.com"}'

# Get all users again
curl http://localhost:8080/api/users

# Get specific user
curl http://localhost:8080/api/users/1
```

### Step 5: Monitor Requests

1. Go to **"Request Monitor"** tab
2. Select server: `demo`
3. Enable **"Live Mode"** checkbox
4. Send requests and watch them appear in real-time!

### Step 6: Manage State

1. Go to **"State Explorer"** tab
2. Select server: `demo`
3. View the `users` array
4. Click **"Edit"** to modify the data
5. Save and test again

## What's Next?

### Create Custom Mocks

1. Go to **"Mock Editor"** tab
2. Create a new `.feature` file
3. Upload it
4. Start a new server with your mock

### Example: Simple Product API

```gherkin
Feature: Products Mock

Background:
  * def products = [
      { id: 1, name: 'Laptop', price: 999 },
      { id: 2, name: 'Mouse', price: 29 }
    ]

Scenario: pathMatches('/products')
  * def response = products
  * def responseStatus = 200
```

Save as `products.feature`, upload, and start!

## Tips

- **Hot Reload**: Enable "Watch" when starting servers - changes to .feature files reload automatically
- **Multiple Servers**: Run different mocks on different ports simultaneously
- **Path Prefix**: Use path prefix like `/api/v1` to namespace your endpoints
- **Variables**: Use the State Explorer to debug and modify mock state without restarting

## Troubleshooting

**Backend won't start?**
- Ensure Java 17+ is installed: `java -version`
- Check port 9090 is free: `lsof -i :9090`

**Frontend won't start?**
- Ensure Node 18+ is installed: `node -version`
- Delete `node_modules` and `package-lock.json`, reinstall

**Mock server won't start?**
- Check the feature file path is correct
- Ensure the port is not already in use
- Look at backend logs for detailed error messages

**Requests not showing in monitor?**
- Enable "Live Mode" for real-time updates
- Check WebSocket connection in browser console
- Click "Refresh" to load historical requests

Enjoy managing your Karate mocks! 🚀
