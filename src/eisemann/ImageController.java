package eisemann;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.scene.image.Image;

public class ImageController {
	
	private String imageNoFlash;
	private String imageFlash;
	private Image imageResult;

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
		Mat intensityNF = computeIntensityNF(imgFlash, imgNoFlash);
		Mat intensityNFLarge = computeIntensity(imgNoFlash);
		Mat largeScaleNF = computeLargeScale(intensityNFLarge);
		Mat largeScaleF = computeLargeScale(intensityF);
		Mat detail = computeDetail(intensityF, largeScaleF);
		Mat detailNF = computeDetail(intensityNFLarge, largeScaleNF);
		Mat color = computeColor(imgFlash, intensityF);
		Mat colorNF = computeColor(imgNoFlash, intensityNF);
		Mat finalR = new Mat();
		Mat finalG = new Mat();
		Mat finalB = new Mat();
		Mat image = new Mat();
		List<Double> weights = whiteBalanceCorr(imgFlash, imgNoFlash);
		
		color.convertTo(color, CvType.CV_64F);
		largeScaleNF.convertTo(largeScaleNF, CvType.CV_64F);
		detail.convertTo(detail, CvType.CV_64F);
		
		Core.multiply(detail, largeScaleNF, image);
		
		List<Mat> rgb = getRGB(color);
	
		Core.multiply(image, rgb.get(0), finalR);
		Core.multiply(image, rgb.get(1), finalG);
		Core.multiply(image, rgb.get(2), finalB);
		
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
		
		Imgcodecs.imwrite(getImageFlash().substring(0,getImageFlash().lastIndexOf("."))+"-final.jpg", image);
		
