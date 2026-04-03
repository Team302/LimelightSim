package limelight;

import com.sun.net.httpserver.HttpServer;
import limelight.LimelightHttpHandler;
import java.net.InetSocketAddress;
import java.util.Map;

public class LimelightSimulator {
    private static final int[] PORTS_TO_TRY = {5801, 5805, 5809, 8080, 8000};
    private static int m_frameCounter = 0;

    public static void main(String[] args) {
        SimEngine engine = new SimEngine();
        NT4Publisher publisher = new NT4Publisher("localhost", 5810);

        System.out.println("\n========================================");
        System.out.println("Limelight 4 Simulator Starting");
        System.out.println("========================================");

        // Add a shutdown hook to handle Ctrl+C cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Simulator] Shutting down...");
            // The JVM will automatically terminate once this hook completes.
            System.out.println("[Simulator] Exited successfully.");
        }));

        Thread updateThread = new Thread(() -> updateLoop(engine, publisher));
        updateThread.setDaemon(true);
        updateThread.setName("SimulationUpdateThread");
        updateThread.start();

        startHttpServer(engine, publisher);
    }

    
    public String getGreeting() {
        return "Hello from LimelightHttpHandler!";
    }

    private static void updateLoop(SimEngine engine, NT4Publisher publisher) {
        while (true) {
            try {
                Map<String, Object> data = engine.getNtData();
                publisher.publishData(data);
                m_frameCounter++;
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void startHttpServer(SimEngine engine, NT4Publisher publisher) {
        HttpServer server = null;
        int port = 0;

        for (int p : PORTS_TO_TRY) {
            try {
                server = HttpServer.create(new InetSocketAddress("localhost", p), 0);
                port = p;
                System.out.println("[SUCCESS] Bound to port " + port);
                break;
            } catch (Exception e) {
                System.out.println("[PORT] Port " + p + " is restricted or in use. Trying next...");
                e.printStackTrace();
            }
        }

        if (server == null) {
            System.out.println("[ERROR] Could not find an open port. Please close programs using these ports and try again.");
            System.exit(1);
        }

        try {
            server.createContext("/", new LimelightHttpHandler(engine, publisher));
            server.createContext("/api/latest", new LimelightHttpHandler(engine, publisher));
            server.createContext("/api/configure", new LimelightHttpHandler(engine, publisher));
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n========================================");
        System.out.println("Limelight 4 Simulator Started");
        System.out.println("========================================");
        System.out.println("Web interface: http://localhost:" + port);
        System.out.println("API endpoint: http://localhost:" + port + "/api/latest");
        System.out.println("Simulation: 50Hz update rate");
        System.out.println("NT4 Status: " + (publisher.isConnected() ? "Connected" : "Mock Mode (Standalone)"));
        System.out.println("========================================\n");
    }
}
