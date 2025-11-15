@ignore
Feature: Multipart Form Data Support in Karate Mock

# This feature demonstrates the improved multipart/form-data support in Karate mock server.
# All multipart fields (both text and file uploads) are now accessible via 'requestParts'.

Background:
    * def mockServerUrl = 'http://localhost:8080/'

Scenario: Mock endpoint that handles multipart form with text and file fields
    # Mock server scenario
    # All multipart fields are accessible via requestParts
    Given url mockServerUrl + 'upload'

    # Text field in multipart - accessible as requestParts.fieldName[0].value
    And multipart field username = 'john.doe'
    And multipart field description = 'User profile photo'

    # File field in multipart - accessible as requestParts.fieldName[0]
    And multipart file photo = { read: 'test.pdf.zip', filename: 'profile.jpg', contentType: 'image/jpeg' }

    When method post
    Then status 200
    And match response.username == 'john.doe'
    And match response.description == 'User profile photo'
    And match response.fileName == 'profile.jpg'

Scenario: Access multipart text fields via requestParts
    # Mock server side would have:
    # Scenario: pathMatches('/api/upload')
    #     * def usernamePart = requestParts.username[0]
    #     * def username = usernamePart.value
    #     * def descPart = requestParts.description[0]
    #     * def description = descPart.value
    #     * def photoPart = requestParts.photo[0]
    #     * def fileName = photoPart.filename
    #     * def contentType = photoPart.contentType
    #     * def fileBytes = photoPart.value
    #     * def response = { username: '#(username)', description: '#(description)', fileName: '#(fileName)', fileSize: '#(fileBytes.length)' }

    Given url mockServerUrl + 'api/upload'
    And multipart field username = 'alice'
    And multipart field description = 'Document upload'
    And multipart file photo = { read: 'test.pdf.zip', filename: 'doc.pdf', contentType: 'application/pdf' }
    When method post
    Then status 200

Scenario: Multiple files in multipart request
    # Mock server can handle multiple files with the same field name
    # Scenario: pathMatches('/batch-upload')
    #     * def files = requestParts.documents
    #     * def fileCount = files.length
    #     * def firstFile = files[0].filename
    #     * def secondFile = files[1].filename
    #     * def response = { count: '#(fileCount)', files: ['#(firstFile)', '#(secondFile)'] }

    Given url mockServerUrl + 'batch-upload'
    And multipart file documents = { read: 'test.pdf.zip', filename: 'file1.pdf' }
    And multipart file documents = { read: 'test.xlsx', filename: 'file2.xlsx' }
    When method post
    Then status 200

# Key Benefits:
# 1. Consistent access pattern - all multipart data via requestParts
# 2. Text fields include: name, value, charset
# 3. File fields include: name, value (bytes), filename, contentType, charset, transferEncoding
# 4. Backward compatible - text fields still accessible via requestParams
