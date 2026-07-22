package com.xacobeu.Bodies;

import java.util.LinkedList;
import java.util.Queue;

abstract public class Body {
    protected double positionX;
    protected double positionY;
    protected double positionZ;

    protected double velocityX;
    protected double velocityY;
    protected double velocityZ;
    protected double mass;

    protected Queue<double[]> trail = new LinkedList<>();
    protected static final int TRAIL_LENGTH = 10000;
    protected static final int resolution = 100;

    public Body(double positionX, double positionY, double mass) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.mass = mass;
    }

    public abstract void updatePosition();
    public abstract void drawTrail();
    public abstract void draw();
    public abstract void resolveCollision(Body other);

    // Accessor methods.

    public double getMass() {
        return mass;
    }

    public double getPositionX() {
        return positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public double getPositionZ() {
        return positionZ;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    // Mutator methods.
    
    public void setMass(double mass) {
        this.mass = mass;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public void setPositionZ(double positionZ) {
        this.positionZ = positionZ;
    }

    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public void setVelocityZ(double velocityZ) {
        this.velocityZ = velocityZ;
    }

    public Queue<double[]> getTrail() {
        return trail;
    }
}
