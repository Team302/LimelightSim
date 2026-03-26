package limelight;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import java.io.*;
import java.util.Map;

public class LimelightHttpHandler implements HttpHandler {
    private SimEngine m_engine;
    private NT4Publisher m_publisher;
    private Gson m_gson = new Gson();

    public LimelightHttpHandler(SimEngine engine, NT4Publisher publisher) {
        this.m_engine = engine;
        this.m_publisher = publisher;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        // Add CORS headers
        addCorsHeaders(exchange);

        if ("GET".equals(exchange.getRequestMethod())) {
            if ("/".equals(path)) {
                handleGetRoot(exchange);
            } else if ("/api/latest".equals(path)) {
                handleGetLatest(exchange);
            } else if ("/api/config".equals(path)) {
                handleGetConfig(exchange);
            } else {
                sendResponse(exchange, 404, "Not Found");
            }
        } else if ("POST".equals(exchange.getRequestMethod())) {
            if ("/api/configure".equals(path)) {
                handlePostConfigure(exchange);
            } else {
                sendResponse(exchange, 404, "Not Found");
            }
        } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
            // Handle preflight requests
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        }
    }
    
    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }

    private void handleGetRoot(HttpExchange exchange) throws IOException {
        try {
            // Try to load from classpath first (works in JAR)
            InputStream is = getClass().getClassLoader().getResourceAsStream("index.html");
            if (is == null) {
                // Fallback to filesystem (works in development)
                is = new java.io.FileInputStream("src/main/resources/index.html");
            }
            byte[] content = is.readAllBytes();
            is.close();
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        } catch (Exception e) {
            sendResponse(exchange, 404, "index.html not found");
        }
    }

    private void handleGetLatest(HttpExchange exchange) throws IOException {
        Map<String, Object> data = m_engine.getNtData();
        String json = m_gson.toJson(data);
        sendJsonResponse(exchange, 200, json);
    }

    private void handleGetConfig(HttpExchange exchange) throws IOException {
        Map<String, Object> config = new java.util.HashMap<>();
        config.put("robot_pose", m_engine.getRobotPose());
        config.put("camera_offset", m_engine.getCameraOffset());
        config.put("targets", m_engine.getTargets());
        config.put("detections", m_engine.getDetections());
        config.put("classifications", m_engine.getClassifications());
        config.put("barcodes", m_engine.getBarcodes());
        config.put("imu", m_engine.getImuData());
        config.put("name", m_publisher.getName());
        
        String json = m_gson.toJson(config);
        sendJsonResponse(exchange, 200, json);
    }

    private void handlePostConfigure(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = exchange.getRequestBody()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len));
            }
        }

        try {
            Map<String, Object> config = m_gson.fromJson(sb.toString(), Map.class);

            if (config.containsKey("robot_pose")) {
                Object pose = config.get("robot_pose");
                if (pose instanceof java.util.List) {
                    java.util.List<?> poseList = (java.util.List<?>) pose;
                    double[] robotPose = new double[poseList.size()];
                    for (int i = 0; i < poseList.size(); i++) {
                        robotPose[i] = ((Number) poseList.get(i)).doubleValue();
                    }
                    m_engine.setRobotPose(robotPose);
                }
            }

            if (config.containsKey("camera_offset")) {
                Object offset = config.get("camera_offset");
                if (offset instanceof java.util.List) {
                    java.util.List<?> offsetList = (java.util.List<?>) offset;
                    double[] cameraOffset = new double[offsetList.size()];
                    for (int i = 0; i < offsetList.size(); i++) {
                        cameraOffset[i] = ((Number) offsetList.get(i)).doubleValue();
                    }
                    m_engine.setCameraOffset(cameraOffset);
                }
            }

            m_engine.clearTargets();
            if (config.containsKey("targets")) {
                java.util.List<?> targets = (java.util.List<?>) config.get("targets");
                for (Object t : targets) {
                    Map<String, Object> target = (Map<String, Object>) t;
                    m_engine.addTarget(
                            ((Number) target.get("id")).intValue(),
                            ((Number) target.get("x")).doubleValue(),
                            ((Number) target.get("y")).doubleValue(),
                            ((Number) target.get("z")).doubleValue()
                    );
                }
            }

            m_engine.getDetections().clear();
            if (config.containsKey("detections")) {
                java.util.List<?> detections = (java.util.List<?>) config.get("detections");
                for (Object d : detections) {
                    Map<String, Object> det = (Map<String, Object>) d;
                    m_engine.addDetection(
                            ((Number) det.get("classId")).intValue(),
                            (String) det.get("className"),
                            ((Number) det.get("confidence")).doubleValue(),
                            ((Number) det.get("tx")).doubleValue(),
                            ((Number) det.get("ty")).doubleValue(),
                            ((Number) det.getOrDefault("ta", 0.0)).doubleValue()
                    );
                }
            }

            m_engine.getClassifications().clear();
            if (config.containsKey("classifications")) {
                java.util.List<?> classifications = (java.util.List<?>) config.get("classifications");
                for (Object c : classifications) {
                    Map<String, Object> clf = (Map<String, Object>) c;
                    m_engine.addClassification(
                            ((Number) clf.get("classId")).intValue(),
                            (String) clf.get("className"),
                            ((Number) clf.get("confidence")).doubleValue()
                    );
                }
            }

            m_engine.getBarcodes().clear();
            if (config.containsKey("barcodes")) {
                java.util.List<?> barcodes = (java.util.List<?>) config.get("barcodes");
                for (Object b : barcodes) {
                    Map<String, Object> bc = (Map<String, Object>) b;
                    m_engine.addBarcode(
                            (String) bc.get("family"),
                            (String) bc.get("data"),
                            ((Number) bc.get("tx")).doubleValue(),
                            ((Number) bc.get("ty")).doubleValue(),
                            ((Number) bc.getOrDefault("ta", 0.0)).doubleValue()
                    );
                }
            }

            if (config.containsKey("imu")) {
                java.util.List<?> imuList = (java.util.List<?>) config.get("imu");
                double[] imu = new double[imuList.size()];
                for (int i = 0; i < imuList.size(); i++) {
                    imu[i] = ((Number) imuList.get(i)).doubleValue();
                }
                m_engine.setImuData(imu);
            }

            if (config.containsKey("name")) {
                String name = (String) config.get("name");
                m_publisher.setName(name);
            }

            sendJsonResponse(exchange, 200, "{\"status\":\"ok\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 400, "Invalid request");
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
