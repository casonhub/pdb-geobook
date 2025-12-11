# GeoBook – Location-Based Book Insight Application

GeoBook is a spatially enhanced full-stack application that visualizes real-world locations mentioned in books.
It uses **Oracle Spatial (SDO_GEOMETRY)**, **Spring Boot (REST API)**, and a **JavaScript / Leaflet frontend**.

GeoBook allows users to browse books, explore chapters, and view associated locations on an interactive map.
Multimedia content (images, metadata) can also be linked to each location. The application features advanced spatial entity management with 5 geometry types (POINT, LINESTRING, POLYGON, CIRCLE, RECTANGLE) and spatial analysis capabilities.

---

## 📌 Features

- **Book & Chapter Management**: Full CRUD operations for books and chapters
- **Spatial Locations**: Store and visualize locations using Oracle SDO_GEOMETRY
- **Spatial Entities**: Create and manage 5 types of geometric entities (POINT, LINESTRING, POLYGON, CIRCLE, RECTANGLE)
- **Multimedia Storage**: Images and metadata using Oracle ORDSYS multimedia types
- **Interactive Map UI**: Leaflet-based map with spatial entity visualization
- **Spatial Analysis**: Intersection analysis, density analysis, and spatial queries
- **Search Functionality**: Search books, chapters, locations, and spatial entities
- **REST API**: Spring Boot REST API with JSON responses
- **User Authentication**: Secure login and registration system
- **Full Oracle 19c+ Database Schema**: Complete spatial database implementation

---

# 📁 Project Structure
    GeoBook/
    │
    ├── geobook-app/ # Spring Boot REST API
    │ ├── src/main/
    │ │ └── java/com/geobook/
    │ │ ├── controller/ # REST controllers (Book, Chapter, Location, Map, Multimedia, Auth)
    │ │ ├── model/ # JPA entities (Book, Chapter, Location, SpatialEntity, Multimedia, User)
    │ │ ├── repository/ # Spring Data JPA repositories
    │ │ └── service/ # Business logic and spatial services
    │ │
    │ ├── src/main/resources/
    │ │ ├── static/
    │ │ │ ├── css/
    │ │ │ ├── js/
    │ │ │ └── images/
    │ │ │
    │ │ ├── templates/
    │ │ │ └── *.html # Thymeleaf frontend templates
    │ │ │
    │ │ ├── application.properties
    │ │ ├── application-prod.properties
    │ │ ├── data.sql # Initial data
    │ │ └── data-oracle.sql
    │ │
    │ ├── pom.xml # Maven build file
    │ └── libs/ # Oracle JDBC drivers
    │
    ├── DBschema/
    │ ├── geoBookSql.sql # Complete database schema
    │ └── dataset.txt
    │
    └── README.md

---

# 🧱 Database Schema (Oracle)

### Main Tables:

- **BOOKS**: Book information and metadata
- **CHAPTERS**: Chapter details linked to books
- **LOCATIONS**: Geographic locations with SDO_GEOMETRY
- **MULTIMEDIA**: Images and media using ORDSYS objects
- **SPATIAL_ENTITIES**: Advanced geometric entities (POINT, LINESTRING, POLYGON, CIRCLE, RECTANGLE)
- **USERS**: User authentication and authorization

### Spatial Features:
- Oracle Spatial SDO_GEOMETRY for all spatial data
- Spatial indexes for performance
- Custom spatial functions and operators
- Support for 5 geometry types with full CRUD operations

For full SQL schema, see: `DBschema/geoBookSql.sql`

---

# 🚀 How to Start the Application

Below is the complete guide for starting the **database**, **backend**, and **frontend**.

---

# 1 Install Requirements

### Software Required:
- **Oracle Database 19c or higher** with Spatial and Multimedia components
- **Java 17+ or 21**
- **Maven 3.8+**
- **Web browser** (Chrome, Firefox, Safari, Edge)
- Node.js (optional; not required for basic frontend functionality)

---

# 2 Database Setup (Oracle)

### Step 1 — Connect to SQL*Plus or Oracle SQL Developer

### Step 1.1 — Configure Database Connection
Edit `geobook-app/src/main/resources/application.properties` to set your Oracle DB connection details:
```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
```

### Step 1.2 — Create Database Schema
Run the SQL script to create all tables, indexes, and initial data:
```sql
@DBschema/geoBookSql.sql
```

### Step 1.3 — Verify Tables Created
Check that all tables are created successfully:
```sql
SELECT table_name FROM user_tables;
```

Expected tables: BOOKS, CHAPTERS, LOCATIONS, MULTIMEDIA, SPATIAL_ENTITIES, USERS

