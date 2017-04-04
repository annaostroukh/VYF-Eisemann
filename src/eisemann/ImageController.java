package eisemann;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

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
		imgNoFlash = Imgcodecs.imread(getImageNoFlash());
		imgFlash = Imgcodecs.imread(getImageFlash());
		Mat intensityF = computeIntensity(imgFlash);
		Mat intensityNF = computeIntensity(imgNoFlash);
		Mat largeScaleNF = computeLargeScale(intensityNF);
		Mat largeScaleF = computeLargeScale(intensityF);
		Mat detail = computeDetail(intensityF, largeScaleF);
		Mat color = computeColor(imgFlash, intensityF);
		Mat finalR = new Mat();
		Mat finalG = new Mat();
		Mat finalB = new Mat();
		Mat image = new Mat();
		List<Double> weights = whiteBalanceCorr(imgFlash, imgNoFlash);
		
		color.convertTo(color, CvType.CV_64F);
		largeScaleNF.convertTo(largeScaleNF, CvType.CV_64F);
		detail.convertTo(detail, CvType.CV_64F);
		
		Core.add(detail, largeScaleNF, image);
		
		List<Mat> rgb = getRGB(color);
	
		Core.add(image, rgb.get(0), finalR);
		Core.add(image, rgb.get(1), finalG);
		Core.add(image, rgb.get(2), finalB);
		
		Scalar R = new Scalar(weights.get(0));
		Scalar G = new Scalar(weights.get(1));
		Scalar B = new Scalar(weights.get(2));
		
		// White balance correction
		Core.multiply(finalR, R, finalR);
		Core.multiply(finalG, G, finalG);
		Core.multiply(finalB, B, finalB);

		List<Mat> finalRGB = new ArrayList<Mat>(3);
		finalRGB.add(0,finalR);
		finalRGB.add(1,finalG);
		finalRGB.add(2,finalB);
		Core.merge(finalRGB, image);

		Imgcodecs.imencode(".jpg", bcNormalize(image), buffer);
		imageResult = new Image(new ByteArrayInputStream(buffer.toArray()));
		setImageResult(imageResult);
		
		// Layers test
		Imgcodecs.imencode(".jpg", intensityF, buffer);
		imageResultTest = new Image(new ByteArrayInputStream(buffer.toArray()));
		setImageResultTest(imageResultTest);	
	}
	
	/**
     * Method computes weighted values for white balance correction
     * @param image with flash, image without flash
     * @return weighted values
    */
	private List<Double> whiteBalanceCorr(Mat imgF, Mat imgNF) {
		imgF.convertTo(imgF, CvType.CV_64F);
		imgNF.convertTo(imgNF, CvType.CV_64F);
		Mat output = new Mat();
		double pow = 0.1;
		double weightR = 0.0;
		double weightG = 0.0;
		double weightB = 0.0;
		List<Double> weights = new ArrayList(3);
		List<Mat> imgFRGB = getRGB(imgF);
		List<Mat> imgNFRGB = getRGB(imgNF);
		
		weightR = Math.pow((double)(0.4*getSum(getBrightPixels(imgFRGB.get(0), getMax(imgFRGB.get(0)))) 
				+ 0.6*getSum(getBrightPixels(imgNFRGB.get(0), getMax(imgNFRGB.get(0))))) , pow);
		weightG =  Math.pow((double)(0.4*getSum(getBrightPixels(imgFRGB.get(1), getMax(imgFRGB.get(1)))) 
				+ 0.6*getSum(getBrightPixels(imgNFRGB.get(1), getMax(imgNFRGB.get(1))))), pow);
		weightB =  Math.pow((double)(0.4*getSum(getBrightPixels(imgFRGB.get(2), getMax(imgFRGB.get(2)))) 
				+ 0.6*getSum(getBrightPixels(imgNFRGB.get(2), getMax(imgNFRGB.get(2))))) , pow);
		
		weights.add(weightR);
		weights.add(weightG);
		weights.add(weightB);
		return weights;
	}
		
	/**
     * Method computes the detail layer of image (intensity divided by large scale layer)
     * @param intensity level, large scale level
     * @return detail layer
    */
	private Mat computeDetail(Mat intensity, Mat largeScale) {
		intensity.convertTo(intensity, CvType.CV_32F);
		Mat detail = new Mat();
		
		Core.divide(intensity, largeScale, detail, 255.0);
	
		return bcNormalize(detail);
	}
	
	/**
     * Method computes the large scale layer of image (using bilateral filtering on intensity)
     * @param intensity level
     * @return large scale
    */
	private Mat computeLargeScale(Mat intensity) {
		intensity.convertTo(intensity, CvType.CV_32F);
		Mat output = new Mat();
		double sigmaColor = 40;
		double sigmaSpace = 40;
 
		Imgproc.bilateralFilter(intensity, output, 15, sigmaColor, sigmaSpace);
		
		return output;
	}
	
	/**
     * Method computes the color layer of image (original image divided by intensity)
     * @param image matrix, intensity level
     * @return color
    */
	private Mat computeColor(Mat img, Mat intensity) {
		img.convertTo(img, CvType.CV_64FC3);
		Mat color = new Mat();
		Mat colorR = new Mat();
		Mat colorG = new Mat();
		Mat colorB = new Mat();
		
		List<Mat> rgb = getRGB(img);
		
		Core.divide(rgb.get(0), intensity, colorR, 255.0, CvType.CV_8UC1);
		Core.divide(rgb.get(1), intensity, colorG, 255.0, CvType.CV_8UC1);
		Core.divide(rgb.get(2), intensity, colorB, 255.0, CvType.CV_8UC1);
		
		List<Mat> colorRGB = new ArrayList<Mat>(3);
		colorRGB.add(0,colorR);
		colorRGB.add(1,colorG);
		colorRGB.add(2,colorB);
		Core.merge(colorRGB, color);
		
		return color;
	}
	
	/**
     * Method computes the intensity layer of image
     * @param image matrix
     * @return intensity
    */
	private Mat computeIntensity(Mat img) {
		img.convertTo(img, CvType.CV_64FC3);
		Mat sum = new Mat();
		Mat divR = new Mat();
		Mat divG = new Mat();
		Mat divB = new Mat();
		Mat intensR = new Mat();
		Mat intensG = new Mat();
		Mat intensB = new Mat();
		Mat intensity = new Mat();
		
		List<Mat> rgb = getRGB(img);
				
		Core.add(rgb.get(0), rgb.get(1), sum);
		Core.add(sum, rgb.get(2), sum);
				
		// Getting intensity according to formula in the article I = (R/sum(R,G,B))*R + (G/sum(R,G,B))*G + (B/sum(R,G,B))*B
		Core.divide(rgb.get(0), sum, divR);
		Core.divide(rgb.get(1), sum, divG);
		Core.divide(rgb.get(2), sum, divB);

		Core.multiply(divR, rgb.get(0), intensR);
		Core.multiply(divG, rgb.get(1), intensG);
		Core.multiply(divB, rgb.get(2), intensB);
				
		Core.add(intensR, intensG, intensity);
		Core.add(intensity, intensB, intensity);
		
		return bcNormalize(intensity);
	}
	
	/**
     * Method normalize output values after arithmetical operations
     * @param image to normalize 
     * @return normalized image
    */
	private Mat bcNormalize(Mat img) {
		
		Object minValue = Collections.min(Mat2Double(img)).intValue();
		Object maxValue = Collections.max(Mat2Double(img)).intValue();

		int range = (int) maxValue - (int)minValue;
		float alpha = (float) 255 / range;
		float beta = (int) minValue * alpha;
		
		img.convertTo(img, CvType.CV_64F, alpha, -beta);
		
		return img;
	}
	
	/**
     * Method computes bright pixels for WB correction
     * @param channel matrix, bright pixel value boundary
     * @return list of bright pixels
    */
	private List<Double> getBrightPixels(Mat channel, double brPixel) {
		List<Double> brPixels = new ArrayList();
		List<Double> ch = Mat2Double(channel);
		
		Iterator<Double> iterator = ch.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == brPixel) {
				brPixels.add(iterator.next());
			}
		}
		return brPixels;
	}
	
	/**
     * Method converts Mat do List of Double
     * @param image to convert
     * @return list
    */
	private List<Double> Mat2Double (Mat img) {
		img.convertTo(img, CvType.CV_64F);
		
		int size = (int) (img.total() * img.channels());
		double[] temp = new double[size];
		img.get(0, 0, temp);
		List<Double> pixels = Arrays.stream(temp).boxed().collect(Collectors.toList());
		
		return pixels;
	}
	
	/**
     * Method for splitting image into RGB channels
     * @param image to split 
     * @return RGB channels list
    */
	private List<Mat> getRGB (Mat img) {
		img.convertTo(img, CvType.CV_64F);
		
		List<Mat> imgRGB = new ArrayList<Mat>(3);
		Core.split(img, imgRGB);
		
		return imgRGB;
	}
	
	/**
     * Method returns maximum value from image matrix
     * @param image matrix 
     * @return maximum value
    */
	private double getMax(Mat matrix) {
		double max = 0.0;
		max = Collections.max(Mat2Double(matrix)).intValue();
		return max;
	}
	
	/**
     * Method for finding sum of all elements in list
     * @param list of elements 
     * @return sum of elements
    */
	private double getSum(List<Double> list) {
		double sum = 0.0;
		Iterator<Double> iterator = list.iterator();
		while (iterator.hasNext()) {
			sum =+ iterator.next();
		}
		return sum;
	}

		

}
