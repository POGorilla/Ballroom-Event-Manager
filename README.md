# Ballroom Event Manager

This is a comprehensive desktop application developed using JavaFX for managing events at a ballroom or event venue. The application provides a complete solution for handling events, clients, employees, services, and generating insightful reports.

## Features

*   **Dashboard**: A central hub providing key performance indicators (KPIs) such as active events, events in the current month, and total clients. It also features a revenue chart for the last 12 months and a list of upcoming events.

*   **Event Management**:
    *   CRUD (Create, Read, Update, Delete) operations for events.
    *   Advanced filtering capabilities to search events by name, client, type (Wedding, Corporate, etc.), and date.
    *   Assign employees and services to specific events.

*   **Admin Panel**: A secure, password-protected section for administrative tasks.
    *   **Employee Management**: Add, edit, and delete employee records, including assigning supervisors.
    *   **User Management**: Manage application user accounts, link them to employees, and set roles (admin, manager, etc.).
    *   **Venue Management**: Manage the ballroom's amenties, including halls/rooms, capacity, and pricing.
    *   **Service Management**: Add, edit, and delete services offered by the venue.
    *   **Access Logs**: Monitor successful and failed login attempts.

*   **Reporting**: Generate dynamic reports based on the data from the selected year, including:
    *   Top clients by total spending.
    *   Revenue generated per event hall.
    *   Most popular services.
    *   Employees with the highest and lowest number of assigned events.
    *   Recurring clients from the previous year.

*   **Client & Contact Management**:
    *   Maintain a database of clients.
    *   Quickly access contact lists for clients and supervisors with options to call or email directly from the application.

*   **Security**:
    *   User authentication with password hashing using jBCrypt.
    *   Secondary password verification for accessing the sensitive Admin Panel.

## Technology Stack

*   **Frontend**: JavaFX
*   **Backend**: Java 21
*   **Database**: MySQL
*   **Build Tool**: Apache Maven
*   **Key Libraries**:
    *   OpenJFX
    *   MySQL Connector/J
    *   jBCrypt for password hashing

## Getting Started

### Prerequisites

*   **Java Development Kit (JDK)**: Version 21 or newer.
*   **Apache Maven**: To build and run the project.
*   **MySQL Server**: A running instance of a MySQL database.

### Database Setup

1.  Create a MySQL database for the application. The full schema can be inferred from the SQL queries within the controller classes (e.g., `src/main/java/controller/`).
2.  Set the following environment variables to configure the database connection, as required by `src/main/java/database/DatabaseManager.java`:
    *   `DB_URL`: The JDBC URL for your database (e.g., `jdbc:mysql://localhost:3306/your_database_name`).
    *   `USER`: Your MySQL database username.
    *   `PASS`: Your MySQL database password.

### Installation and Running

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/pogorilla/ballroom-event-manager.git
    cd ballroom-event-manager
    ```

2.  **Build the project using Maven:**
    ```sh
    mvn clean install
    ```

3.  **Run the application:**
    ```sh
    mvn javafx:run
    ```

## Usage

### Initial User Setup

The application uses hashed passwords for security. To create your first user (e.g., an administrator), you need to generate a password hash.

1.  Run the `GenereazaHash.java` utility file provided in `src/main/java/`. This will print a hashed password to the console.
2.  Insert a new user record directly into your `Utilizatori` (Users) table in the database, using the username of your choice and the generated hash from the previous step.

### Logging In

Launch the application and use the credentials you created to log in.

### Navigation

The main application window features a navigation pane on the left to switch between different modules:
*   **Dashboard**: The main landing page with an overview of operations.
*   **Evenimente (Events)**: The main table for managing all events.
*   **Rapoarte (Reports)**: The reporting module.
*   **Admin Panel**: Access to user, employee, and venue management (requires password re-entry).
