import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jhlabs.image.NoiseFilter;


public class InfraredPhotographyFilter extends JFrame implements ChangeListener{

	/**
	 * @param args
	 */
	BufferedImage beforeImage;
	BufferedImage monoChromeImage;
	BufferedImage monoChromeImageB;
	BufferedImage blurImage;
	BufferedImage blendImage;
	BufferedImage pseudoColorImage;
	BufferedImage pseudoColorImageB;
	BufferedImage finalComposite;
	int width;
	int height;
	float[] bluFilter = {1f/9f, 1f/9f, 1f/9f,
			          1f/9f, 1f/9f, 1f/9f,
			          1f/9f, 1f/9f, 1f/9f};
	
	float redChannel = 0.9f;
	float greenChannel = 0.9f;
	float blueChannel = 0.0f;
	float saturation = 0.06f;
	
	static int SLIDER_MAX = 1000;
	static int SLIDER_MIN = 0;
	static int SLIDER_FACTOR = 1000;
	JSlider sliderRed;
	JSlider sliderGreen;
	JSlider sliderBlue;
	JSlider sliderSaturation;
	ImageProcessor ip;
	
	public InfraredPhotographyFilter(){
		this.setTitle("Infrared Photography Filter");
		this.setVisible(true);
		sliderRed = new JSlider(JSlider.HORIZONTAL,SLIDER_MIN,SLIDER_MAX,0);
		sliderRed.addChangeListener(this);
		sliderRed.setValue((int)(redChannel*SLIDER_FACTOR));
		
		sliderGreen = new JSlider(JSlider.HORIZONTAL,SLIDER_MIN,SLIDER_MAX,0);
		sliderGreen.addChangeListener(this);
		sliderGreen.setValue((int)(greenChannel*SLIDER_FACTOR));
		
		sliderBlue = new JSlider(JSlider.HORIZONTAL,SLIDER_MIN,SLIDER_MAX,0);
		sliderBlue.addChangeListener(this);
		sliderBlue.setValue((int)(blueChannel*SLIDER_FACTOR));
		
		sliderSaturation = new JSlider(JSlider.HORIZONTAL,SLIDER_MIN,SLIDER_MAX,0);
		sliderSaturation.addChangeListener(this);
		sliderSaturation.setValue((int)(saturation*SLIDER_FACTOR));
		this.add(sliderRed);
		this.add(sliderGreen);
		this.add(sliderBlue);
		this.add(sliderSaturation);
		
		loadImages();
		
		//Anonymous inner-class listener to terminate program
		this.addWindowListener(
				new WindowAdapter(){//anonymous class definition
					public void windowClosing(WindowEvent e){
						System.exit(0);//terminate the program
						ip.cancleThread();
					}//end windowClosing()
				}//end WindowAdapter
						);//end addWindowListener
	}
	
	private BufferedImage monoChromeFilter(BufferedImage src){
		BufferedImage result = new BufferedImage(src.getWidth(),
                src.getHeight(), src.getType());

        //TODO: Complete this
        for(int i=0; i<result.getWidth(); i++){
        	for(int j=0; j<result.getHeight(); j++){
        		int rgb = src.getRGB(i, j);
        		
        		float avg = clip((float)(getRed(rgb)*redChannel + getGreen(rgb)*greenChannel
        				+ getBlue(rgb)*blueChannel)/255);
        		Color c = new Color(avg,avg,avg);
        		result.setRGB(i, j, c.getRGB());
        	}
        }
        
        return result;
	}
	
	
	private BufferedImage blur(BufferedImage src){
		Kernel kernel = new Kernel(3,3,bluFilter);
		ConvolveOp con = new ConvolveOp(kernel,ConvolveOp.EDGE_NO_OP,null);
		BufferedImage result = con.filter(src, null);
		return result;
	}
	
	private BufferedImage nearInfrared(BufferedImage src){
		BufferedImage result = new BufferedImage(src.getWidth(),
				src.getHeight(), src.getType());

		
		// Write your code here
		for(int i = 0; i < result.getWidth(); i++){
			for(int j = 0; j < result.getHeight(); j++){
				int rgb = src.getRGB(i, j);
				int red = getRed(rgb);
				int green = getGreen(rgb);
				int blue = getBlue(rgb);
				
				Color c = new Color(green,blue,0);
				result.setRGB(i, j, c.getRGB());
				
			}
			
		}
		
		
		return result;
	}
	
