# OpenLibrary API Specification for op_client

## Overview
This document details the API specifications for the op_client Kotlin Multiplatform application, which integrates with all OpenLibrary APIs.

## API Endpoints

### 1. Search API
**Base URL**: https://openlibrary.org/search.json

**Functionality**:
- Search books by title, author, ISBN, or subject
- Filter results with various parameters
- Return detailed book metadata including covers

**Key Features to Implement**:
- Text search with query parameter
- Field selection for optimized responses
- Sorting options (relevance, publication date, etc.)
- Pagination support
- Edition-level data inclusion

### 2. Books API
**Individual Works**: https://openlibrary.org/works/{OLID}.json
**Individual Editions**: https://openlibrary.org/books/{OLID}.json

**Functionality**:
- Retrieve complete metadata for specific books
- Access detailed edition information
- Get associated authors and subjects

### 3. Authors API
**Search**: https://openlibrary.org/search/authors.json
**Individual Authors**: https://openlibrary.org/authors/{OLID}.json
**Author Works**: https://openlibrary.org/authors/{OLID}/works.json

**Functionality**:
- Search authors by name
- Retrieve detailed author profiles
- Access complete bibliographies
- Get author photos via Covers API

### 4. Subjects API
**Base URL**: https://openlibrary.org/subjects/{subject}.json

**Functionality**:
- Browse books organized by subject categories
- Access related subjects and popular themes
- View publishing history trends
- Get associated authors and publishers

### 5. Covers API
**Base URL**: https://covers.openlibrary.org/

**Functionality**:
- Fetch book covers by ISBN, OLID, or Cover ID
- Retrieve author photos
- Multiple image sizes (Small, Medium, Large)
- Graceful handling of missing images

### 6. Lists API
**Various Endpoints**:
- User Lists: https://openlibrary.org/people/{username}/lists.json
- List Seeds: https://openlibrary.org/people/{username}/lists/{list_id}/seeds.json
- List Creation: POST to https://openlibrary.org/people/{username}/lists

**Functionality**:
- Create and manage personal reading lists
- Add/remove books from lists
- Browse public lists
- Search lists by keyword

### 7. Search Inside API
**Base URL**: https://{datanode}/fulltext/inside.php

**Functionality**:
- Search full text content of books
- Get page-specific results with highlights
- Access passage locations for navigation

### 8. My Books API
**Endpoints**:
- Want to Read: https://openlibrary.org/people/{username}/books/want-to-read.json
- Currently Reading: https://openlibrary.org/people/{username}/books/currently-reading.json
- Already Read: https://openlibrary.org/people/{username}/books/already-read.json

**Functionality**:
- Access personal reading logs
- Track reading progress
- Manage reading goals

### 9. Recent Changes API
**Base URL**: https://openlibrary.org/recentchanges.json

**Functionality**:
- Monitor new additions to the library
- Track edits and updates
- Filter by date ranges and change types

## Integration Requirements

### Rate Limiting Compliance
- 1 request/second for default usage
- 3 requests/second with proper identification
- Implement exponential backoff strategy

### Caching Strategy
- Cache static data (author info, book metadata)
- Implement smart cache invalidation
- Support offline access to cached content

### Error Handling
- Network error recovery
- API downtime resilience
- Graceful degradation for experimental APIs

## Data Models

### Book Model
- Title, subtitle
- Authors with IDs
- ISBN identifiers
- Publication details
- Subject classifications
- Cover image URLs
- Description/synopsis

### Author Model
- Name and aliases
- Biography information
- Birth/death dates
- Photo URL
- Work count and top works
- Subject specializations

### Subject Model
- Name and description
- Related subjects
- Prominent authors
- Key publishers
- Publishing history
- Work count

### List Model
- Name and description
- Creation/modification dates
- Book count
- Privacy settings
- Collaborators/members

## Implementation Roadmap

1. Core Search and Discovery (Weeks 1-2)
2. Books and Authors Detail Views (Weeks 3-4)
3. Personal Collections and Lists (Weeks 5-6)
4. Advanced Features and Polish (Weeks 7-8)
