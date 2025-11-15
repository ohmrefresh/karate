Feature: Products API Mock

Background:
  * def products =
    """
    [
      { id: 1, name: 'Laptop', price: 999.99, category: 'Electronics' },
      { id: 2, name: 'Phone', price: 599.99, category: 'Electronics' },
      { id: 3, name: 'Desk', price: 299.99, category: 'Furniture' }
    ]
    """

Scenario: pathMatches('/api/products') && methodIs('get')
  * def category = paramValue('category')
  * def filtered = category ? products.filter(p => p.category == category) : products
  * def response = filtered
  * def responseStatus = 200

Scenario: pathMatches('/api/products/{id}') && methodIs('get')
  * def id = parseInt(pathParams.id)
  * def product = products.find(p => p.id == id)
  * def responseStatus = product ? 200 : 404
  * def response = product || { error: 'Product not found' }

Scenario: pathMatches('/api/products/search') && methodIs('get')
  * def query = paramValue('q')
  * def results = query ? products.filter(p => p.name.toLowerCase().includes(query.toLowerCase())) : []
  * def response = results
  * def responseStatus = 200
