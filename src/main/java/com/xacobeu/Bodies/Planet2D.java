package com.xacobeu.Bodies;

import static org.lwjgl.opengl.GL11.*;

import com.xacobeu.Colors;

public class Planet2D extends Body {

    protected double radius;
    protected float[] color = {1.0f, 0.0f, 0.0f, 1.0f};

    public Planet2D(double positionX, double positionY, double radius, double mass, float[] color) {
        super(positionX, positionY, mass);

        this.radius = radius;

        if (color.length == 4) {
            this.color = color;
        } else {
            System.out.println("Invalid color array length");
        }
    }

    @Override
    public void resolveCollision(Body p2) {
		double dx = p2.getPositionX() - positionX;
		double dy = p2.getPositionY() - positionY;
		double distance = Math.sqrt(dx * dx + dy * dy);
	
		// Normalize the collision normal
		double nx = dx / distance;
		double ny = dy / distance;
	
		// Relative velocity
		double vx = p2.getVelocityX() - velocityX;
		double vy = p2.getVelocityY() - velocityY;
		
		// Velocity along the normal
		double vn = vx * nx + vy * ny;
		
		if (vn > 0) return; // They are moving apart, no need to resolve
	
		// Compute impulse scalar
		double massSum = mass + p2.getMass();
		double impulse = (2 * vn) / massSum;
	
		// Apply impulse
		setVelocityX(velocityX + impulse * p2.getMass() * nx);
		setVelocityY(velocityX + impulse * p2.getMass() * ny);
		p2.setVelocityX(p2.getVelocityX() - impulse * mass * nx);
		p2.setVelocityY(p2.getVelocityY() - impulse * mass * ny);
	}

    @Override
    public void drawTrail() {
		glBegin(GL_POINTS);
		for (double[] pos : trail) {
			glColor4f(Colors.WHITE[0], Colors.WHITE[1], Colors.WHITE[2], 1.0f);
			glVertex2d(pos[0], pos[1]);
		}
		glEnd();
	}

    @Override
    public void updatePosition() {
        positionX += velocityX;
        positionY += velocityY;

        // Store past positions
        if (trail.size() >= TRAIL_LENGTH) {
            trail.poll(); // Remove oldest position
        }
        trail.add(new double[]{positionX, positionY});
    }

    @Override
    public void draw() {
		glBegin(GL_TRIANGLE_FAN);

		glColor3f(color[0], color[1], color[2]);
		glVertex2d(positionX, positionY);
		for (int i = 0; i <= resolution; i++) {
			double angle = 2.0 * Math.PI * ((double) i / resolution);
			double x = positionX + radius * Math.cos(angle);
			double y = positionY + radius * Math.sin(angle);
			glVertex2d(x, y);
		}

		glEnd();
    }

    public void applyVelocity(double velocityX, double velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    public void checkBorderCollision(int screenWidth, int screenHeight) {
        if (positionX + radius > screenWidth || positionX - radius < 0) {
            velocityX = -velocityX;
        }
        if (positionY + radius > screenHeight || positionY - radius < 0) {
            velocityY = -velocityY;
        }
    }

    public float[] getColor() {
        return color;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}
