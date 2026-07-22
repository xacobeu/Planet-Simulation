package com.xacobeu;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;

import com.xacobeu.Bodies.Body;
import com.xacobeu.Bodies.Planet2D;
import com.xacobeu.Bodies.Planet3D;

import java.util.ArrayList;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.type.ImBoolean;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

public class PlanetRenderer {
	enum Dimensions {
		TWO_D,
		THREE_D
	}

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
	private Camera3D camera = new Camera3D(0, 100, 300);

	private boolean running = false;
	private boolean isSimulating = false;
	
	// Rendering mode.
	private Dimensions renderingMode = Dimensions.TWO_D;
	private static boolean lightingEnabled = true;

	private boolean[] keyStates = new boolean[GLFW_KEY_LAST + 1];

	private ImGuiImplGlfw imGuiGlfw;
	private ImGuiImplGl3 imGuiGl3;

	public PlanetRenderer() {
		initialiseObjects();
	}

	public void start() {
		System.out.println("Starting simulation");
		isSimulating = true;
	}

	public void stop() {
		System.out.println("Stopping simulation");
		isSimulating = false;
	}

	public void reset() {
		System.out.println("Resetting simulation");
		objects2D.clear();
		objects3D.clear();
		initialiseObjects();
	}

	public void run() {
		running = true;
		init();
		render();
		
		imGuiGl3.dispose();
		imGuiGlfw.dispose();
		ImGui.destroyContext();

		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
	}

