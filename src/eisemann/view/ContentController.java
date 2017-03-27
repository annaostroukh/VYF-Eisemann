package eisemann.view;

import java.io.File;

import eisemann.EisemannApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ContentController {
	
	private EisemannApp eisemannApp;
	
	private Stage contentStage; 
	private GridPane contentLayout;
	private ImageController imageController;
	
	final FileChooser fileChooser = new FileChooser();
    
    @FXML
    private ImageView imageViewNoFlash;
    
    @FXML
    private ImageView imageViewFlash;
    
    @FXML
    private ImageView imageViewResult;
    
    @FXML
    private ImageView imageViewResultTest;

	@FXML
    private Button uploadNoFlash;
    
    @FXML
    private Button uploadFlash;
    
    @FXML
    private Button getResult;
    
    @FXML
    private Button exportImage;
	
	public ContentController() {
		imageController = new ImageController();
	}
	
	private void initialize() {
	}
	 
	public void setEisemannApp(EisemannApp eisemannApp) {
        this.eisemannApp = eisemannApp;
	}
	
	@FXML
	private void handleButtonAction(ActionEvent event) {
		File file = fileChooser.showOpenDialog(contentStage);
        if (file != null) {
            loadImage(file);
        }    
	 }
	
	@FXML
	private void handleResultAction(ActionEvent event) {
		if (imageController.getImageNoFlash() != null && imageController.getImageFlash() != null) {
			//imageController.getIntensity();
			imageController.getColor();
			
			Image image = imageController.getImageResult();
			Image imageTest = imageController.getImageResultTest(); //TODO:Delete in final version
			imageViewResult.setImage(image);
			imageViewResultTest.setImage(imageTest);
		}
			
	}
	
	@FXML
	private void handleExportAction(ActionEvent event) {
		
	}
	
	private void loadImage(File file) {
		String pathToImage = file.toURI().toString();
		String absolutePathToImage = file.getAbsolutePath();
		Image image = new Image(pathToImage);
		if (uploadNoFlash.isFocused()) {
			imageViewNoFlash.setImage(image);
			imageController.setImageNoFlash(absolutePathToImage);
		}
		else if(uploadFlash.isFocused()) {
			imageViewFlash.setImage(image);
			imageController.setImageFlash(absolutePathToImage);
		}	
	}

}
