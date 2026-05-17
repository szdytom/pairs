package app.pairs.user;

import app.pairs.save.Database;
import app.pairs.save.Users;

import java.util.Optional;

public class UserManager {
	public static Optional<User> login(String username, String password) {
		Users users = Database.instance().users();
		if (!users.containsUser(username)) {
			return Optional.empty();
		}
		return users.checkPassword(username, password)
			? Optional.of(new RealUser(username))
			: Optional.empty();
	}

	public static Optional<User> register(String username, String password) {
		Users users = Database.instance().users();
		if (users.containsUser(username)) {
			return Optional.empty();
		}
		users.createUser(username, password);
		return Optional.of(new RealUser(username));
	}

	public static boolean changePassword(
		String username, String oldPassword, String newPassword
	) {
		Users users = Database.instance().users();
		if (!users.checkPassword(username, oldPassword)) {
			return false;
		}
		users.changePassword(username, newPassword);
		return true;
	}
}
