# GeoBook Application

A Spring Boot web application for managing books with geographical locations. Users can perform CRUD operations on books, view chapters and locations, and visualize locations on an interactive map.

## Features

- User authentication and authorization
- Full CRUD operations for books (Create, Read, Update, Delete)
- Search books by title
- Interactive map displaying geographical locations using Leaflet JS
- Support for H2 in-memory database (development) and Oracle database (production)
- Thymeleaf templates for responsive UI
- Sample data seeding for testing

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- (For production) Oracle Database 11g or higher with JDBC driver

## Installation and Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd geobook-app
```

Replace `<repository-url>` with the actual Git repository URL.

### 2. Database Configuration

#### Development (H2 In-Memory)
No additional setup required. The application uses H2 in-memory database by default, with sample data loaded from `src/main/resources/data.sql`.

#### Production (Oracle Database)
- Install and configure Oracle Database.
- Update `src/main/resources/application-prod.properties` with your Oracle connection details:

```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
spring.datasource.username=your_username
spring.datasource.password=your_password
```

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

The application will start on `http://localhost:8080`.

## Usage

1. Open your browser and navigate to `http://localhost:8080`.
2. Log in with default credentials: `user` / `password` (in-memory authentication).
3. Navigate through the app:
   - **Home**: Overview and navigation links.
   - **Books**: List, search, add, edit, and delete books.
   - **Map**: View locations on an interactive map.
4. For production, register or use configured users.

## Project Structure

- `src/main/java/com/geobook/`: Java source files (entities, controllers, repositories, services)
- `src/main/resources/templates/`: Thymeleaf HTML templates
- `src/main/resources/static/`: Static assets (CSS, JS)
- `src/main/resources/application.properties`: Default configuration
- `src/main/resources/application-prod.properties`: Production configuration
- `src/main/resources/data.sql`: Sample data for H2

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

## Testing

Run tests with:

```bash
mvn test
```

## Contributing

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Push to the branch.
5. Open a Pull Request.

## License

This project is licensed under the MIT License.
