package com.xacobeu;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class Camera3D {

	// Camera speeds.
	private final float SLOW_CAMERA_SPEED = 1.0f;
    private final float FAST_CAMERA_SPEED = 15.0f;
	
    private boolean fastCamera = false;
	private float cameraSpeed = 1.0f;
    
    // Camera position
	private float cameraX = 0.0f;
	private float cameraY = 0.0f;
	private float cameraZ = 5.0f;
		
	// Camera front direction
	private float frontX = 0.0f;
	private float frontY = 0.0f;
	private float frontZ = -1.0f;

	// Camera right direction
	private float rightX = 1.0f;
	private float rightY = 0.0f;
	private float rightZ = 0.0f;

    // Up vector
	private final float upX = 0.0f;
	private final float upY = 1.0f;
	private final float upZ = 0.0f;

    // Camera variables
	private float yaw = -90.0f;   // Yaw angle (left/right rotation)
	private float pitch = 0.0f;   // Pitch angle (up/down rotation)
	private float lastX = PlanetRenderer.WIDTH / 2.0f; // Last mouse X position
	private float lastY = PlanetRenderer.HEIGHT / 2.0f; // Last mouse Y position
	private boolean firstMouse = true; // Flag to handle initial mouse movement

    public Camera3D(float cameraX, float cameraY, float cameraZ) {
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
    }

    public void handleMouseInput(double xpos, double ypos) {
		if (firstMouse) {
			lastX = (float) xpos;
			lastY = (float) ypos;
			firstMouse = false;
		}

		float xoffset = (float) xpos - lastX;
		float yoffset = lastY - (float) ypos; // Reversed since y-coordinates go from bottom to top
		lastX = (float) xpos;
		lastY = (float) ypos;

		float sensitivity = 0.1f; // Adjust sensitivity
		xoffset *= sensitivity;
		yoffset *= sensitivity;

		yaw += xoffset;
		pitch += yoffset;

		// Constrain pitch to avoid flipping
		if (pitch > 89.0f) pitch = 89.0f;
		if (pitch < -89.0f) pitch = -89.0f;

		// Calculate the new front vector
		frontX = (float) (Math.cos(Math.toRadians(yaw)) * (float) (Math.cos(Math.toRadians(pitch))));
		frontY = (float) Math.sin(Math.toRadians(pitch));
		frontZ = (float) (Math.sin(Math.toRadians(yaw)) * (float) (Math.cos(Math.toRadians(pitch))));

		// Normalize the front vector
		float frontLength = (float) Math.sqrt(frontX * frontX + frontY * frontY + frontZ * frontZ);
		frontX /= frontLength;
		frontY /= frontLength;
		frontZ /= frontLength;

		// Calculate the right vector
		rightX = frontY * upZ - frontZ * upY;
		rightY = frontZ * upX - frontX * upZ;
		rightZ = frontX * upY - frontY * upX;

		// Normalize the right vector
		float rightLength = (float) Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
		rightX /= rightLength;
		rightY /= rightLength;
		rightZ /= rightLength;
	}

    public FloatBuffer createViewMatrix() {
		// Create the view matrix
		FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);
		
		// Calculate the camera's look-at point
		float lookAtX = cameraX + frontX;
		float lookAtY = cameraY + frontY;
		float lookAtZ = cameraZ + frontZ;

		// Calculate the view matrix using the camera's position, look-at point, and up vector
		float[] viewMatrixArray = calculateLookAtMatrix(cameraX, cameraY, cameraZ, lookAtX, lookAtY, lookAtZ, upX, upY, upZ);
		viewMatrix.put(viewMatrixArray).flip();

		return viewMatrix;
	}

    private float[] calculateLookAtMatrix(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
		// Calculate the forward, right, and up vectors
		float[] forward = {centerX - eyeX, centerY - eyeY, centerZ - eyeZ};
		float forwardLength = (float) Math.sqrt(forward[0] * forward[0] + forward[1] * forward[1] + forward[2] * forward[2]);
		forward[0] /= forwardLength;
		forward[1] /= forwardLength;
		forward[2] /= forwardLength;

		float[] up = {upX, upY, upZ};
		float[] right = {
			forward[1] * up[2] - forward[2] * up[1],
			forward[2] * up[0] - forward[0] * up[2],
			forward[0] * up[1] - forward[1] * up[0]
		};
		float rightLength = (float) Math.sqrt(right[0] * right[0] + right[1] * right[1] + right[2] * right[2]);
		right[0] /= rightLength;
		right[1] /= rightLength;
		right[2] /= rightLength;

		float[] newUp = {
			right[1] * forward[2] - right[2] * forward[1],
			right[2] * forward[0] - right[0] * forward[2],
			right[0] * forward[1] - right[1] * forward[0]
		};

		// Create the view matrix
		return new float[]{
			right[0], newUp[0], -forward[0], 0.0f,
			right[1], newUp[1], -forward[1], 0.0f,
			right[2], newUp[2], -forward[2], 0.0f,
			-(right[0] * eyeX + right[1] * eyeY + right[2] * eyeZ),
			-(newUp[0] * eyeX + newUp[1] * eyeY + newUp[2] * eyeZ),
			forward[0] * eyeX + forward[1] * eyeY + forward[2] * eyeZ,
			1.0f
		};
	}

    public void moveForward() {
        cameraX += frontX * cameraSpeed;
        cameraY += frontY * cameraSpeed;
        cameraZ += frontZ * cameraSpeed;
    }

    public void moveBackward() {
        cameraX -= frontX * cameraSpeed;
        cameraY -= frontY * cameraSpeed;
        cameraZ -= frontZ * cameraSpeed;
    }

    public void moveRight() {
        cameraX += rightX * cameraSpeed;
        cameraY += rightY * cameraSpeed;
        cameraZ += rightZ * cameraSpeed;
    }

    public void moveLeft() {
        cameraX -= rightX * cameraSpeed;
        cameraY -= rightY * cameraSpeed;
        cameraZ -= rightZ * cameraSpeed;
    }

    public void moveUp() {
        cameraY += cameraSpeed;
    }

    public void moveDown() {
        cameraY -= cameraSpeed;
    }

    public boolean isFastCamera() {
        return fastCamera;
    }

    public void toggleFastCamera() {
        fastCamera = !fastCamera;
        cameraSpeed = fastCamera ? FAST_CAMERA_SPEED : SLOW_CAMERA_SPEED;
    }

	public void increaseSpeed() {
		if (cameraSpeed + 1.0f > 100.0f) return;
		cameraSpeed += 1.0f;
	}

	public void decreaseSpeed() {
		if (cameraSpeed - 1.0f < 1.0f) return;
		cameraSpeed -= 1.0f;
	}

	public float getCameraSpeed() {
		return cameraSpeed;
	}
}
