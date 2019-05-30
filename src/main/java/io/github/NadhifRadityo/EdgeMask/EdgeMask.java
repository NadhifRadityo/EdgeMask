package io.github.NadhifRadityo.EdgeMask;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.UIManager;

import io.github.NadhifRadityo.Objects.Canvas.CanvasPanel;
import io.github.NadhifRadityo.Objects.Canvas.Sprite;
import io.github.NadhifRadityo.Objects.Canvas.Managers.FrameLooperManager;
import io.github.NadhifRadityo.Objects.Canvas.Managers.FrameLooperManager.FrameUpdater;
import io.github.NadhifRadityo.Objects.Canvas.Managers.GraphicModifierManager;
import io.github.NadhifRadityo.Objects.Canvas.Managers.GraphicModifierManager.CustomGraphicModifier;
import io.github.NadhifRadityo.Objects.Canvas.Managers.KeyListenerManager;
import io.github.NadhifRadityo.Objects.Canvas.Managers.KeyListenerManager.CustomKeyListener;
import io.github.NadhifRadityo.Objects.Canvas.Managers.MouseListenerManager;
import io.github.NadhifRadityo.Objects.Canvas.Managers.MouseListenerManager.CustomMouseListener;
import io.github.NadhifRadityo.Objects.Canvas.RenderHints.AntiAlias;
import io.github.NadhifRadityo.Objects.Canvas.RenderHints.FontChanger;
import io.github.NadhifRadityo.Objects.Canvas.RenderHints.Easing.Easing;
import io.github.NadhifRadityo.Objects.Canvas.Shapes.Text;
import io.github.NadhifRadityo.Objects.Thread.Handler;
import io.github.NadhifRadityo.Objects.Thread.HandlerThread;
import io.github.NadhifRadityo.Objects.Utilizations.DimensionUtils;
import io.github.NadhifRadityo.Objects.Utilizations.FlatColor;
import io.github.NadhifRadityo.Objects.Utilizations.NumberUtils;

public class EdgeMask extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5473120387688304134L;

	private Dimension windowDim;
	private CanvasPanel canvasPanel;
	
	public EdgeMask() throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		GraphicsDevice screenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(screenDevice.getDefaultConfiguration());
		Rectangle screenSize = screenDevice.getDefaultConfiguration().getBounds();

		Rectangle windowBounds = new Rectangle(screenSize);
		windowBounds.x += screenInsets.left;
		windowBounds.y += screenInsets.top;
		windowBounds.width -= screenInsets.left + screenInsets.right;
		windowBounds.height -= screenInsets.top + screenInsets.bottom;

		Area taskbarArea = new Area(screenSize);
		taskbarArea.subtract(new Area(windowBounds));
