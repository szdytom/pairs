package app.pairs;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		Rectangle rectangle = new Rectangle(50, 50, 200, 100);
		rectangle.setFill(Color.LIGHTBLUE);
		rectangle.setStroke(Color.BLUE);
		rectangle.setStrokeWidth(3);

		Pane root = new Pane(rectangle);
		Scene scene = new Scene(root, 400, 300);

		primaryStage.setTitle("P.A.I.R.S.");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
