package limelight;

import java.util.*;

public class SimEngine {
    private double[] m_robotPose = {0, 0, 0}; // X, Y, Yaw
    private double[] m_cameraOffset = {0, 0, 0.5, 0, 0, 0}; // X, Y, Z, Yaw, Pitch, Roll
    private List<AprilTag> m_targets = new ArrayList<>();
    private List<Detection> m_detections = new ArrayList<>();
    private List<Classification> m_classifications = new ArrayList<>();
    private List<Barcode> m_barcodes = new ArrayList<>();
    private double[] m_imuData = new double[6]; // [yaw, pitch, roll, rot_yaw, rot_pitch, rot_roll]
    private long m_heartbeat = 0;

    public double[] getRobotPose() { return m_robotPose; }
    public double[] getCameraOffset() { return m_cameraOffset; }
    public double[] getImuData() { return m_imuData; }

    public SimEngine() {
    }

    public static class AprilTag {
        public int id;
        public double x, y, z;

        public AprilTag(int id, double x, double y, double z) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Detection {
        public int classId;
        public String className;
        public double confidence;
        public double tx, ty, ta;

        public Detection(int classId, String className, double confidence, double tx, double ty, double ta) {
            this.classId = classId;
            this.className = className;
            this.confidence = confidence;
            this.tx = tx;
            this.ty = ty;
            this.ta = ta;
        }
    }

    public static class Classification {
        public int classId;
        public String className;
        public double confidence;

        public Classification(int classId, String className, double confidence) {
            this.classId = classId;
            this.className = className;
            this.confidence = confidence;
        }
    }

    public static class Barcode {
        public String family;
        public String data;
        public double tx, ty, ta;

        public Barcode(String family, String data, double tx, double ty, double ta) {
            this.family = family;
            this.data = data;
            this.tx = tx;
            this.ty = ty;
            this.ta = ta;
        }
    }

    public void setRobotPose(double[] pose) {
        this.m_robotPose = pose;
    }

    public void setCameraOffset(double[] offset) {
        this.m_cameraOffset = offset;
    }

    public void addTarget(int id, double x, double y, double z) {
        m_targets.add(new AprilTag(id, x, y, z));
    }

    public void clearTargets() {
        m_targets.clear();
    }

    public void addDetection(int classId, String className, double confidence, double tx, double ty, double ta) {
        m_detections.add(new Detection(classId, className, confidence, tx, ty, ta));
    }

    public void addClassification(int classId, String className, double confidence) {
        m_classifications.add(new Classification(classId, className, confidence));
    }

    public void addBarcode(String family, String data, double tx, double ty, double ta) {
        m_barcodes.add(new Barcode(family, data, tx, ty, ta));
    }

    public void setImuData(double... data) {
        this.m_imuData = data;
    }

