# Google Places API Integration - Backend Endpoints

This document describes the REST API endpoints for Google Places integration in the SiW Trips application.

## Configuration

### API Key Setup
1. Get a Google Maps API key from [Google Cloud Console](https://console.developers.google.com/)
2. Enable the following APIs:
   - Places API
   - Maps JavaScript API
3. Add the API key to `application.properties`:
   ```properties
   google.maps.api.key=YOUR_GOOGLE_MAPS_API_KEY_HERE
   ```

## REST API Endpoints

### Base URL
All endpoints are prefixed with `/api/places`

### 1. Search Places by Text

**Endpoint:** `GET /api/places/search`

**Description:** Search for places using a text query

**Parameters:**
- `q` (required): Search query (e.g., "Eiffel Tower", "restaurants in Paris")
- `lat` (optional): Latitude for location-biased results
- `lng` (optional): Longitude for location-biased results
- `radius` (optional): Search radius in meters (default: 5000)
- `type` (optional): Place type filter (e.g., "tourist_attraction", "restaurant")
- `lang` (optional): Language code (default: "en")

**Example Request:**
```bash
GET /api/places/search?q=eiffel tower&lat=48.8566&lng=2.3522&radius=5000&type=tourist_attraction&lang=en
```

**Response:**
```json
{
  "success": true,
  "count": 5,
  "places": [
    {
      "placeId": "ChIJLU7jZClu5kcR4PcOOO6p3I0",
      "name": "Eiffel Tower",
      "formattedAddress": "Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France",
      "latitude": 48.8583701,
      "longitude": 2.2944813,
      "city": "Paris",
      "country": "France",
      "rating": 4.6,
      "types": ["tourist_attraction", "point_of_interest"],
      "vicinity": "Paris",
      "photoReference": "ATtYBwI..."
    }
  ]
}
```

### 2. Get Place Details

**Endpoint:** `GET /api/places/{placeId}/details`

**Description:** Get detailed information about a specific place

**Parameters:**
- `placeId` (path): Google Places ID

**Example Request:**
```bash
GET /api/places/ChIJLU7jZClu5kcR4PcOOO6p3I0/details
```

**Response:**
```json
{
  "success": true,
  "place": {
    "placeId": "ChIJLU7jZClu5kcR4PcOOO6p3I0",
    "name": "Eiffel Tower",
    "formattedAddress": "Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France",
    "latitude": 48.8583701,
    "longitude": 2.2944813,
    "city": "Paris",
    "country": "France",
    "rating": 4.6,
    "types": ["tourist_attraction", "point_of_interest"],
    "vicinity": "Paris",
    "photoReference": "ATtYBwI..."
  }
}
```

### 3. Search Nearby Places

**Endpoint:** `GET /api/places/nearby`

**Description:** Search for places near a specific location

**Parameters:**
- `lat` (required): Latitude
- `lng` (required): Longitude
- `radius` (optional): Search radius in meters (default: 5000)
- `type` (optional): Place type filter

**Example Request:**
```bash
GET /api/places/nearby?lat=48.8566&lng=2.3522&radius=1000&type=restaurant
```

**Response:**
```json
{
  "success": true,
  "count": 10,
  "places": [
    {
      "placeId": "ChIJ...",
      "name": "Restaurant Name",
      "formattedAddress": "Address",
      "latitude": 48.8566,
      "longitude": 2.3522,
      "city": "Paris",
      "country": "France",
      "rating": 4.2,
      "types": ["restaurant", "food"],
      "vicinity": "Paris"
    }
  ]
}
```

### 4. Create Location from Google Place

**Endpoint:** `POST /api/places/create-location`

**Description:** Create a location in your trip from a Google Place

**Authentication:** Required (session-based)

**Request Body:**
```json
{
  "placeId": "ChIJLU7jZClu5kcR4PcOOO6p3I0",
  "tripId": 123,
  "tripDayId": 456,
  "description": "Optional location description"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Location created successfully from Google Place",
  "location": {
    "id": 789,
    "name": "Eiffel Tower",
    "address": "Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France",
    "latitude": 48.8583701,
    "longitude": 2.2944813,
    "city": "Paris",
    "country": "France",
    "description": "Optional location description"
  },
  "visitCreationUrl": "/trips/123/days/456/visits/new?locationId=789"
}
```

### 5. Check API Status

**Endpoint:** `GET /api/places/status`

**Description:** Check if Google Places API is configured

**Response:**
```json
{
  "configured": true,
  "service": "Google Places API"
}
```

## Error Responses

All endpoints may return error responses in the following format:

```json
{
  "success": false,
  "error": "Error message description"
}
```

**Common HTTP Status Codes:**
- `200 OK`: Success
- `400 Bad Request`: Invalid parameters
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Not authorized to modify trip
- `404 Not Found`: Resource not found
- `503 Service Unavailable`: Google Places API not configured

## Existing Location Management Endpoints

The application also provides standard location management endpoints:

### Get All Locations
```bash
GET /api/locations
```

### Get Location by ID
```bash
GET /api/locations/{id}
```

### Search Locations by Name
```bash
GET /api/locations/search/name?name=tower
```

### Search Locations by City
```bash
GET /api/locations/search/city?city=Paris
```

### Search Locations by Country
```bash
GET /api/locations/search/country?country=France
```

### Search Nearby Locations (existing in database)
```bash
GET /api/locations/search/nearby?latitude=48.8566&longitude=2.3522&radius=5.0
```

### Create Location Manually
```bash
POST /api/locations
Content-Type: application/json

{
  "name": "Location Name",
  "address": "Address",
  "city": "City",
  "country": "Country",
  "latitude": 48.8566,
  "longitude": 2.3522,
  "description": "Description"
}
```

### Create Location from Google Place (alternative endpoint)
```bash
POST /api/locations/from-google-place
Content-Type: application/json

{
  "placeId": "ChIJ...",
  "name": "Place Name",
  "formattedAddress": "Address",
  "latitude": 48.8566,
  "longitude": 2.3522,
  "city": "City",
  "country": "Country"
}
```

## Usage Examples

### 1. Search and Create Location Workflow

```javascript
// 1. Search for places
const searchResponse = await fetch('/api/places/search?q=colosseum rome');
const searchData = await searchResponse.json();

// 2. Get detailed information
const placeId = searchData.places[0].placeId;
const detailsResponse = await fetch(`/api/places/${placeId}/details`);
const detailsData = await detailsResponse.json();

// 3. Create location in trip
const createResponse = await fetch('/api/places/create-location', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    placeId: placeId,
    tripId: 123,
    tripDayId: 456,
    description: 'Historic amphitheater'
  })
});
const createData = await createResponse.json();

// 4. Redirect to visit creation
window.location.href = createData.visitCreationUrl;
```

### 2. Nearby Places Discovery

```javascript
// Get user's current location
navigator.geolocation.getCurrentPosition(async (position) => {
  const lat = position.coords.latitude;
  const lng = position.coords.longitude;
  
  // Find nearby restaurants
  const response = await fetch(
    `/api/places/nearby?lat=${lat}&lng=${lng}&radius=1000&type=restaurant`
  );
  const data = await response.json();
  
  // Display results
  data.places.forEach(place => {
    console.log(`${place.name} - ${place.rating} stars`);
  });
});
```

## Place Types

Common place types you can use in the `type` parameter:

- `tourist_attraction`
- `restaurant`
- `lodging`
- `museum`
- `park`
- `shopping_mall`
- `gas_station`
- `hospital`
- `pharmacy`
- `bank`
- `atm`
- `subway_station`
- `bus_station`
- `airport`

For a complete list, see the [Google Places API documentation](https://developers.google.com/maps/documentation/places/web-service/supported_types).

## Rate Limits and Quotas

Google Places API has usage quotas and rate limits. Monitor your usage in the Google Cloud Console to avoid service interruptions.

## Security Notes

- The API key is server-side only and not exposed to client applications
- Cross-Origin Resource Sharing (CORS) is configured - adjust as needed for production
- Authentication is required for location creation endpoints
- Users can only modify trips they are administrators of