//		Rectangle taskbarBounds = taskbarArea.getBounds();
		
		windowDim = windowBounds.getSize();
		setUndecorated(true);
		setSize(windowDim);
		setPreferredSize(windowDim);
		setBackground(new Color(0, 0, 0, 0));
		setLayout(new GridLayout());
		setAlwaysOnTop(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		canvasPanel = new CanvasPanel();
		canvasPanel.setSize(DimensionUtils.getMaxDimension());
		canvasPanel.setPreferredSize(DimensionUtils.getMaxDimension());
		add(canvasPanel);
		
		GraphicModifierManager graphicManager = new GraphicModifierManager(true, -2);
		graphicManager.addModifier(new AntiAlias(true));
		graphicManager.addModifier(new FontChanger(new Font("Segoe UI", Font.PLAIN, 20)));
		graphicManager.addModifier(new CustomGraphicModifier() {
			@Override public void draw(Graphics g) {
				g.setColor(new Color(0, 0, 0, 0));
				g.fillRect(0, 0, canvasPanel.getWidth(), canvasPanel.getHeight());
				g.setColor(Color.BLACK);
			} @Override public void reset(Graphics g) { }
		}, -2);
		canvasPanel.addManager(graphicManager);
		drawRefraction();
	}
	
	Color fpsColor = Color.BLACK;
	boolean insertFpsValue = false;
	boolean insertLagValue = false;
	long lagValue = 0;
	
	public void drawRefraction() throws Exception {
		HandlerThread handlerThread = new HandlerThread("Animate Stuffs");
		handlerThread.start();
		handlerThread.getLooper().setExceptionHandler(e -> e.printStackTrace());
		Handler handler = new Handler(handlerThread.getLooper());
		
		Text fpsText = new Text(0, 0, "FPS: 0");
		FrameLooperManager frameLooper = new FrameLooperManager(false, true, handler);
		frameLooper.setFps(15);
		frameLooper.addUpdater(new FrameUpdater() {
			long delta; long lastTime; int frameCount;
			@Override public void update() {
				if(lagValue > 0) try { Thread.sleep(lagValue); } catch (InterruptedException e) { e.printStackTrace(); }
				long current = System.currentTimeMillis();
				delta += current - lastTime; lastTime = current;
				frameCount++; if(delta < 1000) return;
				delta = delta % 1000;
				fpsText.setText("FPS: " + frameCount); frameCount = 0;
			}
		});
		frameLooper.setRunBehindCallback(delay -> { if(delay > 10) System.out.println("Late: " + delay + "ms"); });
		canvasPanel.addManager(frameLooper);
		
		GraphicModifierManager fpsTextManager = new GraphicModifierManager(false);
		fpsTextManager.addSprite(fpsText);
		fpsTextManager.addModifier(new CustomGraphicModifier(fpsText) {
			Color oldColor;
			@Override public void draw(Graphics g) {
				fpsText.setPosition(getWidth() - fpsText.getWidth() - 17, getHeight() - fpsText.getHeight());
				oldColor = g.getColor(); g.setColor(fpsColor);
			} @Override public void reset(Graphics g) { g.setColor(oldColor); }
		});
		frameLooper.addManager(fpsTextManager);
		
		// Draw
		canvasPanel.addSprite(new Sprite(0, 0) {
			float speedScale = 1.0f;
			int animationSpeed = 3000; 
			Easing easing = Easing.LINEAR;
			
			long start = 0;
			final float minVal = -25000.0f;
			final float maxVal = 25000.0f;
			@Override public void draw(Graphics g_) {
				if(!(g_ instanceof Graphics2D)) return;
				Graphics2D g = (Graphics2D) g_;
				
				long duration = (long) ((10000 - animationSpeed) * 10000 * speedScale);
				long current = System.currentTimeMillis();
				int nonGlicthyDur = NumberUtils.map(animationSpeed, 0, 3000, 4000, 2700);
				if(current - start >= nonGlicthyDur)
					start = current + Math.abs((current - start) % nonGlicthyDur);
				
				float value = easing.ease(current - start, minVal, maxVal, duration);
				float width = getWidth() * value;
				float height = getHeight() * value;
				Point2D startPoint = new Point2D.Float(width, height);
				Point2D endPoint = new Point2D.Float(width + getWidth(), height + getHeight());
				
				Color[] colors = new Color[] { FlatColor.Green_Sea.getColor(0.5f), FlatColor.Alizarin.getColor(), FlatColor.Green_Sea.getColor(0.5f) };
				float[] fractions = new float[colors.length];
				for(int i = 0; i < fractions.length; i++)
					fractions[i] = i / (fractions.length - 1.0f);
				
				LinearGradientPaint linear = new LinearGradientPaint(startPoint, endPoint, fractions, colors, CycleMethod.REPEAT);
				g.setPaint(linear);
				g.fill(getEdgeLighting());
				
				g.setColor(Color.BLACK);
				g.fill(getRoundedCorner());
			}
			@Override public Area getArea() { return null; }
			@Override public boolean equals(Object obj) { return this == obj; }
			
			float roundedSize = 0.2f;
			public Area getRoundedCorner() {
				float roundedSize = getTotalSize(this.roundedSize);
				Area area = new Area();
				GeneralPath path;
				
				path = new GeneralPath();
                path.moveTo(0, 0);
                path.lineTo(roundedSize, 0);
                path.curveTo(roundedSize, 0, 0, 0, 0, roundedSize);
                path.lineTo(0, 0);
                area.add(new Area(path));
                
                path = new GeneralPath();
                path.moveTo(getWidth(), 0);
                path.lineTo(getWidth(), roundedSize);
                path.curveTo(getWidth(), roundedSize, getWidth(), 0, getWidth() - roundedSize, 0);
                path.lineTo(getWidth() - roundedSize, 0);
                area.add(new Area(path));
                
                path = new GeneralPath();
                path.moveTo(0, getHeight());
                path.lineTo(0, getHeight() - roundedSize);
                path.curveTo(0, getHeight() - roundedSize, 0, getHeight(), roundedSize, getHeight());
                path.lineTo(roundedSize, getHeight());
                area.add(new Area(path));
                
                path = new GeneralPath();
                path.moveTo(getWidth(), getHeight());
                path.lineTo(getWidth(), getHeight() - roundedSize);
                path.curveTo(getWidth(), getHeight() - roundedSize, getWidth(), getHeight(), getWidth() - roundedSize, getHeight());
                path.lineTo(getWidth() - roundedSize, getHeight());
                area.add(new Area(path));
                return area;
			}
			
			float edgeSize = 0.2f;
			public Area getEdgeLighting() {
				float roundedSize = getTotalSize(this.roundedSize);
				float edgeSize = getTotalSize(this.edgeSize);
				Area area = new Area();
				GeneralPath path;
				
				area.add(new Area(new Rectangle2D.Float(0, 0, getWidth(), edgeSize)));
				area.add(new Area(new Rectangle2D.Float(0, getHeight() - edgeSize, getWidth(), getHeight() )));
				area.add(new Area(new Rectangle2D.Float(0, edgeSize, edgeSize, getHeight() - edgeSize)));
				area.add(new Area(new Rectangle2D.Float(getWidth() - edgeSize, edgeSize, getWidth(), getHeight() - edgeSize)));
				
				path = new GeneralPath();
				path.moveTo(edgeSize, edgeSize);
				path.lineTo(edgeSize + roundedSize, edgeSize);
				path.curveTo(edgeSize + roundedSize, edgeSize, edgeSize, edgeSize, edgeSize, edgeSize + roundedSize);
				path.lineTo(edgeSize, edgeSize);
				area.add(new Area(path));

				path = new GeneralPath();
				path.moveTo(getWidth() - edgeSize, edgeSize);
				path.lineTo(getWidth() - edgeSize, edgeSize + roundedSize);
				path.curveTo(getWidth() - edgeSize, edgeSize + roundedSize, getWidth() - edgeSize, edgeSize, getWidth() - edgeSize - roundedSize, edgeSize);
				path.lineTo(getWidth() - edgeSize - roundedSize, edgeSize);
				area.add(new Area(path));

				path = new GeneralPath();
				path.moveTo(edgeSize, getHeight() - edgeSize);
				path.lineTo(edgeSize, getHeight() - edgeSize - roundedSize);
				path.curveTo(edgeSize, getHeight() - edgeSize - roundedSize, edgeSize, getHeight() - edgeSize, edgeSize + roundedSize, getHeight() - edgeSize);
				path.lineTo(edgeSize + roundedSize, getHeight() - edgeSize);
				area.add(new Area(path));

				path = new GeneralPath();
				path.moveTo(getWidth() - edgeSize, getHeight() - edgeSize);
				path.lineTo(getWidth() - edgeSize, getHeight() - edgeSize - roundedSize);
				path.curveTo(getWidth() - edgeSize, getHeight() - edgeSize - roundedSize, getWidth() - edgeSize, getHeight() - edgeSize, getWidth() - edgeSize - roundedSize, getHeight() - edgeSize);
				path.lineTo(getWidth() - edgeSize - roundedSize, getHeight() - edgeSize);
				area.add(new Area(path));
				return area;
			}
			
			float getTotalSize(float f) {
				int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
				return (dpi * f) * (dpi * 0.003125f);
			}
		});
		// End draw
		
		MouseListenerManager mouseManager = new MouseListenerManager(true);
		mouseManager.addListener(new CustomMouseListener(null) {
			public void mouseClicked(MouseEvent e) { canvasPanel.requestFocus(); };
		});
		canvasPanel.addManager(mouseManager);
		
		KeyListenerManager keyManager = new KeyListenerManager(true);
		keyManager.addListener(new CustomKeyListener(null, KeyEvent.VK_F) {
			public void keyPressed(KeyEvent e) { System.out.println("Insert Fps"); insertFpsValue = true; fpsColor = Color.GREEN; };
		});
		keyManager.addListener(new CustomKeyListener(null, KeyEvent.VK_L) {
			public void keyPressed(KeyEvent e) { System.out.println("Insert Lag"); insertLagValue = true; fpsColor = Color.RED; };
		});
		keyManager.addListener(new CustomKeyListener(null) {
			String inputNumber = "";
			public void keyPressed(KeyEvent e) {
				if(!insertLagValue && !insertFpsValue) { inputNumber = ""; return; }
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER) {
					fpsColor = Color.BLACK;
					if(insertLagValue) { insertLagValue = false;
						lagValue = (e.getKeyCode() == KeyEvent.VK_ESCAPE) ? lagValue : Long.parseLong(inputNumber);
					} if(insertFpsValue) { insertFpsValue = false;
						frameLooper.setFps((e.getKeyCode() == KeyEvent.VK_ESCAPE) ? frameLooper.getFps() : Integer.parseInt(inputNumber));
					} inputNumber = ""; return;
				} if(!NumberUtils.isNumber(e.getKeyChar() + "")) return;
				inputNumber += e.getKeyChar();
				System.out.println(inputNumber);
			};
		});
		keyManager.addListener(new CustomKeyListener(null, KeyEvent.VK_ALT) {
			public void keyPressed(KeyEvent e) { System.exit(0); };
		});
		canvasPanel.addManager(keyManager);
	}
	
	public static void main(String... strings) {
		try {
			EdgeMask edgeMask = new EdgeMask();
			edgeMask.setVisible(true);
		} catch (Exception e) { e.printStackTrace(); }
	}
}
