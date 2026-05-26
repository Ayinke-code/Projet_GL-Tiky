package fr.horizon.backend_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class BackendApiApplication {

	public static void main(String[] args) {

		Dotenv dotenv = Dotenv.load();

		// Database
		System.setProperty("DB_HOST", dotenv.get("DB_HOST"));
		System.setProperty("DB_PORT", dotenv.get("DB_PORT"));
		System.setProperty("DB_NAME", dotenv.get("DB_NAME"));
		System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
		System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));

		// Server
		System.setProperty("SERVER_PORT", dotenv.get("SERVER_PORT"));
		System.setProperty("SERVER_CONTEXT_PATH", dotenv.get("SERVER_CONTEXT_PATH"));

		// JPA
		System.setProperty("JPA_SHOW_SQL", dotenv.get("JPA_SHOW_SQL"));
		System.setProperty("JPA_DDL_AUTO", dotenv.get("JPA_DDL_AUTO"));

		// Error handling
		System.setProperty("ERROR_INCLUDE_MESSAGE", dotenv.get("ERROR_INCLUDE_MESSAGE"));

		// App config
		System.setProperty("APP_NAME", dotenv.get("APP_NAME"));

		// JWT
		System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));
		System.setProperty("JWT_EXPIRATION", dotenv.get("JWT_EXPIRATION"));

		// Default user
		System.setProperty("DEFAULT_PASSWORD", dotenv.get("DEFAULT_PASSWORD"));

		// Frontend url (Selenium)
		System.setProperty("FRONT_URL", dotenv.get("FRONT_URL"));

		SpringApplication.run(BackendApiApplication.class, args);
	}

}