	public BufferedImage combineImages(BufferedImage src1, BufferedImage src2, Operations op) {

		if (src1.getType() != src2.getType()) {
			System.out.println("Source Images should be of the same type");
			return null;
		}
		BufferedImage result = new BufferedImage(src1.getWidth(),
				src1.getHeight(), src1.getType());
		
		for (int i = 0; i < result.getWidth(); i++)
			for (int j = 0; j < result.getHeight(); j++) {
				int rgb1 = src1.getRGB(i, j);
				int rgb2 = src2.getRGB(i, j);
		
				int newR = 0, newG = 0, newB = 0;
				
				if (op == Operations.add) {
					// Write your code here
					newR = getRed(rgb1) + getRed(rgb2);
					newG = getGreen(rgb1) + getGreen(rgb2);
					newB = getBlue(rgb1) + getBlue(rgb2);
				}
				else if (op == Operations.subtract) {
					// Write your code here
					newR = Math.abs(getRed(rgb1) - getRed(rgb2));
					newG = Math.abs(getGreen(rgb1) - getGreen(rgb2));
					newB = Math.abs(getBlue(rgb1) - getBlue(rgb2));
				}
				else if (op == Operations.multiply) {
					// Write your code here
					newR = (getRed(rgb1) * getRed(rgb2))/255;
					newG = (getGreen(rgb1) * getGreen(rgb2))/255;
					newB = (getBlue(rgb1) * getBlue(rgb2))/255;
				}
				
				Color newRGB = new Color(clip(newR), clip(newG), clip(newB));
				result.setRGB(i, j, newRGB.getRGB());
			}
		
		return result;
	}
	
	
	public BufferedImage over(BufferedImage foreground, BufferedImage matte, BufferedImage background) {
		BufferedImage result = new BufferedImage(foreground.getWidth(),foreground.getHeight(), foreground.getType());

		// Write your code here
		for(int i = 0; i < result.getWidth(); i++){
			for(int j = 0; j < result.getHeight(); j++){

				int rgb1 = foreground.getRGB(i, j);
				int rgb2 = background.getRGB(i, j);
				int matteRgb = matte.getRGB(i, j);

				int red = ((getRed(rgb1) * getRed(matteRgb)) + ((255-getRed(matteRgb)) * getRed(rgb2)))/255;
				int green = ((getGreen(rgb1) * getGreen(matteRgb)) + ((255-getGreen(matteRgb)) * getGreen(rgb2)))/255;
				int blue = ((getBlue(rgb1) * getBlue(matteRgb)) + ((255-getBlue(matteRgb)) * getBlue(rgb2)))/255;

				Color c = new Color(clip(red),clip(green),clip(blue));
				result.setRGB(i, j, c.getRGB());
			}

		}

		return result;
	}
	
	public BufferedImage dissolve(BufferedImage src1, BufferedImage src2,float mixValue) 
	{
		if (mixValue > 1 || mixValue < 0) {
			System.out.println("mixValue shouldbe within [0, 1]");
			return null;
		}

		BufferedImage result = new BufferedImage(src1.getWidth(),
				src1.getHeight(), src1.getType());

		
		// Write your code here
		for(int i = 0; i < result.getWidth(); i++){
			for(int j = 0; j < result.getHeight(); j++){
				
				int rgb1 = src1.getRGB(i, j);
				int rgb2 = src2.getRGB(i, j);
				
				float red = ((getRed(rgb1) * mixValue) + ((1-mixValue) * getRed(rgb2)))/255;
				float green = ((getGreen(rgb1) * mixValue) + ((1-mixValue) * getGreen(rgb2)))/255;
				float blue = ((getBlue(rgb1) * mixValue) + ((1-mixValue) * getBlue(rgb2)))/255;
				
				Color c = new Color(clip(red),clip(green),clip(blue));
				result.setRGB(i, j, c.getRGB());
			}
			
		}
		
		
		return result;
	}
	
	private BufferedImage reduceSaturation(BufferedImage src){
		BufferedImage result = new BufferedImage(src.getWidth(),
				src.getHeight(), src.getType());
		float[] hsbSrc = new float[3];
		
		for(int i=0; i<result.getWidth(); i++){
			for(int j=0; j<result.getHeight(); j++){
				int rgbSrc = src.getRGB(i, j);
				
				hsbSrc = Color.RGBtoHSB(getRed(rgbSrc),getGreen(rgbSrc),getBlue(rgbSrc),hsbSrc);
				hsbSrc[1] = saturation;
				int colorCorrectedRgb = Color.HSBtoRGB(hsbSrc[0],hsbSrc[1],hsbSrc[2]);
				result.setRGB(i, j, colorCorrectedRgb);
			}
		}
		return result;
	}
	
	private BufferedImage addNoise(BufferedImage src){
		NoiseFilter filter = new NoiseFilter();
		filter.setAmount(20);
		filter.setDistribution(NoiseFilter.UNIFORM);
		filter.setMonochrome(true);
		BufferedImage result= new BufferedImage(src.getWidth(),
				src.getHeight(), src.getType());
		filter.filter(src, result);
		return result;
	}
	
	
	
