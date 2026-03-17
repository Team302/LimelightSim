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
    private boolean connected = false;
    private Map<String, Object> latest_data = new java.util.HashMap<>();
    private NetworkTableInstance inst;
    private NetworkTable table;

    // Cache for our publishers to prevent memory leaks and "maximum number of publishers" errors
    private Map<String, Publisher> publishers = new HashMap<>();
    private Map<String, Subscriber> subscribers = new HashMap<>();

    public NT4Publisher(String ip, int port) {
        inst = NetworkTableInstance.getDefault();
        inst.startClient4("LimelightSimulator");
        inst.setServer(ip, port);
        table = inst.getTable("limelight");
        this.connected = true;
        System.out.println("[NT4] Connecting to NT4 server at " + ip + ":" + port);
        
        // Initialize Subscribers for all Limelight Controls
        // Basic Targeting Data
        subscribers.put("tv", table.getDoubleTopic("tv").subscribe(0.0));
        subscribers.put("tx", table.getDoubleTopic("tx").subscribe(0.0));
        subscribers.put("ty", table.getDoubleTopic("ty").subscribe(0.0));
        subscribers.put("txnc", table.getDoubleTopic("txnc").subscribe(0.0));
        subscribers.put("tync", table.getDoubleTopic("tync").subscribe(0.0));
        subscribers.put("ta", table.getDoubleTopic("ta").subscribe(0.0));
        subscribers.put("tl", table.getDoubleTopic("tl").subscribe(0.0));
        subscribers.put("cl", table.getDoubleTopic("cl").subscribe(0.0));
        subscribers.put("t2d", table.getDoubleArrayTopic("t2d").subscribe(new double[0]));
        subscribers.put("getpipe", table.getDoubleTopic("getpipe").subscribe(0.0));
        subscribers.put("getpipetype", table.getStringTopic("getpipetype").subscribe(""));
        subscribers.put("json", table.getStringTopic("json").subscribe(""));
        subscribers.put("tc", table.getDoubleArrayTopic("tc").subscribe(new double[]{0,0,0}));
        subscribers.put("hb", table.getDoubleTopic("hb").subscribe(0.0));
        subscribers.put("hw", table.getDoubleArrayTopic("hw").subscribe(new double[]{0,0,0,0}));
        subscribers.put("crosshairs", table.getDoubleArrayTopic("crosshairs").subscribe(new double[]{0,0,0,0}));
        subscribers.put("tcclass", table.getStringTopic("tcclass").subscribe(""));
        subscribers.put("tdclass", table.getStringTopic("tdclass").subscribe(""));
        
        // AprilTag and 3D Data
        subscribers.put("botpose", table.getDoubleArrayTopic("botpose").subscribe(new double[11]));
        subscribers.put("botpose_wpiblue", table.getDoubleArrayTopic("botpose_wpiblue").subscribe(new double[11]));
        subscribers.put("botpose_wpired", table.getDoubleArrayTopic("botpose_wpired").subscribe(new double[11]));
        subscribers.put("botpose_orb", table.getDoubleArrayTopic("botpose_orb").subscribe(new double[11]));
        subscribers.put("botpose_orb_wpiblue", table.getDoubleArrayTopic("botpose_orb_wpiblue").subscribe(new double[11]));
        subscribers.put("botpose_orb_wpired", table.getDoubleArrayTopic("botpose_orb_wpired").subscribe(new double[11]));
        subscribers.put("camerapose_targetspace", table.getDoubleArrayTopic("camerapose_targetspace").subscribe(new double[6]));
        subscribers.put("targetpose_cameraspace", table.getDoubleArrayTopic("targetpose_cameraspace").subscribe(new double[6]));
        subscribers.put("targetpose_robotspace", table.getDoubleArrayTopic("targetpose_robotspace").subscribe(new double[6]));
        subscribers.put("botpose_targetspace", table.getDoubleArrayTopic("botpose_targetspace").subscribe(new double[6]));
        subscribers.put("camerapose_robotspace", table.getDoubleArrayTopic("camerapose_robotspace").subscribe(new double[6]));
        subscribers.put("tid", table.getDoubleTopic("tid").subscribe(-1.0));
        subscribers.put("stddevs", table.getDoubleArrayTopic("stddevs").subscribe(new double[12]));
        
        // Camera Controls (Setters)
        subscribers.put("ledMode", table.getDoubleTopic("ledMode").subscribe(0.0));
        subscribers.put("camMode", table.getDoubleTopic("camMode").subscribe(0.0));
        subscribers.put("pipeline", table.getDoubleTopic("pipeline").subscribe(0.0));
        subscribers.put("stream", table.getDoubleTopic("stream").subscribe(0.0));
        subscribers.put("snapshot", table.getDoubleTopic("snapshot").subscribe(0.0));
        subscribers.put("crop", table.getDoubleArrayTopic("crop").subscribe(new double[]{0,0,0,0}));
        subscribers.put("cropKeystone", table.getDoubleArrayTopic("cropKeystone").subscribe(new double[]{0,0}));
        subscribers.put("frameskip", table.getDoubleTopic("frameskip").subscribe(0.0));
        
        // Video Recording Controls
        subscribers.put("rewind_enable", table.getDoubleTopic("rewind_enable").subscribe(0.0));
        subscribers.put("rewind_capture", table.getDoubleArrayTopic("rewind_capture").subscribe(new double[]{0,0}));
        
        // AprilTag and 3D Data (Setters)
        subscribers.put("camerapose_robotspace_set", table.getDoubleArrayTopic("camerapose_robotspace_set").subscribe(new double[]{0,0,0,0,0,0}));
        subscribers.put("priorityid", table.getDoubleTopic("priorityid").subscribe(0.0));
        subscribers.put("robot_orientation_set", table.getDoubleArrayTopic("robot_orientation_set").subscribe(new double[]{0,0,0,0,0,0}));
        subscribers.put("fiducial_id_filters_set", table.getDoubleArrayTopic("fiducial_id_filters_set").subscribe(new double[0]));
        subscribers.put("fiducial_offset_set", table.getDoubleArrayTopic("fiducial_offset_set").subscribe(new double[]{0,0,0}));
        subscribers.put("fiducial_downscale_set", table.getDoubleTopic("fiducial_downscale_set").subscribe(0.0));
        
        // IMU Data
        subscribers.put("imu", table.getDoubleArrayTopic("imu").subscribe(new double[10]));
        
        // IMU Controls
        subscribers.put("imumode_set", table.getDoubleTopic("imumode_set").subscribe(0.0));
        subscribers.put("imuassistalpha_set", table.getDoubleTopic("imuassistalpha_set").subscribe(0.001));
        
        // Python
        subscribers.put("llpython", table.getDoubleArrayTopic("llpython").subscribe(new double[0]));
        subscribers.put("llrobot", table.getDoubleArrayTopic("llrobot").subscribe(new double[0]));
        
        // Raw Data
        subscribers.put("tcornxy", table.getDoubleArrayTopic("tcornxy").subscribe(new double[0]));
        subscribers.put("rawtargets", table.getDoubleArrayTopic("rawtargets").subscribe(new double[0]));
        subscribers.put("rawfiducials", table.getDoubleArrayTopic("rawfiducials").subscribe(new double[0]));
        subscribers.put("rawdetections", table.getDoubleArrayTopic("rawdetections").subscribe(new double[0]));
        subscribers.put("rawbarcodes", table.getStringArrayTopic("rawbarcodes").subscribe(new String[0]));
    }

    public void publishData(Map<String, Object> data) {
        this.latest_data = data;
        
        // Ensure publishers are created once and reused to avoid memory leaks
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Double) {
                DoublePublisher pub = (DoublePublisher) publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return inst.getDoubleTopic(k).publish();
                    return table.getDoubleTopic(k).publish();
                });
                pub.set((Double) value);
            } else if (value instanceof Integer) {
                IntegerPublisher pub = (IntegerPublisher) publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return inst.getIntegerTopic(k).publish();
                    return table.getIntegerTopic(k).publish();
                });
                pub.set((Integer) value);
            } else if (value instanceof String) {
                StringPublisher pub = (StringPublisher) publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return inst.getStringTopic(k).publish();
                    return table.getStringTopic(k).publish();
                });
                pub.set((String) value);
            } else if (value instanceof double[]) {
                DoubleArrayPublisher pub = (DoubleArrayPublisher) publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return inst.getDoubleArrayTopic(k).publish();
                    return table.getDoubleArrayTopic(k).publish();
                });
                pub.set((double[]) value);
            } else if (value instanceof String[]) {
                StringArrayPublisher pub = (StringArrayPublisher) publishers.computeIfAbsent(key, k -> {
                    if (k.startsWith("/")) return inst.getStringArrayTopic(k).publish();
                    return table.getStringArrayTopic(k).publish();
                });
                pub.set((String[]) value);
            }
        }
    }

    public boolean isConnected() {
        return inst.isConnected();
    }

    public Map<String, Object> getLatestData() {
        return latest_data;
    }
}
