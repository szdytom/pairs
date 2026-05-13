package app.pairs.user;

import app.pairs.save.Database;
import app.pairs.save.Users;

import java.util.Optional;

public class UserManager {
	// Login if credentials match, register if new user. Returns empty on wrong
	// password.
	public static Optional<RealUser> loginOrRegister(
		String username, String password
	) {
		Users users = Database.instance().users();
		if (users.containsUser(username)) {
			return users.checkPassword(username, password)
				? Optional.of(new RealUser(username))
				: Optional.empty();
		}
		users.createUser(username, password);
		return Optional.of(new RealUser(username));
	}
}
