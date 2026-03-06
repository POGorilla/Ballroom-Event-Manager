package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("USER");
    private static final String PASS = System.getenv("PASS");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }
}