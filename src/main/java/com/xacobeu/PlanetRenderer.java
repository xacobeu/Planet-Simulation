package com.xacobeu;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;

import com.xacobeu.Bodies.Body;
import com.xacobeu.Bodies.Planet2D;
import com.xacobeu.Bodies.Planet3D;

import java.util.ArrayList;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import javax.swing.*;

import java.awt.*;
import java.nio.FloatBuffer;

public class PlanetRenderer {

	// Window to render in.
	private long window;

	// Screen dimensions.
	public static final int WIDTH = 1024;
	public static final int HEIGHT = 1024;

	// Center of the screen.
	private static final int centerX = WIDTH / 2;
	private static final int centerY = HEIGHT / 2;

	// Gravitational constant.
	private final double G = 6.67430e-11;

	// Objects.
	private ArrayList<Body> objects2D = new ArrayList<>();
	private ArrayList<Body> objects3D = new ArrayList<>();
	private Camera3D camera = new Camera3D(0, 0, 5);

	// Canvas to integrate LJWGL with Swing.
	private static boolean running = false;
	
	// Rendering mode: 0 = 2D and 1 = 3D.
	private int renderingMode = 0;
	private static boolean lightingEnabled = false;

	private boolean[] keyStates = new boolean[GLFW_KEY_LAST + 1];

	// UI elements.
	private JFrame frame = new JFrame("Planet simulation");

	private JPanel panel = new JPanel();

	private JButton startButton = new JButton("Start");
	private JButton stopButton = new JButton("Stop");
	private JButton resetButton = new JButton("Reset");
	private JToggleButton mode2D = new JToggleButton("2D");
	private JToggleButton mode3D = new JToggleButton("3D");
	private JCheckBox lightingCheckBox = new JCheckBox("Lighting");
	private JLabel cameraSpeedLabel = new JLabel("Camera Speed: " + camera.getCameraSpeed());
	
	private ButtonGroup modes = new ButtonGroup();

	public PlanetRenderer() {
		modes.add(mode2D);
		modes.add(mode3D);
		initialiseObjects();
	}

	public void start() {
		System.out.println("Starting simulation");
		if (running) return;
		running = true;
		new Thread(this::run).start();
	}

	public void stop() {
		System.out.println("Stopping simulation");
		running = false;
	}

	public void reset() {
		System.out.println("Resetting simulation");
		objects2D.clear();
		objects3D.clear();
		initialiseObjects();
	}

	public void run() {
		init();
		render();
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
	}

	private void init() {

		System.out.println("Initialising simulation");

		glfwInit();
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(WIDTH, HEIGHT, "Planet simulation", NULL, NULL);
		if ( window == NULL ) {
			throw new RuntimeException("Failed to create the GLFW window");
		} else {
			System.out.println("Window created");
		}

		// Center the window
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(
			window,
			(vidmode.width() - WIDTH) / 2,
			(vidmode.height() - HEIGHT) / 2
		);

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1); // Enable v-sync.
		glfwShowWindow(window);

		// Set up OpenGL context.
		GL.createCapabilities();

		System.out.println("OpenGL " + glGetString(GL_VERSION) + " initialised.");

		if (renderingMode == 0) {
			System.out.println("2D rendering mode");

			// Make coordinate system match the window size.
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			glOrtho(0, WIDTH, HEIGHT, 0, -1, 1);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

		}
		else if (renderingMode == 1) {
			
			System.out.println("3D rendering mode");

			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_LESS);
			
