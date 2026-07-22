package com.xacobeu;

import com.xacobeu.Bodies.Body;
import com.xacobeu.Bodies.Planet2D;
import com.xacobeu.Bodies.Planet3D;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class PlanetConfigLoader {

    public static void loadConfig(String filePath, List<Body> objects2D, List<Body> objects3D, double centerX, double centerY) {
        try (InputStream input = new FileInputStream(filePath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            if (data == null) {
                throw new RuntimeException("YAML file is empty");
            }

            // Parse 2D objects
            List<Map<String, Object>> p2dList = (List<Map<String, Object>>) data.get("objects2D");
            if (p2dList != null) {
                for (Map<String, Object> map : p2dList) {
                    double x = parseCoord(map.get("x"), centerX, centerX, centerY);
                    double y = parseCoord(map.get("y"), centerY, centerX, centerY);
                    double radius = getDouble(map.get("radius"));
                    double mass = getDouble(map.get("mass"));
                    float[] color = getColorByName((String) map.get("color"));
                    
                    Planet2D planet = new Planet2D(x, y, radius, mass, color);
                    planet.setVelocityX(getDouble(map.getOrDefault("vx", 0.0)));
                    planet.setVelocityY(getDouble(map.getOrDefault("vy", 0.0)));
                    
                    objects2D.add(planet);
                }
            }

            // Parse 3D objects
            List<Map<String, Object>> p3dList = (List<Map<String, Object>>) data.get("objects3D");
            if (p3dList != null) {
                for (Map<String, Object> map : p3dList) {
                    double x = parseCoord(map.get("x"), 0.0, centerX, centerY);
                    double y = parseCoord(map.get("y"), 0.0, centerX, centerY);
                    double z = parseCoord(map.get("z"), 0.0, centerX, centerY);
                    double radius = getDouble(map.get("radius"));
                    double mass = getDouble(map.get("mass"));
                    float[] color = getColorByName((String) map.get("color"));
                    boolean isSun = (Boolean) map.getOrDefault("isSun", false);

                    Planet3D planet = new Planet3D(x, y, z, radius, mass, color, isSun);
                    planet.setVelocityX(getDouble(map.getOrDefault("vx", 0.0)));
                    planet.setVelocityY(getDouble(map.getOrDefault("vy", 0.0)));
                    planet.setVelocityZ(getDouble(map.getOrDefault("vz", 0.0)));

                    objects3D.add(planet);
                }
            }
            System.out.println("Successfully loaded planet configuration from " + filePath);
        } catch (Exception e) {
            System.err.println("Error loading " + filePath + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration file", e);
        }
    }

    private static float[] getColorByName(String name) {
        if (name == null) return Colors.WHITE;
        try {
            java.lang.reflect.Field field = Colors.class.getField(name.toUpperCase());
            return (float[]) field.get(null);
        } catch (Exception e) {
            System.out.println("Could not resolve color '" + name + "', using WHITE");
            return Colors.WHITE;
        }
    }

    private static double parseCoord(Object value, double center, double centerX, double centerY) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            String s = ((String) value).replaceAll("\\s+", "");
            double base = 0;
            if (s.contains("centerX")) {
                base = centerX;
                s = s.replace("centerX", "");
            } else if (s.contains("centerY")) {
                base = centerY;
                s = s.replace("centerY", "");
            }
            if (s.isEmpty()) {
                return base;
            }
            if (s.startsWith("+")) {
                return base + Double.parseDouble(s.substring(1));
            } else if (s.startsWith("-")) {
                return base - Double.parseDouble(s.substring(1));
            }
            return Double.parseDouble(s);
        }
        return 0.0;
    }

    private static double getDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            return Double.parseDouble((String) obj);
        }
        return 0.0;
    }
}
