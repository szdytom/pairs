package app.pairs.user;

// Singleton holding the currently logged-in user (or a NullUser for guests).
public class UserSession {
	private static final UserSession INSTANCE = new UserSession();

	private User current = new NullUser();
	private Long activeSaveId = null;

	private UserSession() {}

	public static UserSession instance() {
		return INSTANCE;
	}

	public User getUser() {
		return current;
	}

	public void setUser(User user) {
		this.current = user;
		this.activeSaveId = null;
	}

	public Long getActiveSaveId() {
		return activeSaveId;
	}

	public void setActiveSaveId(Long id) {
		this.activeSaveId = id;
	}

	public boolean isAuthorized() {
		return current.isAuthorized();
	}
}
