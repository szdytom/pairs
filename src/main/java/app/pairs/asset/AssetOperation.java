package app.pairs.asset;

public interface AssetOperation {
	String type();
	void process(Context ctx) throws Exception;

	public static interface Context {
		String id();
		AssetLoader loader();
		Registry registry();
		void put(String id, Object asset);
		<T> T getInput(String ref) throws Exception;
	}

	public static interface Registry {
		void register(String type, OperationFactory factory);
		OperationFactory get(String type);
	}
}
