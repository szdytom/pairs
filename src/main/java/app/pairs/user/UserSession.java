package app.pairs.user;

// Singleton holding the currently logged-in user (or a NullUser for guests).
public class UserSession {
	private static final UserSession INSTANCE = new UserSession();

	private User current = new NullUser();

	private UserSession() {}

	public static UserSession instance() {
		return INSTANCE;
	}

	public User getUser() {
		return current;
	}

	public void setUser(User user) {
		this.current = user;
	}
}