    public Map<String, Object> getNtData() {
        m_heartbeat++;
        Map<String, Object> data = new HashMap<>();

        // Robot pose
        double robotX = m_robotPose.length > 0 ? m_robotPose[0] : 0;
        double robotY = m_robotPose.length > 1 ? m_robotPose[1] : 0;
        double robotYaw = m_robotPose.length > 2 ? m_robotPose[2] : 0;

        // Camera offset
        double offsetX = m_cameraOffset.length > 0 ? m_cameraOffset[0] : 0;
        double offsetY = m_cameraOffset.length > 1 ? m_cameraOffset[1] : 0;
        double offsetZ = m_cameraOffset.length > 2 ? m_cameraOffset[2] : 0;
        double offsetYaw = m_cameraOffset.length > 3 ? m_cameraOffset[3] : 0;

        // Compute camera field pose
        double rRad = Math.toRadians(robotYaw);
        double cx = robotX + offsetX * Math.cos(rRad) - offsetY * Math.sin(rRad);
        double cy = robotY + offsetX * Math.sin(rRad) + offsetY * Math.cos(rRad);
        double cz = offsetZ;
        double cyaw = robotYaw + offsetYaw;

        double tx = 0.0, ty = 0.0, ta = 0.0, txnc = 0.0, tync = 0.0, tid = -1.0;
        int targetCount = m_targets.size();
        
        double avgDist = 0.0;
        double tx_rel = 0.0, ty_rel = 0.0, tz_rel = 0.0;

        if (!m_targets.isEmpty()) {
            AprilTag t = m_targets.get(0);
            tid = t.id;
            
            // Assume UI specifies target in FIELD space.
            // Convert to camera-relative space (WPILib: x forward, y left, z up)
            // Delta from camera to target
            double dx = t.x - cx;
            double dy = t.y - cy;
            double dz = t.z - cz;
            
            // Rotate by -yaw to get into camera's local coordinate frame
            double rad = Math.toRadians(-cyaw);
            double local_x = dx * Math.cos(rad) - dy * Math.sin(rad);
            double local_y = dx * Math.sin(rad) + dy * Math.cos(rad);
            double local_z = dz; // assuming no camera pitch/roll for simplicity
            
            tx_rel = local_x;
            ty_rel = local_y;
            tz_rel = local_z;
            
            double dist = Math.sqrt(local_x * local_x + local_y * local_y + local_z * local_z);
            avgDist = dist;
            
            if (local_x > 0.01) {
                // tx is horizontal offset (left/right) -> angle derived from y and x
                tx = Math.toDegrees(Math.atan2(local_y, local_x));
                // ty is vertical offset -> angle derived from z and x
                ty = Math.toDegrees(Math.atan2(local_z, local_x));
                txnc = tx;
                tync = ty;
            }
            if (dist > 0.01) {
                ta = Math.min(100.0, 10.0 / (dist * dist)); // crude area approximation
            }
        } else if (!m_detections.isEmpty()) {
            Detection d = m_detections.get(0);
            tx = d.tx;
            ty = d.ty;
            ta = d.ta;
            txnc = d.tx;
            tync = d.ty;
        }

        // Basic targeting data
        data.put("tv", (m_targets.isEmpty() && m_detections.isEmpty()) ? 0.0 : 1.0);
        data.put("tx", tx);
        data.put("ty", ty);
        data.put("txnc", txnc);
        data.put("tync", tync);
        data.put("ta", ta);
        data.put("tl", 1.5); // fake latency
        data.put("cl", 2.0); // fake capture latency
        
        // t2d array [tv, count, tl, cl, tx, ty, txnc, tync, ta, tid, detect_class, clf_class, ...]
        double detClass = m_detections.isEmpty() ? 0.0 : m_detections.get(0).classId;
        double clfClass = m_classifications.isEmpty() ? 0.0 : m_classifications.get(0).classId;
        data.put("t2d", new double[]{
            m_targets.isEmpty() ? 0.0 : 1.0, 
            targetCount, 
            1.5, 2.0, 
            tx, ty, txnc, tync, ta, tid, 
            detClass, clfClass, 
            10.0, 10.0, 10.0, 10.0, 0.0
        });
        
        data.put("getpipe", 0.0);
        data.put("getpipetype", "pipe_fiducial");
        data.put("json", "{}");
        data.put("tc", new double[]{0, 0, 0});
        data.put("hb", (double) m_heartbeat);
        data.put("hw", new double[]{35.0, 10.0, 50.0, 50.0});
        data.put("crosshairs", new double[]{0, 0, 0, 0});
        data.put("tcclass", m_classifications.isEmpty() ? "" : m_classifications.get(0).className);
        data.put("tdclass", m_detections.isEmpty() ? "" : m_detections.get(0).className);

        double totalLatency = 3.5;

        // 11 elements: [X, Y, Z, Roll, Pitch, Yaw, Latency, TagCount, TagSpan, AvgDist, AvgArea]
        double[] currentPose = new double[]{
            robotX, robotY, 0.0, 
            0.0, 0.0, robotYaw, 
            totalLatency, targetCount, 
            targetCount > 1 ? 0.5 : 0.0, 
            avgDist, ta
        };

        data.put("botpose", currentPose);
        data.put("botpose_wpiblue", currentPose);
        data.put("botpose_wpired", currentPose);
        data.put("botpose_orb", currentPose);
        data.put("botpose_orb_wpiblue", currentPose);
        data.put("botpose_orb_wpired", currentPose);

        // target space offsets
        double[] targetCamSpace = new double[]{tx_rel, ty_rel, tz_rel, 0, 0, 0};
        double[] camTargetSpace = new double[]{-tx_rel, -ty_rel, -tz_rel, 0, 0, 0};
        
        data.put("camerapose_targetspace", camTargetSpace);
        data.put("targetpose_cameraspace", targetCamSpace);
        data.put("targetpose_robotspace", targetCamSpace); // assume cam=robot for now
        data.put("botpose_targetspace", camTargetSpace);
        data.put("camerapose_robotspace", new double[]{0,0,0,0,0,0});
        data.put("stddevs", new double[]{0.01, 0.01, 0.01, 0.1, 0.1, 0.1, 0,0,0,0,0,0});

        data.put("tid", tid);

        data.put("imu", m_imuData != null && m_imuData.length >= 10 ? m_imuData : new double[10]);
        data.put("llpython", new double[0]);

        // Raw Data
        data.put("tcornxy", new double[0]);
        
        // rawtargets [txnc, tync, ta, txnc2, tync2, ta2...]
        double[] rawt = new double[m_targets.size() * 3];
        double[] rawf = new double[m_targets.size() * 7];
        for (int i=0; i<m_targets.size(); i++) {
            AprilTag t = m_targets.get(i);
            
            double dx = t.x - cx;
            double dy = t.y - cy;
            double dz = t.z - cz;
            double rad = Math.toRadians(-cyaw);
            double local_x = dx * Math.cos(rad) - dy * Math.sin(rad);
            double local_y = dx * Math.sin(rad) + dy * Math.cos(rad);
            double local_z = dz;
            
            double dist = Math.sqrt(local_x*local_x + local_y*local_y + local_z*local_z);
            double rx = 0, ry = 0, ra = 0;
            if (local_x > 0.01) {
                rx = Math.toDegrees(Math.atan2(local_y, local_x));
                ry = Math.toDegrees(Math.atan2(local_z, local_x));
                ra = Math.min(100.0, 10.0 / (dist * dist));
            }
            rawt[i*3] = rx;
            rawt[i*3+1] = ry;
            rawt[i*3+2] = ra;
            
            rawf[i*7] = t.id;
            rawf[i*7+1] = rx;
            rawf[i*7+2] = ry;
            rawf[i*7+3] = ra;
            rawf[i*7+4] = dist;
            rawf[i*7+5] = dist;
            rawf[i*7+6] = 0.0;
        }
        data.put("rawtargets", rawt);
        data.put("rawfiducials", rawf);
        
        double[] rawd = new double[m_detections.size() * 12];
        for (int i=0; i<m_detections.size(); i++) {
            Detection d = m_detections.get(i);
            rawd[i*12] = d.classId;
            rawd[i*12+1] = d.tx;
            rawd[i*12+2] = d.ty;
            rawd[i*12+3] = d.ta;
        }
        data.put("rawdetections", rawd);
        
        String[] rawb = new String[m_barcodes.size()];
        for (int i=0; i<m_barcodes.size(); i++) {
            rawb[i] = m_barcodes.get(i).data;
        }
        data.put("rawbarcodes", rawb);

        data.put("/SmartDashboard/limelight_Interface", "http://127.0.0.1:5801");
        data.put("/SmartDashboard/limelight_Stream", "http://127.0.0.1:5800");
        data.put("/SmartDashboard/limelight_PipelineName", "Simulation");

        return data;
    }

    public List<AprilTag> getTargets() {
        return m_targets;
    }

    public List<Detection> getDetections() {
        return m_detections;
    }

    public List<Classification> getClassifications() {
        return m_classifications;
    }

    public List<Barcode> getBarcodes() {
        return m_barcodes;
    }
}
