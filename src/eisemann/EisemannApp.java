package eisemann;

import java.io.IOException;

import org.opencv.core.Core;

import eisemann.view.ContentController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class EisemannApp extends Application {
	
	private Stage primaryStage;
    private BorderPane rootLayout;

	@Override
	public void start(Stage primaryStage) {
		 this.primaryStage = primaryStage;
	     this.primaryStage.setTitle("Eisemann");

	     initRootLayout();
	     showContent();
	}

	/**
     * Initializes the root layout.
     */
	public void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(EisemannApp.class.getResource("view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	 /**
     * Shows layout with photos
     */
    public void showContent() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(EisemannApp.class.getResource("view/Content.fxml"));
            GridPane content = (GridPane) loader.load();

            rootLayout.setCenter(content);
            
            ContentController controller = loader.getController();
            controller.setEisemannApp(this);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Returns the main stage.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}
}
