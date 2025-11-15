Feature: Demo API Mock

Background:
  * def users = []
  * def nextId = 1

Scenario: pathMatches('/api/users') && methodIs('get')
  * def response = users
  * def responseStatus = 200

Scenario: pathMatches('/api/users') && methodIs('post')
  * def user = request
  * set user.id = nextId
  * eval nextId = nextId + 1
  * eval users.add(user)
  * def response = user
  * def responseStatus = 201

Scenario: pathMatches('/api/users/{id}') && methodIs('get')
  * def id = parseInt(pathParams.id)
  * def user = users.find(u => u.id == id)
  * def responseStatus = user ? 200 : 404
  * def response = user || { error: 'User not found' }

Scenario: pathMatches('/api/users/{id}') && methodIs('put')
  * def id = parseInt(pathParams.id)
  * def index = users.findIndex(u => u.id == id)
  * if (index >= 0) karate.set('users[' + index + ']', request)
  * def user = index >= 0 ? users[index] : null
  * def responseStatus = user ? 200 : 404
  * def response = user || { error: 'User not found' }

Scenario: pathMatches('/api/users/{id}') && methodIs('delete')
  * def id = parseInt(pathParams.id)
  * def index = users.findIndex(u => u.id == id)
  * if (index >= 0) users.splice(index, 1)
  * def responseStatus = index >= 0 ? 204 : 404
  * def response = index >= 0 ? '' : { error: 'User not found' }

Scenario: pathMatches('/api/health')
  * def response = { status: 'healthy', timestamp: new Date().toISOString() }
  * def responseStatus = 200

Scenario:
  * def responseStatus = 404
  * def response = { error: 'Not found', path: requestPath }
