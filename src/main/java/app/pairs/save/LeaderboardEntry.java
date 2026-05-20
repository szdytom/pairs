package app.pairs.save;

import app.pairs.model.*;

public record LeaderboardEntry(
	String username, GameType difficulty, long score, long updatedAt
) {}
