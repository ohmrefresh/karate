# Phase 2 Features Guide

This guide covers the advanced Phase 2 features of Karate API Mocks Manager.

## Table of Contents

1. [OpenAPI/Swagger Import](#openapi-import)
2. [API Recording Proxy](#api-recording)
3. [Performance Testing](#performance-testing)

---

## OpenAPI Import

Automatically generate Karate mock features from OpenAPI 3.0 specifications.

### How It Works

The OpenAPI importer:
1. Parses your OpenAPI/Swagger specification (JSON or YAML)
2. Generates Karate scenarios for each endpoint and HTTP method
3. Extracts path/query parameters automatically
4. Creates response examples from your spec
5. Sets appropriate HTTP status codes

### Usage

#### Via UI

1. Go to **"OpenAPI Import"** tab
2. Upload your OpenAPI file or paste the content
3. Optionally provide a feature name
4. Click **"Convert"** to preview or **"Convert & Save"** to save directly

#### Via API

```bash
# Upload and convert OpenAPI file
curl -X POST http://localhost:9090/api/openapi/import \
  -F "file=@swagger.json" \
  -F "featureName=MyAPI"

# Convert OpenAPI content
curl -X POST http://localhost:9090/api/openapi/convert \
  -H "Content-Type: application/json" \
  -d '{
    "content": "...",
    "featureName": "MyAPI"
  }'
```

### Example

**Input (OpenAPI):**

```yaml
openapi: 3.0.0
info:
  title: Users API
  version: 1.0.0
paths:
  /users:
    get:
      summary: Get all users
      responses:
        '200':
          description: Success
          content:
            application/json:
              example:
                - id: 1
                  name: John Doe
  /users/{id}:
    get:
      summary: Get user by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: Success
```

**Output (Karate Feature):**

```gherkin
Feature: MyAPI

Background:
  * def apiData = {}

Scenario: Get all users
  * def pathMatch = pathMatches('/users')
  * def methodMatch = methodIs('get')
  * if (!pathMatch || !methodMatch) karate.abort()

  * def responseStatus = 200
  * def response = [{ "id": 1, "name": "John Doe" }]

Scenario: Get user by ID
  * def pathMatch = pathMatches('/users/{id}')
  * def methodMatch = methodIs('get')
  * if (!pathMatch || !methodMatch) karate.abort()

  # Path parameters
  * def id = pathParams.id

  * def responseStatus = 200
  * def response = { "id": 1, "name": "John Doe" }
```

### Benefits

- **Time Savings**: Convert entire APIs to mocks in seconds
- **Consistency**: Generated mocks match your API contract
- **Up-to-date**: Regenerate mocks when your API spec changes
- **Quick Prototyping**: Start mocking before backend is ready

---

## API Recording

Record real API traffic and automatically generate mock features.

### How It Works

The recording proxy:
1. Acts as an HTTP proxy between your client and the real API
2. Captures all request/response pairs
3. Stores recordings in memory
4. Generates Karate features from recorded traffic

### Usage

#### Start a Recording Session

1. Go to **"Recording"** tab
2. Click **"Start Recording"**
3. Enter:
   - **Session ID**: Unique identifier (e.g., `my-recording`)
   - **Target API URL**: Real API base URL (e.g., `https://api.github.com`)
   - **Proxy Port**: Port for proxy (default: 9999)

#### Send Requests Through Proxy

```bash
# Example: Record GitHub API calls
SESSION_ID="github-api"

# Send requests to the proxy instead of the real API
curl http://localhost:9090/api/recording/proxy/$SESSION_ID/users/octocat
curl http://localhost:9090/api/recording/proxy/$SESSION_ID/repos/karatelabs/karate
```

The proxy will:
- Forward requests to the real API (`https://api.github.com`)
- Capture request/response details
- Return the actual response to your client

#### Generate Feature File

1. Click **"Generate Feature"** in the UI
2. Provide a feature name
3. The tool creates a `.feature` file with all recorded scenarios

### Example

**Recorded Traffic:**

```
GET /users/1 → 200 OK {"id": 1, "name": "Alice"}
GET /users/2 → 200 OK {"id": 2, "name": "Bob"}
POST /users → 201 Created {"id": 3, "name": "Charlie"}
```

**Generated Feature:**

```gherkin
Feature: Recorded from github-api

Background:
  # Generated from recorded API traffic
  * def recordedData = {}

Scenario: GET /users/1
  * def pathMatch = pathMatches('/users/1')
  * def methodMatch = methodIs('get')
  * if (!pathMatch || !methodMatch) karate.abort()

  * def responseStatus = 200
  * def response =
    """
    {"id": 1, "name": "Alice"}
    """
  # 2 instances recorded

Scenario: POST /users
  * def pathMatch = pathMatches('/users')
  * def methodMatch = methodIs('post')
  * if (!pathMatch || !methodMatch) karate.abort()

  * def responseStatus = 201
  * def response =
    """
    {"id": 3, "name": "Charlie"}
    """
```

### Use Cases

1. **Shadow Testing**: Record production traffic to create realistic mocks
2. **API Migration**: Capture behavior of legacy API before replacement
3. **Documentation**: Auto-generate examples from real usage
4. **Contract Testing**: Verify mock matches real API behavior

### Tips

- **Filtering**: Review and edit recordings before generating features
- **Privacy**: Clear sensitive data (tokens, passwords) from recordings
- **Deduplication**: The tool groups similar requests automatically
- **Customization**: Edit generated features to add logic/validation

---

## Performance Testing

Load test your mocks with integrated Karate Gatling support.

### How It Works

The performance tester:
1. Uses your existing `.feature` files as test scenarios
2. Simulates concurrent users
3. Tracks detailed performance metrics
4. Provides real-time and historical results

### Usage

#### Create a Performance Test

1. Go to **"Performance"** tab
2. Click **"Create Test"**
3. Configure:
   - **Test Name**: Descriptive name
   - **Feature File**: Select mock feature to test
   - **Concurrent Users**: Number of simultaneous users (e.g., 50)
   - **Ramp Up**: Time to reach max users (e.g., 10s)
   - **Duration**: How long to run test (e.g., 60s)

#### Run the Test

1. Click **"Start"** on your test
2. Monitor progress in real-time
3. View results when complete

### Metrics Tracked

#### Overview Metrics
- **Total Requests**: All requests made
- **Success Rate**: Percentage of successful requests
- **Requests per Second**: Throughput
- **Response Times**: Min, Max, Average

#### Percentiles
- **P50 (Median)**: 50% of requests faster than this
- **P95**: 95% of requests faster than this
- **P99**: 99% of requests faster than this

#### Status Code Distribution
- Breakdown of HTTP status codes (200, 201, 400, 500, etc.)

#### Scenario Statistics
- Per-scenario performance metrics
- Success/failure counts
- Individual response times

### Example Results

```
Test: User API Load Test
Users: 50 concurrent
Duration: 60 seconds

Results:
├─ Total Requests: 3,000
├─ Successful: 2,940 (98%)
├─ Failed: 60 (2%)
├─ Requests/Second: 50.0
├─ Avg Response Time: 45.5ms
└─ Percentiles:
   ├─ P50: 42ms
   ├─ P95: 95ms
   └─ P99: 150ms

Scenario Breakdown:
├─ GET /users
│  ├─ Count: 1,500
│  ├─ Success: 1,470
│  ├─ Avg: 40ms
│  └─ Max: 200ms
└─ POST /users
   ├─ Count: 1,500
   ├─ Success: 1,470
   ├─ Avg: 51ms
   └─ Max: 300ms
```

### Use Cases

1. **Capacity Planning**: Determine max load your mocks can handle
2. **Performance Regression**: Track performance over time
3. **SLA Verification**: Ensure response times meet requirements
4. **Bottleneck Identification**: Find slow scenarios

### Integration with Karate Gatling

The tool can generate Gatling simulation code for advanced scenarios:

```scala
package perftest

import com.intuit.karate.gatling.PreDef._
import io.gatling.core.Predef._
import scala.concurrent.duration._

class UsersAPISimulation extends Simulation {

  val protocol = karateProtocol()

  val usersapi = scenario("Users API")
    .exec(karateFeature("classpath:mocks/users.feature"))

  setUp(
    usersapi.inject(
      rampUsers(50) during (10 seconds)
    ).protocols(protocol)
  )
}
```

### Best Practices

1. **Start Small**: Begin with 10 users and gradually increase
2. **Realistic Scenarios**: Use recorded or OpenAPI-generated features
3. **Monitor Resources**: Watch CPU/memory on the server
4. **Baseline First**: Test with 1 user to establish baseline
5. **Iterate**: Adjust test parameters based on results

---

## API Endpoints Reference

### OpenAPI Import

```
POST /api/openapi/import
  - Upload OpenAPI file and convert to feature
  - Form data: file, featureName (optional)

POST /api/openapi/convert
  - Convert OpenAPI content to feature
  - Body: { content, featureName }
```

### Recording Proxy

```
POST /api/recording/sessions
  - Start recording session
  - Body: { sessionId, targetBaseUrl, proxyPort }

DELETE /api/recording/sessions/{sessionId}
  - Stop recording session

GET /api/recording/sessions
  - List all sessions

GET /api/recording/sessions/{sessionId}/recordings
  - Get all recordings for session

POST /api/recording/sessions/{sessionId}/generate
  - Generate feature from recordings
  - Body: { featureName }

* /api/recording/proxy/{sessionId}/**
  - Proxy endpoint for recording traffic
```

### Performance Testing

```
POST /api/performance/tests
  - Create performance test
  - Body: { name, featureFile, users, rampUpSeconds, durationSeconds }

POST /api/performance/tests/{testId}/start
  - Start a test

GET /api/performance/tests
  - List all tests

GET /api/performance/tests/{testId}
  - Get test details and results

DELETE /api/performance/tests/{testId}
  - Delete a test

POST /api/performance/generate-simulation
  - Generate Gatling simulation code
  - Body: { featureFile, simulationName }
```

---

## Troubleshooting

### OpenAPI Import Issues

**Error: "Failed to parse OpenAPI specification"**
- Ensure your file is valid OpenAPI 3.0 (validate at editor.swagger.io)
- Check that JSON/YAML is properly formatted

**Generated feature doesn't work**
- OpenAPI examples may need adjustment for Karate syntax
- Edit the generated feature to add custom logic

### Recording Proxy Issues

**Requests not being recorded**
- Verify session is in ACTIVE status
- Check proxy URL format is correct
- Ensure target API is accessible from server

**SSL/HTTPS errors**
- Recording proxy currently supports HTTP
- For HTTPS APIs, consider using HTTP in development

### Performance Testing Issues

**Test takes too long**
- Reduce user count or duration
- Current implementation is simulated; real Gatling integration would be faster

**High failure rate**
- Check mock server has sufficient resources
- Reduce concurrent users
- Verify feature file scenarios are valid

---

## Next Steps

### Suggested Workflow

1. **Import OpenAPI** → Start with your API specification
2. **Customize Mocks** → Edit generated features in Mock Editor
3. **Record Real API** → Capture actual behavior for comparison
4. **Start Mock Server** → Run your mocks
5. **Performance Test** → Verify mock performance
6. **Monitor** → Use Request Monitor for live traffic inspection

### Advanced Scenarios

- **Combine OpenAPI + Recording**: Import spec, then record real API to enhance with actual examples
- **Continuous Testing**: Automate performance tests in CI/CD
- **Mock Evolution**: Regenerate from OpenAPI as API evolves
- **Multi-Environment**: Record from different environments (staging, prod) for comprehensive mocks

---

## Contributing

Have ideas for Phase 3? Consider:
- **AI-Powered Generation**: Use LLMs to create realistic mock data
- **Chaos Engineering**: Inject latency, errors for resilience testing
- **Mock Versioning**: Track changes to mocks over time
- **Collaborative Features**: Share mocks with teams
- **Advanced Analytics**: Deeper insights into mock usage patterns

See the main README for contribution guidelines.
