package limelight;

import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.networktables.StringArrayPublisher;
import edu.wpi.first.networktables.Publisher;
import edu.wpi.first.networktables.Subscriber;
import edu.wpi.first.networktables.DoubleArraySubscriber;
import edu.wpi.first.networktables.DoubleSubscriber;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.StringSubscriber;
import edu.wpi.first.networktables.StringArraySubscriber;

import java.util.Map;
import java.util.HashMap;

public class NT4Publisher {
    private boolean m_connected = false;
    private Map<String, Object> m_latest_data = new java.util.HashMap<>();
    private NetworkTableInstance m_inst;
    private NetworkTable m_table;
    private String m_name = "limelight";
    private String m_serverIp;
    private int m_serverPort;

    // Cache for our publishers to prevent memory leaks and "maximum number of publishers" errors
    private Map<String, Publisher> m_publishers = new HashMap<>();
    private Map<String, Subscriber> m_subscribers = new HashMap<>();

    public NT4Publisher(String ip, int port) {
        this.m_serverIp = ip;
        this.m_serverPort = port;
        m_inst = NetworkTableInstance.getDefault();
        m_inst.startClient4(m_name);
        m_inst.setServer(ip, port);
        m_table = m_inst.getTable(m_name);
        this.m_connected = true;
        System.out.println("[NT4] Connecting to NT4 server at " + ip + ":" + port + " with name: " + m_name);
        
        initSubscribers();
    }
    
    private void initSubscribers() {
        // Initialize Subscribers for all Limelight Controls (Setters)
        
        // Camera Controls (Setters)
        m_subscribers.put("ledMode", m_table.getDoubleTopic("ledMode").subscribe(0.0));
        m_subscribers.put("camMode", m_table.getDoubleTopic("camMode").subscribe(0.0));
        m_subscribers.put("pipeline", m_table.getDoubleTopic("pipeline").subscribe(0.0));
        m_subscribers.put("stream", m_table.getDoubleTopic("stream").subscribe(0.0));
        m_subscribers.put("snapshot", m_table.getDoubleTopic("snapshot").subscribe(0.0));
        m_subscribers.put("crop", m_table.getDoubleArrayTopic("crop").subscribe(new double[]{0,0,0,0}));
        m_subscribers.put("cropKeystone", m_table.getDoubleArrayTopic("cropKeystone").subscribe(new double[]{0,0}));
        m_subscribers.put("frameskip", m_table.getDoubleTopic("frameskip").subscribe(0.0));
        
        // Video Recording Controls
        m_subscribers.put("rewind_enable", m_table.getDoubleTopic("rewind_enable").subscribe(0.0));
        m_subscribers.put("rewind_capture", m_table.getDoubleArrayTopic("rewind_capture").subscribe(new double[]{0,0}));
        
        // AprilTag and 3D Data (Setters)
        m_subscribers.put("camerapose_robotspace_set", m_table.getDoubleArrayTopic("camerapose_robotspace_set").subscribe(new double[]{0,0,0,0,0,0}));
        m_subscribers.put("priorityid", m_table.getDoubleTopic("priorityid").subscribe(0.0));
        m_subscribers.put("robot_orientation_set", m_table.getDoubleArrayTopic("robot_orientation_set").subscribe(new double[]{0,0,0,0,0,0}));
        m_subscribers.put("fiducial_id_filters_set", m_table.getDoubleArrayTopic("fiducial_id_filters_set").subscribe(new double[0]));
        m_subscribers.put("fiducial_offset_set", m_table.getDoubleArrayTopic("fiducial_offset_set").subscribe(new double[]{0,0,0}));
        m_subscribers.put("fiducial_downscale_set", m_table.getDoubleTopic("fiducial_downscale_set").subscribe(0.0));
        
        // IMU Data
        m_subscribers.put("imu", m_table.getDoubleArrayTopic("imu").subscribe(new double[10]));
        
        // IMU Controls
        m_subscribers.put("imumode_set", m_table.getDoubleTopic("imumode_set").subscribe(0.0));
        m_subscribers.put("imuassistalpha_set", m_table.getDoubleTopic("imuassistalpha_set").subscribe(0.001));
    }

    public void publishData(Map<String, Object> data) {
        this.m_latest_data = data;
        
        // Ensure publishers are created once and reused to avoid memory leaks
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Double) {
                DoublePublisher pub = (DoublePublisher) m_publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return m_inst.getDoubleTopic(k).publish();
                    return m_table.getDoubleTopic(k).publish();
                });
                pub.set((Double) value);
            } else if (value instanceof Integer || value instanceof Long) {
                IntegerPublisher pub = (IntegerPublisher) m_publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return m_inst.getIntegerTopic(k).publish();
                    return m_table.getIntegerTopic(k).publish();
                });
                pub.set(((Number) value).longValue());
            } else if (value instanceof String) {
                StringPublisher pub = (StringPublisher) m_publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return m_inst.getStringTopic(k).publish();
                    return m_table.getStringTopic(k).publish();
                });
                pub.set((String) value);
            } else if (value instanceof double[]) {
                DoubleArrayPublisher pub = (DoubleArrayPublisher) m_publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return m_inst.getDoubleArrayTopic(k).publish();
                    return m_table.getDoubleArrayTopic(k).publish();
                });
                pub.set((double[]) value);
            } else if (value instanceof String[]) {
                StringArrayPublisher pub = (StringArrayPublisher) m_publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return m_inst.getStringArrayTopic(k).publish();
                    return m_table.getStringArrayTopic(k).publish();
                });
                pub.set((String[]) value);
            }
        }
    }

    public boolean isConnected() {
        return m_inst.isConnected();
    }

    public Map<String, Object> getLatestData() {
        return m_latest_data;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        if (!this.m_name.equals(name)) {
            this.m_name = name;
            System.out.println("[NT4] Name updated to: " + name + ", restarting...");
            
            for (Publisher pub : m_publishers.values()) pub.close();
            for (Subscriber sub : m_subscribers.values()) sub.close();
            m_publishers.clear();
            m_subscribers.clear();
            
            m_inst.stopClient();
            m_inst.startClient4(m_name);
            m_inst.setServer(m_serverIp, m_serverPort);
            
            m_table = m_inst.getTable(m_name);
            initSubscribers();
        }
    }
}