### Step 1.4 — Verify Spatial Components
Ensure Oracle Spatial is properly installed:
```sql
SELECT * FROM v$option WHERE parameter = 'Spatial';
```

---

# 3 Build and Run the Application

### Step 1 — Build the Application
```bash
cd geobook-app
mvn clean install
```

### Step 2 — Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

### Step 3 — Access the Application
- **Main Application**: http://localhost:8080
- **Login Page**: http://localhost:8080/login
- **Map View**: http://localhost:8080/map
- **Books**: http://localhost:8080/books

---

# 📡 API Endpoints

### Books
- `GET /books` - List all books (with optional search)
- `GET /books/{id}` - Get book details
- `GET /books/add` - Show add book form
- `POST /books/add` - Create new book
- `GET /books/{id}/edit` - Show edit book form
- `POST /books/{id}/edit` - Update book
- `POST /books/{id}/delete` - Delete book

### Chapters
- `GET /chapters` - List all chapters
- `GET /chapters/{id}` - Get chapter details
- `GET /chapters/add` - Show add chapter form
- `POST /chapters/add` - Create new chapter
- `GET /chapters/{id}/edit` - Show edit chapter form
- `POST /chapters/{id}/edit` - Update chapter
- `POST /chapters/{id}/delete` - Delete chapter

### Locations
- `GET /locations` - List all locations
- `GET /locations/{id}` - Get location details
- `GET /locations/add` - Show add location form
- `POST /locations/add` - Create new location
- `GET /locations/{id}/edit` - Show edit location form
- `POST /locations/{id}/edit` - Update location
- `POST /locations/{id}/delete` - Delete location

### Spatial Entities
- `GET /map/entities` - Get all spatial entities
- `GET /map/entities/{id}` - Get spatial entity by ID
- `GET /map/entities/type/{type}` - Get entities by type
- `POST /map/entities` - Create new spatial entity
- `PUT /map/entities/{id}` - Update spatial entity
- `DELETE /map/entities/{id}` - Delete spatial entity

### Map & Spatial Analysis
- `GET /map` - Show interactive map
- `POST /map/search` - Search locations within distance
- `POST /map/spatial/intersections` - Find spatial intersections
- `GET /map/analysis` - Show spatial analysis dashboard

### Multimedia
- `GET /multimedia` - List all multimedia
- `GET /multimedia/{id}` - Get multimedia details
- `GET /multimedia/add` - Show add multimedia form
- `POST /multimedia/add` - Create new multimedia
- `GET /multimedia/{id}/edit` - Show edit multimedia form
- `POST /multimedia/{id}/edit` - Update multimedia
- `POST /multimedia/{id}/delete` - Delete multimedia

### Authentication
- `GET /login` - Login page
- `POST /login` - Process login
- `GET /register` - Registration page
- `POST /register` - Process registration
- `POST /logout` - Logout

---

# 🔧 Configuration

### Application Properties
Key configuration options in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
spring.datasource.username=your_username
spring.datasource.password=your_password

# Server
server.port=8080

# Security
spring.security.user.name=admin
spring.security.user.password=admin

# Thymeleaf
spring.thymeleaf.cache=false
```

### Oracle Spatial Setup
Ensure your Oracle database has:
- Oracle Spatial installed and configured
- ORDSYS multimedia components
- Proper user privileges for spatial operations

---

# 🐛 Troubleshooting

### Common Issues:

1. **Oracle JDBC Driver**: Ensure `ojdbc8.jar` and other Oracle drivers are in the `libs/` folder
2. **Spatial Functions**: If spatial queries fail, check Oracle Spatial installation
3. **Port Conflicts**: Change `server.port` if 8080 is in use
4. **Database Connection**: Verify connection string and credentials

### Database Verification:
```sql
-- Check spatial components
SELECT * FROM mdsys.sdo_version;

-- Test spatial functions
SELECT SDO_GEOM.SDO_AREA(SDO_GEOMETRY(2003, 8307, NULL, SDO_ELEM_INFO_ARRAY(1,1003,3), SDO_ORDINATE_ARRAY(0,0,1,1)), 0.005) FROM dual;
```

---

# 📝 Development Notes

- **Spatial Entities**: Support 5 geometry types with automatic geometry generation
- **Map Integration**: Leaflet.js with custom spatial entity rendering
- **Security**: Spring Security with CSRF protection
- **Database**: Full Oracle Spatial integration with custom functions
- **Frontend**: Thymeleaf templates with Bootstrap styling

For more details, see the source code and database schema files.
