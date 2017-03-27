package eisemann.view;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.scene.image.Image;

public class ImageController {
	
	private String imageNoFlash;
	private String imageFlash;
	private Image imageResult;
	private Image imageResultTest;

	private Mat imgNoFlash;
	private Mat imgFlash;
	private MatOfByte buffer;
	
	public String getImageNoFlash() {
		return imageNoFlash;
	}

	public void setImageNoFlash(String imageNoFlash) {
		this.imageNoFlash = imageNoFlash;
	}

	public String getImageFlash() {
		return imageFlash;
	}

	public void setImageFlash(String imageFlash) {
		this.imageFlash = imageFlash;
	}
	
	public Image getImageResult() {
		return imageResult;
	}

	public void setImageResult(Image imageResult) {
		this.imageResult = imageResult;
	}
	
	public Image getImageResultTest() {
		return imageResultTest;
	}

	public void setImageResultTest(Image imageResultTest) {
		this.imageResultTest = imageResultTest;
	}

	public ImageController() {
		imgNoFlash = new Mat();
		imgFlash = new Mat();
		buffer = new MatOfByte();
	}
	
	 private void initialize() {
	 }
	
	public void getIntensity () {		
		imgNoFlash = computeIntensity(getImageNoFlash());
		imgFlash = computeIntensity(getImageFlash());
	
		Imgcodecs.imencode(".jpg", imgNoFlash, buffer);
		imageResult = new Image(new ByteArrayInputStream(buffer.toArray()));
		setImageResult(imageResult);
		
		Imgcodecs.imencode(".jpg", imgFlash, buffer);
		imageResultTest = new Image(new ByteArrayInputStream(buffer.toArray()));
		setImageResultTest(imageResultTest);
	}
	
	public void getColor() {
		imgNoFlash = computeColor(getImageNoFlash());
		imgFlash = computeColor(getImageFlash());
		
		Imgcodecs.imencode(".jpg", imgNoFlash, buffer);
		imageResult = new Image(new ByteArrayInputStream(buffer.toArray()));
		setImageResult(imageResult);
		
		Imgcodecs.imencode(".jpg", imgFlash, buffer);
		imageResultTest = new Image(new ByteArrayInputStream(buffer.toArray()));
		setImageResultTest(imageResultTest);	
	}
	
	/**
     * Method computes the color layer of image (original image divided by intensity)
     * @param imagePath
     * @return color
     */
	private Mat computeColor(String imagePath) {
		Mat color = new Mat();
		Mat colorR = new Mat();
		Mat colorG = new Mat();
		Mat colorB = new Mat();
		Mat originalImage = new Mat();
		
		Mat intensity = computeIntensity(imagePath);
		//intensity.convertTo(intensity, CvType.CV_8UC1);
		originalImage = Imgcodecs.imread(imagePath);
		originalImage.convertTo(originalImage, CvType.CV_64FC3);
		
		List<Mat> rgb = new ArrayList<Mat>(3);
		Core.split(originalImage, rgb);
		Mat R = rgb.get(0);
		Mat G = rgb.get(1);
		Mat B = rgb.get(2);
		
		//System.out.println(intensity.channels());
		Core.divide(R, intensity, colorR, 255.0, CvType.CV_8UC1);
		Core.divide(G, intensity, colorG, 255.0, CvType.CV_8UC1);
		Core.divide(B, intensity, colorB, 255.0, CvType.CV_8UC1);
		
		List<Mat> colorRGB = new ArrayList<Mat>(3);
		colorRGB.add(0,colorR);
		colorRGB.add(1,colorG);
		colorRGB.add(2,colorB);
		Core.merge(colorRGB, color);
		
		return color;
	}
	
	/**
     * Method computes the intensity layer of image
     * @param imagePath 
     * @return intensity
     */
	private Mat computeIntensity(String imagePath) {
		Mat imgFinal = new Mat();
		Mat sum = new Mat();
		Mat divR = new Mat();
		Mat divG = new Mat();
		Mat divB = new Mat();
		Mat intensR = new Mat();
		Mat intensG = new Mat();
		Mat intensB = new Mat();
		Mat intensity = new Mat();
		
		imgFinal = Imgcodecs.imread(imagePath);
		imgFinal.convertTo(imgFinal, CvType.CV_64FC3);
		
		// Splitting image into RGB channels and summing them
		List<Mat> rgb = new ArrayList<Mat>(3);
		Core.split(imgFinal, rgb);
		Mat R = rgb.get(0);
		Mat G = rgb.get(1);
		Mat B = rgb.get(2);
				
		Core.add(R, G, sum);
		Core.add(sum, B, sum);
				
		// Getting intensity according to formula in article I = (R/sum(R,G,B))*R + (G/sum(R,G,B))*G + (B/sum(R,G,B))*B
		Core.divide(R, sum, divR);
		Core.divide(G, sum, divG);
		Core.divide(B, sum, divB);

		Core.multiply(divR, R, intensR);
		Core.multiply(divG, G, intensG);
		Core.multiply(divB, B, intensB);
				
		Core.add(intensR, intensB, intensity);
		Core.add(intensity, intensB, intensity);
		
		return intensity;
	}
		

}
