package eisemann;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
	
	/**
     * Method computes the final image (addition of large scale layer, detail layer and color layer)
     * @param 
     * @return 
    */
	public void getFinal() {
		//Mat intensity = computeIntensity(getImageFlash());
		Mat largeScale = computeLargeScale(getImageNoFlash());
		Mat detail = computeDetail(getImageFlash());
		Mat color = computeColor(getImageFlash());
		Mat finalR = new Mat();
		Mat finalG = new Mat();
		Mat finalB = new Mat();
		Mat image = new Mat();
		
		color.convertTo(color, CvType.CV_64F);
		largeScale.convertTo(largeScale, CvType.CV_64F);
		detail.convertTo(detail, CvType.CV_64F);
		
		Core.add(detail, largeScale, imgNoFlash);
		
		List<Mat> rgb = new ArrayList<Mat>(3);
		Core.split(color, rgb);
		Mat R = rgb.get(0);
		Mat G = rgb.get(1);
		Mat B = rgb.get(2);
	
		Core.add(imgNoFlash, R, finalR);
		Core.add(imgNoFlash, G, finalG);
		Core.add(imgNoFlash, B, finalB);

		List<Mat> finalRGB = new ArrayList<Mat>(3);
		finalRGB.add(0,finalR);
		finalRGB.add(1,finalG);
		finalRGB.add(2,finalB);
		Core.merge(finalRGB, image);

		Imgcodecs.imencode(".jpg", bcNormalize(image), buffer);
		imageResult = new Image(new ByteArrayInputStream(buffer.toArray()));
		setImageResult(imageResult);
		
		// Layers test
		Imgcodecs.imencode(".jpg", color, buffer);
		imageResultTest = new Image(new ByteArrayInputStream(buffer.toArray()));
		setImageResultTest(imageResultTest);	
	}
	
	/**
     * Method computes the detail layer of image (intensity divided by large scale layer)
     * @param path to image
     * @return detail layer
    */
	private Mat computeDetail(String imagePath) {
		Mat detail = new Mat();
		Mat intensity = new Mat();
		Mat largeScale = new Mat();
		
		intensity = computeIntensity(imagePath);
		largeScale = computeLargeScale(imagePath);
		largeScale.convertTo(largeScale, CvType.CV_64F);
		
		Core.divide(intensity, largeScale, detail, 255.0);

		return bcNormalize(detail);
	}
	
	/**
     * Method computes the large scale layer of image (using bilateral filtering on intensity)
     * @param path to image
     * @return large scale
    */
	private Mat computeLargeScale(String imagePath) {
		Mat intensity = computeIntensity(imagePath);
		Mat output = new Mat();
		double sigmaColor = 40;
		double sigmaSpace = 40;
 
		intensity.convertTo(intensity, CvType.CV_32F);
		
		Imgproc.bilateralFilter(intensity, output, 15, sigmaColor, sigmaSpace);
		
		return output;
	}
	
	/**
     * Method computes the color layer of image (original image divided by intensity)
     * @param path to image
     * @return color
    */
	private Mat computeColor(String imagePath) {
		Mat color = new Mat();
		Mat colorR = new Mat();
		Mat colorG = new Mat();
		Mat colorB = new Mat();
		Mat originalImage = new Mat();
		Mat intensity = computeIntensity(imagePath);

		originalImage = Imgcodecs.imread(imagePath);
		originalImage.convertTo(originalImage, CvType.CV_64FC3);
		
		List<Mat> rgb = new ArrayList<Mat>(3);
		Core.split(originalImage, rgb);
		Mat R = rgb.get(0);
		Mat G = rgb.get(1);
		Mat B = rgb.get(2);
		
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
     * @param path to image 
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
		
		List<Mat> rgb = new ArrayList<Mat>(3);
		Core.split(imgFinal, rgb);
		Mat R = rgb.get(0);
		Mat G = rgb.get(1);
		Mat B = rgb.get(2);
				
		Core.add(R, G, sum);
		Core.add(sum, B, sum);
				
		// Getting intensity according to formula in the article I = (R/sum(R,G,B))*R + (G/sum(R,G,B))*G + (B/sum(R,G,B))*B
		Core.divide(R, sum, divR, 255.0);
		Core.divide(G, sum, divG, 255.0);
		Core.divide(B, sum, divB, 255.0);

		Core.multiply(divR, R, intensR);
		Core.multiply(divG, G, intensG);
		Core.multiply(divB, B, intensB);
				
		Core.add(intensR, intensB, intensity);
		Core.add(intensity, intensB, intensity);
		
		return bcNormalize(intensity);
	}
	
	/**
     * Method normalize output values after arithmetical operations
     * @param image to normalize 
     * @return normalized image
    */
	private Mat bcNormalize(Mat img) {
		img.convertTo(img, CvType.CV_64F);
		
		int size = (int) (img.total() * img.channels());
		double[] temp = new double[size];
		img.get(0, 0, temp);
		
		List<Double> pixels = Arrays.stream(temp).boxed().collect(Collectors.toList());
		Object minValue = Collections.min(pixels).intValue();
		Object maxValue = Collections.max(pixels).intValue();

		int range = (int) maxValue - (int)minValue;
		float alpha = (float) 255 / range;
		float beta = (int) minValue * alpha;
		
		img.convertTo(img, CvType.CV_64F, alpha, -beta);
		
		return img;
	}
		

}