	private void init() {
		System.out.println("Initialising simulation");

		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(WIDTH, HEIGHT, "Planet simulation", NULL, NULL);
		if (window == NULL) {
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
		glfwSwapInterval(1);
		glfwShowWindow(window);

		// Set up OpenGL context.
		GL.createCapabilities();

		System.out.println("OpenGL " + glGetString(GL_VERSION) + " initialised.");

		// Setup GLFW Callbacks before ImGui initialization
		glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				// Only rotate camera if right mouse button is pressed and mouse is not captured by ImGui
				if (!ImGui.getIO().getWantCaptureMouse() && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
					camera.handleMouseInput(xpos, ypos);
				} else {
					camera.setFirstMouse(true);
				}
			}
		});

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

		// Initialize ImGui
		ImGui.createContext();
		imGuiGlfw = new ImGuiImplGlfw();
		imGuiGl3 = new ImGuiImplGl3();
		imGuiGlfw.init(window, true);
		imGuiGl3.init("#version 130");
		ImGui.getIO().setFontGlobalScale(2.0f);

		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.1f, 1.0f);
		System.out.println("init complete");
	}

	private void handleKeyboardInput() {
		if (ImGui.getIO().getWantCaptureKeyboard()) {
			java.util.Arrays.fill(keyStates, false);
			return;
		}

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
		if (keyStates[GLFW_KEY_DOWN]) { // Decrease speed
			camera.decreaseSpeed();
		}
		if (keyStates[GLFW_KEY_UP]) { // Increase speed
			camera.increaseSpeed();
		}
	}

	private void render() {
		System.out.println("Starting rendering loop");

		int[] fWidth = new int[1];
		int[] fHeight = new int[1];

		while (running && !glfwWindowShouldClose(window)) {
			// Query dynamic window dimension to adjust viewport and projection matrices
			glfwGetFramebufferSize(window, fWidth, fHeight);
			int w = fWidth[0];
			int h = fHeight[0];
			glViewport(0, 0, w, h);

			// Start ImGui frame
			imGuiGlfw.newFrame();
			ImGui.newFrame();

			// Handle physics calculations only if simulating
			if (isSimulating) {
				if (renderingMode == Dimensions.TWO_D) {
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
					}
				} else if (renderingMode == Dimensions.THREE_D) {
					// Update 3D physics.
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
					}
				}
			}

			// Clear buffers and apply rendering mode configurations
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			if (renderingMode == Dimensions.TWO_D) {
				glDisable(GL_DEPTH_TEST);
				glDisable(GL_LIGHTING);

				glMatrixMode(GL_PROJECTION);
				glLoadIdentity();
				glOrtho(0, w, h, 0, -1, 1);

				glMatrixMode(GL_MODELVIEW);
				glLoadIdentity();

				// Draw 2D planets and trails
				for (Body p : objects2D) {
					p.draw();
					p.drawTrail();
					((Planet2D) p).checkBorderCollision(w, h);
				}
			} else if (renderingMode == Dimensions.THREE_D) {
				glEnable(GL_DEPTH_TEST);
				glDepthFunc(GL_LESS);

				if (lightingEnabled) {
					glEnable(GL_LIGHTING);
					glEnable(GL_LIGHT0);
					glEnable(GL_NORMALIZE);

					float[] lightAmbient = {0.2f, 0.2f, 0.2f, 1.0f};
					float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};
					float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};
					glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
					glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
					glLightfv(GL_LIGHT0, GL_SPECULAR, lightSpecular);

					if (!objects3D.isEmpty()) {
						float[] lightPosition = {(float) objects3D.get(0).getPositionX(), (float) objects3D.get(0).getPositionY(), (float) objects3D.get(0).getPositionZ(), 1.0f};
						glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
					}
				} else {
					glDisable(GL_LIGHTING);
				}

				glMatrixMode(GL_PROJECTION);
				glLoadIdentity();
				float aspect = (float) w / (h == 0 ? 1 : h);
				float fov = 90.0f;
				float zNear = 0.1f;
				float zFar = 100000.0f;
				float top = (float) Math.tan(Math.toRadians(fov / 2.0)) * zNear;
				float right = top * aspect;
				glFrustum(-right, right, -top, top, zNear, zFar);

				glMatrixMode(GL_MODELVIEW);
				glLoadIdentity();

				// Load the view matrix
				FloatBuffer viewMatrix = camera.createViewMatrix();
				glLoadMatrixf(viewMatrix);

				// Draw 3D planets and trails
				for (Body p : objects3D) {
					p.draw();
					p.drawTrail();
				}
			}

			// Process camera inputs
			handleKeyboardInput();

			// Construct control panel GUI overlay
			ImGui.setNextWindowPos(10, 10);
			ImGui.setNextWindowSize(340, 240);
			ImGui.begin("Control Panel");

			if (isSimulating) {
				if (ImGui.button("Stop Simulation")) {
					isSimulating = false;
				}
			} else {
				if (ImGui.button("Start Simulation")) {
					isSimulating = true;
				}
			}

			ImGui.sameLine();
			if (ImGui.button("Reset")) {
				reset();
			}

			ImGui.separator();

			// 2D / 3D Mode Selector
			if (ImGui.radioButton("2D Mode", renderingMode == Dimensions.TWO_D)) {
				renderingMode = Dimensions.TWO_D;
			}
			ImGui.sameLine();
			if (ImGui.radioButton("3D Mode", renderingMode == Dimensions.THREE_D)) {
				renderingMode = Dimensions.THREE_D;
			}

			ImGui.separator();

			// Lighting option (only relevant in 3D mode)
			if (renderingMode == Dimensions.THREE_D) {
				ImBoolean lightVal = new ImBoolean(lightingEnabled);
				if (ImGui.checkbox("Enable Lighting", lightVal)) {
					lightingEnabled = lightVal.get();
				}
			} else {
				ImGui.textDisabled("Lighting (3D only)");
			}

			ImGui.separator();

			// Camera Speed details (only relevant in 3D mode)
			if (renderingMode == Dimensions.THREE_D) {
				ImGui.text("Camera Speed: " + camera.getCameraSpeed());
				ImGui.text("Press UP/DOWN to adjust speed");
				ImGui.text("Right-click & drag to rotate camera");
			} else {
				ImGui.textDisabled("Camera Controls (3D only)");
			}

			ImGui.end();

			// Render ImGui overlay to the screen
			ImGui.render();
			imGuiGl3.renderDrawData(ImGui.getDrawData());

			glfwSwapBuffers(window);
			glfwPollEvents();
		}
	}

	public static boolean getLightingEnabled() {
		return lightingEnabled;
	}

	public void initialiseObjects() {
		PlanetConfigLoader.loadConfig("planets.yaml", objects2D, objects3D, centerX, centerY);
	}

	public static void main(String[] args) {
		PlanetRenderer renderer = new PlanetRenderer();
		renderer.run();
	}
}