	private void loadImages(){
		try{
			beforeImage = ImageIO.read(new File("originalTrees.jpg"));
			ip = new ImageProcessor();
			Thread imageProcessingThread = new Thread(ip);
			imageProcessingThread.start();
		}catch (Exception e){
			System.out.println("Cannot load the provided image");
			e.printStackTrace();
		}
		width = beforeImage.getWidth();
		height = beforeImage.getHeight();
	}
	private void update(){
		monoChromeImage = monoChromeFilter(beforeImage);
		monoChromeImageB = reduceSaturation(beforeImage);
		
		BufferedImage background_copy = copyImg(monoChromeImage);
		blurImage = blur(background_copy);
		blendImage = over(monoChromeImage,blurImage,beforeImage);
		pseudoColorImage = addNoise(combineImages(beforeImage,blendImage,Operations.subtract));
		pseudoColorImageB = addNoise(nearInfrared(beforeImage));
		
		finalComposite = addNoise(dissolve(blendImage,monoChromeImageB,0.6f));
	}
	
	public static BufferedImage copyImg(BufferedImage input) {
		BufferedImage tmp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < input.getWidth(); x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				tmp.setRGB(x, y, input.getRGB(x, y));
			}
		}
		return tmp;
	}
	
	private int clip(int v) {
		v = v > 255 ? 255 : v;
		v = v < 0 ? 0 : v;
		return v;
	}
	private float clip(float v){
		if (v<0){
			v = 0;
		}
		if(v>1){
			v = 1;
		}
		return v;
	}

	protected int getRed(int pixel) {
		return (pixel >>> 16) & 0xFF;
	}

	protected int getGreen(int pixel) {
		return (pixel >>> 8) & 0xFF;
	}

	protected int getBlue(int pixel) {
		return pixel & 0xFF;
	}
	
	public void paint(Graphics g){
		super.paint(g);
		int w = width/2;
		int h = height/2;
		
		this.setSize(w*5 +80,h*4+50);
		g.setColor(Color.BLACK);
	    Font f1 = new Font("Verdana", Font.PLAIN, 13);  
	    g.setFont(f1);
	    
	    g.drawImage(beforeImage,20,50,w, h,this);
	    g.drawImage(monoChromeImage, 50+w, 50, w, h,this);
	    g.drawImage(monoChromeImageB,50+w,50+h+180,w, h,this);
	    g.drawImage(blurImage, 80+w*2, 50, w, h,this);
	    g.drawImage(blendImage, 150+w*3, 50, w, h,this);
	    g.drawImage(finalComposite,150+w*3,50+h+70,w, h,this);
	    g.drawImage(pseudoColorImage,150+w*3,50+h+70+250,w, h,this);
	    g.drawImage(pseudoColorImageB,80+w*2,50+h+70+250,w, h,this);
	    
	    g.drawString("Original Image", 20, 40);
	    g.drawString("MonoChrome Image", 50+w, 40);
	    g.drawString("Blurred Monochrome Image", 80+w*2, 40);
	    g.drawString("Blend Image:Over Original+MonoChrome with Matte(Blurred Image)",150+w*3, 40);
	    g.drawString("Final Composite Image:Keymix(Blend Image & Color Corrected)", 150+w*3, 50+h+60);
	    g.drawString("Final PseudoColor: Subtract Original & blend",150+w*3 ,50+h+70+240);
	    g.drawString("Final PseudoColor: Literal Color Shift",80+w*2 ,50+h+70+240);
	    g.drawString("Color Corrected", 50+w, 50+h+170);
	    g.drawString("Red Channel", 50+w, 70+h);
	    g.drawString("Green Channel", 50+w, 50+h+70);
	    g.drawString("Blue Channel", 50+w, 50+h+120);
	    g.drawString("Change Saturation", 50+w, 50+h+180+220);
	    
	    formatSliders(w,h);
	    
	    repaint();
	}
	private void formatSliders(int w, int h){
		if(sliderGreen != null){
			sliderRed.setSize(width/2, 20);
			sliderRed.setLocation(50+w, 50+h);
			
			sliderGreen.setSize(width/2, 20);
			sliderGreen.setLocation(50+w, 50+h+50);
			
			sliderBlue.setSize(width/2, 20);
			sliderBlue.setLocation(50+w, 50+h+100);
			
			sliderSaturation.setSize(width/2, 20);
			sliderSaturation.setLocation(50+w, 50+h+180+200);
		}
		
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		JSlider slider = (JSlider) e.getSource();
		if(slider.equals(sliderRed)){
			redChannel = (float)slider.getValue()/(float)SLIDER_FACTOR;
		}
		else if(slider.equals(sliderGreen)){
			greenChannel = (float)slider.getValue()/(float)SLIDER_FACTOR;
		}
		else if(slider.equals(sliderBlue)){
			blueChannel = (float)slider.getValue()/(float)SLIDER_FACTOR;
		}
		else if(slider.equals(sliderSaturation)){
			saturation = (float)slider.getValue()/(float)SLIDER_FACTOR;
		}
	}
	
	public class ImageProcessor implements Runnable{
		private volatile boolean cancelled = false;

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(!cancelled){
				update();
			}
		}
		
		public void cancleThread(){
			cancelled = true;
		}
		
	}
	
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		InfraredPhotographyFilter infraredFilter = new InfraredPhotographyFilter();
		infraredFilter.repaint();
	}

	

}