			if (lightingEnabled) {
				glEnable(GL_LIGHTING);
				glEnable(GL_LIGHT0);
				glEnable(GL_NORMALIZE);
	
				float[] lightPosition = {0.0f, 0.0f, 0.0f, 1.0f}; // Position of the Sun (x, y, z, w)
				float[] lightAmbient = {0.2f, 0.2f, 0.2f, 1.0f}; // Ambient light (gray)
				float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f}; // Diffuse light (white)
				float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f}; // Specular light (white)
	
				// Set light properties
				glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
				glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
				glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
				glLightfv(GL_LIGHT0, GL_SPECULAR, lightSpecular);
				
			}

			// Set up the projection matrix
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			float aspect = (float) WIDTH / HEIGHT;
			float fov = 90.0f; // Field of view
			float zNear = 0.1f; // Near clipping plane
			float zFar = 100000.0f; // Far clipping plane
			float top = (float) Math.tan(Math.toRadians(fov / 2.0)) * zNear;
			float right = top * aspect;
			glFrustum(-right, right, -top, top, zNear, zFar);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

			// Set up camera rotation.
			glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
				@Override
				public void invoke(long window, double xpos, double ypos) {
					camera.handleMouseInput(xpos, ypos);
				}
			});

			// Set up camera movement.
			glfwSetKeyCallback(window, new GLFWKeyCallback() {
				@Override
				public void invoke(long window, int key, int scancode, int action, int mods) {
					if (key >= 0 && key < keyStates.length) {
						if (action == GLFW_PRESS) {
							keyStates[key] = true;
						} else if (action == GLFW_RELEASE) {
							keyStates[key] = false;
						}
					}
				}
			});

			glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		}

		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.1f, 1.0f);
		System.out.println("init complete");
	}

	private void handleKeyboardInput() {
		if (keyStates[GLFW_KEY_W]) { // Move forward
			camera.moveForward();
		}
		if (keyStates[GLFW_KEY_S]) { // Move backward
			camera.moveBackward();
		}
		if (keyStates[GLFW_KEY_A]) { // Move left
			camera.moveLeft();
		}
		if (keyStates[GLFW_KEY_D]) { // Move right
			camera.moveRight();
		}
		if (keyStates[GLFW_KEY_SPACE]) { // Move up
			camera.moveUp();
		}
		if (keyStates[GLFW_KEY_LEFT_SHIFT]) { // Move down
			camera.moveDown();
		}
		if (keyStates[GLFW_KEY_R]) { // Fast camera toggle
			camera.toggleFastCamera();
		}
		if (keyStates[GLFW_KEY_DOWN]) { // Increase speed
			camera.decreaseSpeed();
			cameraSpeedLabel.setText("Camera Speed: " + camera.getCameraSpeed());
			frame.pack();
		}
		if (keyStates[GLFW_KEY_UP]) { // Decrease speed
			camera.increaseSpeed();
			cameraSpeedLabel.setText("Camera Speed: " + camera.getCameraSpeed());
			frame.pack();
		}
	}

	private void render() {

		System.out.println("Starting rendering loop");

		// Run until escape key is pressed.
		while (running) {
			if (lightingEnabled) {
				float[] lightPosition = {(float) objects3D.get(0).getPositionX(), (float) objects3D.get(0).getPositionY(), (float) objects3D.get(0).getPositionZ(), 1.0f};
				glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
			}
	
			handleKeyboardInput();

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glLoadIdentity();

			if (renderingMode == 0) {
				// Update 2D physics.
				for (Body p1 : objects2D) {
					for (Body p2 : objects2D) {
						if (p1 == p2) continue;

						double dx = ((Planet2D) p2).getPositionX() - ((Planet2D) p1).getPositionX();
						double dy = ((Planet2D) p2).getPositionY() - ((Planet2D) p1).getPositionY();

						double distance = Math.sqrt(dx * dx + dy * dy);
						if (distance <= ((Planet2D) p1).getRadius() + ((Planet2D) p2).getRadius()) {
							p1.resolveCollision(p2);
							continue;
						}
						distance *= 6e5;

						double directionX = dx / distance;
						double directionY = dy / distance;

						double force = G * ((Planet2D) p1).getMass() * ((Planet2D) p2).getMass() / (distance * distance);
						double acc = force / ((Planet2D) p1).getMass();
						((Planet2D) p1).setVelocityX(((Planet2D) p1).getVelocityX() + acc * directionX);
						((Planet2D) p1).setVelocityY(((Planet2D) p1).getVelocityY() + acc * directionY);
					}

					p1.updatePosition();
					p1.draw();
					p1.drawTrail();
				
					((Planet2D) p1).checkBorderCollision(WIDTH, HEIGHT);
				}

			} else if (renderingMode == 1) {


				// Set the modelview matrix
				glMatrixMode(GL_MODELVIEW);
				glLoadIdentity();
	
				// Load the view matrix
				FloatBuffer viewMatrix = camera.createViewMatrix();
				glLoadMatrixf(viewMatrix);

				// // Update 3D physics.
				for (Body p1 : objects3D) {
					for (Body p2 : objects3D) {
						if (p1 == p2) continue;

						double dx = ((Planet3D) p2).getPositionX() - ((Planet3D) p1).getPositionX();
						double dy = ((Planet3D) p2).getPositionY() - ((Planet3D) p1).getPositionY();
						double dz = ((Planet3D) p2).getPositionZ() - ((Planet3D) p1).getPositionZ();

						double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
						if (distance <= ((Planet3D) p1).getRadius() + ((Planet3D) p2).getRadius()) {
							p1.resolveCollision(p2);
							continue;
						}
						distance *= 6e5;

						double directionX = dx / distance;
						double directionY = dy / distance;
						double directionZ = dz / distance;

						double force = G * ((Planet3D) p1).getMass() * ((Planet3D) p2).getMass() / (distance * distance);
						double acc = force / ((Planet3D) p1).getMass();
						((Planet3D) p1).setVelocityX(((Planet3D) p1).getVelocityX() + acc * directionX);
						((Planet3D) p1).setVelocityY(((Planet3D) p1).getVelocityY() + acc * directionY);
						((Planet3D) p1).setVelocityZ(((Planet3D) p1).getVelocityZ() + acc * directionZ);
					}
					p1.updatePosition();
					p1.draw();
					p1.drawTrail();
				}
			}

			glfwSwapBuffers(window);
			glfwPollEvents();
		}
	}

	public static boolean getLightingEnabled() {
		return lightingEnabled;
	}

	public void initialiseObjects() {
		// Earth and Sun.
		objects2D.add(new Planet2D(centerX, centerY, 20, 1.98e30, Colors.YELLOW));
		objects2D.add(new Planet2D(centerX, centerY + 100, 10, 5.97e24, Colors.BLUE));
		objects2D.add(new Planet2D(centerX, centerY + 200, 10, 5.97e24, Colors.GREEN));
		objects2D.get(1).setVelocityX(2);
		objects2D.get(2).setVelocityX(1);

		// Stable orbit.
		// planets.add(new Planet(100, 100, 10, 1e20, Colors.PURPLE));
		// planets.add(new Planet(200, 100, 10, 1e20, Colors.RED));
		// planets.add(new Planet(centerX, centerY, 20, 1e30, Colors.ORANGE));
		// planets.get(1).setVelocityX(2);
		// planets.get(2).setVelocityX(1);

		// Random planets.
		// Random random = new Random();
		// for (int i = 0; i < 100; i++) {
		// 	planets.add(new Planet(random.nextInt(screenWidth), random.nextInt(screenHeight), 5, 1e30, Colors.WHITE));
		// }

		// 3D planets.
		objects3D.add(new Planet3D(0, 0, 0, 20, 1.98e30, Colors.YELLOW, true));
		objects3D.add(new Planet3D(0, 100, 0, 10, 5.97e24, Colors.GREEN, false));
		objects3D.add(new Planet3D(100, 0, 100, 10, 5.97e24, Colors.DARK_GRAY, false));

		objects3D.get(1).setVelocityX(2);
		objects3D.get(2).setVelocityX(2);

		// COOL SUN MOVING EVERYTHING ORBITING IT
		// objects3D.get(0).setVelocityZ(1.5);

		// Scaled down real solar system.
		// Sun
		// objects3D.add(new Planet3D(57.91, 0, 0, 124, 3.30e23, Colors.GRAY)); // Mercury
		// objects3D.add(new Planet3D(108.2, 0, 0, 0.161, 4.87e24, Colors.ORANGE)); // Venus
		// objects3D.add(new Planet3D(149.6, 0, 0, 0.164, 5.97e24, Colors.BLUE)); // Earth
		// objects3D.add(new Planet3D(227.94, 0, 0, 0.034, 6.42e23, Colors.RED)); // Mars
		// objects3D.add(new Planet3D(778.33, 0, 0, 0.699, 1.90e27, Colors.BROWN)); // Jupiter
		// objects3D.add(new Planet3D(1429.4, 0, 0, 0.582, 5.68e26, Colors.YELLOW)); // Saturn
		// objects3D.add(new Planet3D(2870.99, 0, 0, 0.254, 8.68e25, Colors.CYAN)); // Uranus
		// objects3D.add(new Planet3D(4504.3, 0, 0, 0.246, 1.02e26, Colors.BLUE)); // Neptune
		
		// // Set initial velocities for planets (approximate values for circular orbits)
		// for (Body planet : objects3D) {
		// 	if (planet instanceof Planet3D) {
		// 		Planet3D p = (Planet3D) planet;
		// 		p.setVelocityY(1.5); // Set initial velocity for orbit
		// 	}
		// }

		// objects3D.add(new Planet3D(0, 0, 0, 6.96, 1.98e30, Colors.YELLOW)); // Sun

	}

	public void setRenderingMode(int mode) {
		if (mode == 0) renderingMode = 0;
		else renderingMode = 1;
	}
	
	public void initUI() {
		UIManager.put("ToggleButton.select", new Color(0, 0, 225));

		// Set up the frame.
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(true);

		// Make stuff beautiful.
		panel.setBackground(new Color(0, 0, 51));

		cameraSpeedLabel.setForeground(Color.WHITE);		

		lightingCheckBox.setBackground(new Color(0, 0, 51));
		lightingCheckBox.setForeground(Color.WHITE);
		lightingCheckBox.setFocusable(false);
		lightingCheckBox.setBorderPainted(false);
		lightingCheckBox.setSelected(false);

		startButton.setPreferredSize(new Dimension(100, 50));
		startButton.setBorderPainted(false);
		startButton.setFocusable(false);
		startButton.setBackground(new Color(0, 0, 102));
		startButton.setForeground(Color.WHITE);
		
		stopButton.setPreferredSize(new Dimension(100, 50));
		stopButton.setBorderPainted(false);
		stopButton.setFocusable(false);
		stopButton.setBackground(new Color(0, 0, 102));
		stopButton.setForeground(Color.WHITE);

		resetButton.setPreferredSize(new Dimension(100, 50));
		resetButton.setBorderPainted(false);
		resetButton.setFocusable(false);
		resetButton.setBackground(new Color(0, 0, 102));
		resetButton.setForeground(Color.WHITE);

		mode2D.setPreferredSize(new Dimension(100, 50));
		mode2D.setFocusable(false);
		mode2D.setBorderPainted(false);
		mode2D.setBackground(new Color(0, 0, 160));
		mode2D.setForeground(Color.BLACK);

		mode3D.setPreferredSize(new Dimension(100, 50));
		mode3D.setFocusable(false);
		mode3D.setBorderPainted(false);
		mode3D.setBackground(new Color(0, 0, 160));
		mode3D.setForeground(Color.BLACK);

		panel.add(startButton);
		panel.add(stopButton);
		panel.add(resetButton);
		panel.add(mode2D);
		panel.add(mode3D);
		panel.add(lightingCheckBox);
		panel.add(cameraSpeedLabel);

		frame.add(panel);

		lightingCheckBox.addItemListener(e -> {
			if (running) {
				lightingCheckBox.setSelected(false);
				return;
				
			}
			if (lightingCheckBox.isSelected()) {
				lightingEnabled = true;
				System.out.println("Lighting Enabled");

			} else {
				lightingEnabled = false;
				System.out.println("Feature Disabled");

			}
		});

		startButton.addActionListener(e -> {
			start();
			mode2D.setEnabled(false);
			mode3D.setEnabled(false);
		});
		stopButton.addActionListener(e -> {
			stop();
			mode2D.setEnabled(true);
			mode3D.setEnabled(true);
		});
		resetButton.addActionListener(e -> reset());

		mode2D.setSelected(true);
		mode2D.addActionListener(e -> {
			if (mode2D.isSelected()) {
				mode3D.setSelected(false);
				setRenderingMode(0);
			}
		});
		mode3D.addActionListener(e -> {
			if (mode3D.isSelected()) {
				mode2D.setSelected(false);
				setRenderingMode(1);
			}
		});
		
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		PlanetRenderer renderer = new PlanetRenderer();
		renderer.initUI();
	}
}
