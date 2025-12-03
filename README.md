
# GeoBook – Location-Based Book Insight Application

GeoBook is a spatially enhanced full-stack application that visualizes real-world locations mentioned in books.  
It uses **Oracle Spatial (SDO_GEOMETRY)**, **Spring Boot (REST API)**, and a **JavaScript / Leaflet frontend**.

GeoBook allows users to browse books, explore chapters, and view associated locations on an interactive map.  
Multimedia content (imagess, metadata) can also be linked to each location.

---

## 📌 Features

-  Book & chapter management  
-  Spatial locations stored using SDO_GEOMETRY  
-  Multimedia storage (images + metadata + Oracle image objects)  
-  Search for books, chapters, and locations  
-  Interactive Leaflet-based map UI  
-  REST API implemented in Spring Boot  
-  Full Oracle 19c+ database schema  
-  mage content integration using ORDSYS multimedia types  

---

# 📁 Project Structure
    GeoBook/
    │
    ├── source/geobook-app/ # Spring Boot REST API
    │ ├── src/main/
    │ │ └── java/com/geobook/
    │ │ ├── controller/ # REST controllers
    │ │ ├── model/ # JPA entities
    │ │ ├── repository/ # Spring Data JPA repositories
    │ │ └── service/ # Business logic
    │ │
    │ ├── src/main/resources/
    │ │ ├── static/
    │ │ │ ├── css/
    │ │ │ ├── js/
    │ │ │
    │ │ ├── templates/
    │ │ │ └── html/ # Frontend pages
    │ │ │
    │ │ ├── data.sql # Initial data (optional)
    │ │ ├── application.properties
    │ │ ├── application-prod.properties
    │ │
    │ ├── pom.xml # Maven build file
    │ └── README.md



---

# 🧱 Database Schema (Oracle)

### Includes 4 main tables:

- **BOOKS**
- **CHAPTERS**
- **LOCATIONS** (with SDO_GEOMETRY)
- **MULTIMEDIA** (with ORDSYS image objects)

For full SQL, see:  
`DBSchemea/geoBookSql.sql`
---

# 🚀 How to Start the Application

Below is the complete guide for starting the **database**, **backend**, and **frontend**.

---

# 1 Install Requirements

### Software Required:
- Oracle Database 19c or higher  
- Oracle Spatial + ORDSYS image components  
- Java 17+ or 21  
- Maven 3.8+  
- Node.js (optional; not required for simple frontend)  
- Web browser  

---

# 2 Database Setup (Oracle)

### Step 1 — Connect to SQL*Plus or Oracle SQL Developer

### Step 1.1 — Configure Database Connection
 Edit `src/main/resources/application.properties` to set your Oracle DB connection details:
    ```properties
    spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
    spring.datasource.username=your_username
    spring.datasource.password=your_password
    ```
### Step 1.2 — Verify Tables Created
 Check that the tables `BOOKS`, `CHAPTERS`, `LOCATIONS`, and `MULTIMEDIA` are created successfully.
### Step 1.3 — Insert Initial Data
    Run:
    ```sql@src\main\resources/data.sql
    ``` to insert sample data into the tables.  
    ```sql
### Step 1.4 — Verify Data
    SELECT * FROM BOOKS;
    SELECT * FROM CHAPTERS;
    SELECT * FROM LOCATIONS;
    SELECT * FROM MULTIMEDIA;
    ``` 
   
 ### Step 1.5 — Verify Data
    SELECT * FROM BOOKS;
    SELECT * FROM CHAPTERS;
    SELECT * FROM LOCATIONS;
    SELECT * FROM MULTIMEDIA;
```
Run:

```sql
@DBSchemea/geoBookSql.sq
@data.sql
```

### API Endpoints

- `GET /api/books` - Get all books  
- `GET /api/books/{id}` - Get book by ID
- `GET /api/books/{id}/chapters` - Get chapters for a book
- `GET /api/locations` - Get all locations
- `GET /api/locations/{id}` - Get location by ID
- `GET /api/multimedia/{id}` - Get multimedia by ID``` to create the database schema and insert initial data.
to create the database schema and insert initial data.

### Step 3 — Build the Application
```bash
mvn clean install
```
### Step 4 — Run the Application
```bash
mvn spring-boot:run
```
The backend REST API will start on `http://localhost:8080`.
---
