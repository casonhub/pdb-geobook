# GeoBook – Location-Based Book Insight Application

**GeoBook** is a spatially enhanced full-stack web application that visualizes real-world locations mentioned in books.  
It uses **Spring Boot (REST API)**, **Oracle Spatial (SDO_GEOMETRY)**, and a **JavaScript / Leaflet frontend**.

Users can browse books, explore chapters, and view associated locations on an interactive map. Multimedia content (images, metadata) can also be linked to each location.

---

##  Features

- User authentication and authorization
- Book & chapter management  
- Full CRUD operations for books and chapters  
- Search for books by title  
- Interactive map displaying geographical locations using **Leaflet JS**  
- Spatial locations stored using **SDO_GEOMETRY**  
- Multimedia storage (images + metadata using Oracle ORDSYS image objects)  
- Support for **H2 in-memory database** (development) and **Oracle database** (production)  
- Thymeleaf templates for responsive UI  
- REST API implemented in Spring Boot  
- Sample data seeding for testing  

---

##  Project Structure


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

#  Database Schema (Oracle)

### Includes 4 main tables:

- **BOOKS**
- **CHAPTERS**
- **LOCATIONS** (with SDO_GEOMETRY)
- **MULTIMEDIA** (with ORDSYS image objects)

For full SQL, see:  
`DBSchemea/geoBookSql.sql`
---

#  How to Start the Application

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
        
application.properties
        
        spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
        spring.datasource.username=your_username
        spring.datasource.password=your_password
        
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

## API Endpoints

- `GET /`: Home page
- `GET /books`: List books (with optional search param `?search=title`)
- `GET /books/add`: Add new book form
- `POST /books/add`: Create book
- `GET /books/{id}`: View book details
- `GET /books/{id}/edit`: Edit book form
- `POST /books/{id}/edit`: Update book
- `POST /books/{id}/delete`: Delete book
- `GET /books/{bookId}/chapters`: List chapters for a book
- `GET /books/{bookId}/chapters/add`: Add new chapter form
- `POST /books/{bookId}/chapters/add`: Create chapter
- `GET /books/{bookId}/chapters/{chapterId}/edit`: Edit chapter form
- `POST /books/{bookId}/chapters/{chapterId}/edit`: Update chapter
- `POST /books/{bookId}/chapters/{chapterId}/delete`: Delete chapter
- `GET /map`: View locations on map
- `GET /login`: Login page
- `GET /register`: Registration page

### Step 3 — Build the Application
- Ensure the Oracle JDBC driver is available (included in `pom.xml` as runtime dependency).

### 3. Build the Application

```bash
mvn clean compile
```

To package into a JAR:

```bash
mvn package -DskipTests
```

## Running the Application

### Development Mode

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/geobook-app-1.0-SNAPSHOT.jar
```

### Production Mode

```bash
java -jar target/geobook-app-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

The backend REST API will start on `http://localhost:8080`.
