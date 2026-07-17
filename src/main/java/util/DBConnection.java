package util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

	// ✅ Plus d'identifiants codés en dur : lus via AppConfig
	//    (variable d'environnement DB_URL/DB_USER/DB_PASSWORD ou config.properties,
	//    avec les anciennes valeurs comme valeurs par défaut pour ne rien casser).
	private static Connection connection;

	private DBConnection() throws ClassNotFoundException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connection = DriverManager.getConnection(
				AppConfig.getDbUrl(),
				AppConfig.getDbUser(),
				AppConfig.getDbPassword()
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection() throws ClassNotFoundException {
		if (connection == null) new DBConnection();
		return connection;
	}
}