		// Uncomment to get all layers from both images
		/*
		Imgcodecs.imwrite(getImageNoFlash().substring(0,getImageNoFlash().lastIndexOf("."))+"-intensity.jpg", intensityNF);
		Imgcodecs.imwrite(getImageFlash().substring(0,getImageFlash().lastIndexOf("."))+"-intensity.jpg", intensityF);
		
		Imgcodecs.imwrite(getImageNoFlash().substring(0,getImageNoFlash().lastIndexOf("."))+"-largeScale.jpg", largeScaleNF);
		
		Imgcodecs.imwrite(getImageFlash().substring(0,getImageFlash().lastIndexOf("."))+"-largeScale.jpg", largeScaleF);
		
		Imgcodecs.imwrite(getImageFlash().substring(0,getImageFlash().lastIndexOf("."))+"-detail.jpg", detail);
		Imgcodecs.imwrite(getImageNoFlash().substring(0,getImageNoFlash().lastIndexOf("."))+"-detail.jpg", detailNF);
		
		Imgcodecs.imwrite(getImageFlash().substring(0,getImageFlash().lastIndexOf("."))+"-color.jpg", color);
		Imgcodecs.imwrite(getImageNoFlash().substring(0,getImageNoFlash().lastIndexOf("."))+"-color.jpg", colorNF);
		*/
	}
	
	/**
     * Method computes umbra shadow according to histogram analysis
     * @param image with flash, image without flash
     * @return umbra
    */
	private Mat shadowUmbra(Mat flashImg, Mat noFlashImg) {
		Mat umbra = new Mat();
		Mat delta = new Mat();
		Mat histogram = new Mat(); // computed histogram
		int numberOfBins = 128;
		MatOfInt channels = new MatOfInt(0, 2);
		MatOfInt histSize = new MatOfInt(numberOfBins);
		MatOfFloat ranges = new MatOfFloat(0, 256);
		List<Mat> deltaToHist = new LinkedList<Mat>();
		
		Core.subtract(flashImg, noFlashImg, delta);

		deltaToHist.add(delta);
		Imgproc.calcHist(deltaToHist, channels, new Mat(), histogram, histSize, ranges, false);
		
		return umbra;
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
		double pow = 0.2;
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
		intensity.convertTo(intensity, CvType.CV_64F);
		largeScale.convertTo(largeScale, CvType.CV_64F);
		Mat detail = new Mat();
		
		Core.divide(intensity, largeScale, detail);
	
		//return bcNormalize(detail);
		return detail;
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
		int neighbourhoodDiameter = 150;
 
		Imgproc.bilateralFilter(intensity, output, neighbourhoodDiameter, sigmaColor, sigmaSpace); 
		
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
		
		Core.divide(rgb.get(0), intensity, colorR, 255.0, CvType.CV_8U);
		Core.divide(rgb.get(1), intensity, colorG, 255.0, CvType.CV_8U);
		Core.divide(rgb.get(2), intensity, colorB, 255.0, CvType.CV_8U);
		
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
		Mat divR = new Mat();
		Mat divG = new Mat();
		Mat divB = new Mat();
		Mat intensR = new Mat();
		Mat intensG = new Mat();
		Mat intensB = new Mat();
		Mat intensity = new Mat();
		Mat sum = new Mat();
		
		List<Mat> rgb = getRGB(img);
				
		// Getting intensity according to formula in the article I = (R/sum(R,G,B))*R + (G/sum(R,G,B))*G + (B/sum(R,G,B))*B
		Core.add(rgb.get(0), rgb.get(1), sum);
		Core.add(sum, rgb.get(2), sum);
		
		Core.divide(rgb.get(0), sum, divR);
		Core.divide(rgb.get(1), sum, divG);
		Core.divide(rgb.get(2), sum, divB);
		
		Core.multiply(divR, rgb.get(0), intensR);
		Core.multiply(divG, rgb.get(1), intensG);
		Core.multiply(divB, rgb.get(2), intensB);
				
		Core.add(intensR, intensG, intensity);
		Core.add(intensity, intensB, intensity);
		
		return bcNormalize(intensity);
		//return intensity;
	}
	
	/**
     * Method computes the intensity layer of non flash image
     * For this RGB channels of flash image has to be used as weights
     * @param image matrix, non flash image matrix
     * @return intensity
    */
	private Mat computeIntensityNF(Mat img, Mat imgNF) {
		img.convertTo(img, CvType.CV_64FC3);
		imgNF.convertTo(imgNF, CvType.CV_64FC3);
		Mat divR = new Mat();
		Mat divG = new Mat();
		Mat divB = new Mat();
		Mat intensR = new Mat();
		Mat intensG = new Mat();
		Mat intensB = new Mat();
		Mat intensity = new Mat();
		Mat sum = new Mat();
		
		List<Mat> rgb = getRGB(img);
		List<Mat> rgbNF = getRGB(imgNF);
				
		// Getting intensity according to formula in the article I = (R/sum(R,G,B))*R + (G/sum(R,G,B))*G + (B/sum(R,G,B))*B
		Core.add(rgb.get(0), rgb.get(1), sum);
		Core.add(sum, rgb.get(2), sum);
		
		Core.divide(rgbNF.get(0), sum, divR);
		Core.divide(rgbNF.get(1), sum, divG);
		Core.divide(rgbNF.get(2), sum, divB);
		
		Core.multiply(divR, rgbNF.get(0), intensR);
		Core.multiply(divG, rgbNF.get(1), intensG);
		Core.multiply(divB, rgbNF.get(2), intensB);
				
		Core.add(intensR, intensG, intensity);
		Core.add(intensity, intensB, intensity);
		
		return bcNormalize(intensity);
		//return intensity;
	}
	
	/**
     * Method normalize output values after arithmetical operations
     * @param image to normalize 
     * @return normalized image
    */
	private Mat bcNormalize(Mat img) {
		
		Object minValue = Collections.min(Mat2Double(img)).intValue();
		Object maxValue = Collections.max(Mat2Double(img)).intValue();

		int range = (int) maxValue - (int) minValue;
		double alpha = (double) 255 / range;
		double beta = (int) minValue * alpha;
		
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
     * Method converts Mat to List of Doubles
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
