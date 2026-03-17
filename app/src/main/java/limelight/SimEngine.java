package limelight;

import java.util.*;

public class SimEngine {
    private double[] robotPose = {0, 0, 0}; // X, Y, Yaw
    private double[] cameraOffset = {0, 0, 0.5, 0, 0, 0}; // X, Y, Z, Yaw, Pitch, Roll
    private List<AprilTag> targets = new ArrayList<>();
    private List<Detection> detections = new ArrayList<>();
    private List<Classification> classifications = new ArrayList<>();
    private List<Barcode> barcodes = new ArrayList<>();
    private double[] imuData = new double[6]; // [yaw, pitch, roll, rot_yaw, rot_pitch, rot_roll]
    private long heartbeat = 0;

    public double[] getRobotPose() { return robotPose; }
    public double[] getCameraOffset() { return cameraOffset; }
    public double[] getImuData() { return imuData; }

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
        this.robotPose = pose;
    }

    public void setCameraOffset(double[] offset) {
        this.cameraOffset = offset;
    }

    public void addTarget(int id, double x, double y, double z) {
        targets.add(new AprilTag(id, x, y, z));
    }

    public void clearTargets() {
        targets.clear();
    }

    public void addDetection(int classId, String className, double confidence, double tx, double ty, double ta) {
        detections.add(new Detection(classId, className, confidence, tx, ty, ta));
    }

    public void addClassification(int classId, String className, double confidence) {
        classifications.add(new Classification(classId, className, confidence));
    }

    public void addBarcode(String family, String data, double tx, double ty, double ta) {
        barcodes.add(new Barcode(family, data, tx, ty, ta));
    }

    public void setImuData(double... data) {
        this.imuData = data;
    }

    public Map<String, Object> getNtData() {
        heartbeat++;
        Map<String, Object> data = new HashMap<>();

        // Robot pose
        double robotX = robotPose.length > 0 ? robotPose[0] : 0;
        double robotY = robotPose.length > 1 ? robotPose[1] : 0;
        double robotYaw = robotPose.length > 2 ? robotPose[2] : 0;

        // Camera offset
        double offsetX = cameraOffset.length > 0 ? cameraOffset[0] : 0;
        double offsetY = cameraOffset.length > 1 ? cameraOffset[1] : 0;
        double offsetZ = cameraOffset.length > 2 ? cameraOffset[2] : 0;
        double offsetYaw = cameraOffset.length > 3 ? cameraOffset[3] : 0;

        // Compute camera field pose
        double rRad = Math.toRadians(robotYaw);
        double cx = robotX + offsetX * Math.cos(rRad) - offsetY * Math.sin(rRad);
        double cy = robotY + offsetX * Math.sin(rRad) + offsetY * Math.cos(rRad);
        double cz = offsetZ;
        double cyaw = robotYaw + offsetYaw;

        double tx = 0.0, ty = 0.0, ta = 0.0, txnc = 0.0, tync = 0.0, tid = -1.0;
        int targetCount = targets.size();
        
        double avgDist = 0.0;
        double tx_rel = 0.0, ty_rel = 0.0, tz_rel = 0.0;

        if (!targets.isEmpty()) {
            AprilTag t = targets.get(0);
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
        } else if (!detections.isEmpty()) {
            Detection d = detections.get(0);
            tx = d.tx;
            ty = d.ty;
            ta = d.ta;
            txnc = d.tx;
            tync = d.ty;
        }

        // Basic targeting data
        data.put("tv", (targets.isEmpty() && detections.isEmpty()) ? 0.0 : 1.0);
        data.put("tx", tx);
        data.put("ty", ty);
        data.put("txnc", txnc);
        data.put("tync", tync);
        data.put("ta", ta);
        data.put("tl", 1.5); // fake latency
        data.put("cl", 2.0); // fake capture latency
        
        // t2d array [tv, count, tl, cl, tx, ty, txnc, tync, ta, tid, detect_class, clf_class, ...]
        double detClass = detections.isEmpty() ? 0.0 : detections.get(0).classId;
        double clfClass = classifications.isEmpty() ? 0.0 : classifications.get(0).classId;
        data.put("t2d", new double[]{
            targets.isEmpty() ? 0.0 : 1.0, 
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
        data.put("hb", (double) heartbeat);
        data.put("hw", new double[]{35.0, 10.0, 50.0, 50.0});
        data.put("crosshairs", new double[]{0, 0, 0, 0});
        data.put("tcclass", classifications.isEmpty() ? "" : classifications.get(0).className);
        data.put("tdclass", detections.isEmpty() ? "" : detections.get(0).className);

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

        data.put("imu", imuData != null && imuData.length >= 10 ? imuData : new double[10]);
        data.put("llpython", new double[0]);

        // Raw Data
        data.put("tcornxy", new double[0]);
        
        // rawtargets [txnc, tync, ta, txnc2, tync2, ta2...]
        double[] rawt = new double[targets.size() * 3];
        double[] rawf = new double[targets.size() * 7];
        for (int i=0; i<targets.size(); i++) {
            AprilTag t = targets.get(i);
            
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
        
        double[] rawd = new double[detections.size() * 12];
        for (int i=0; i<detections.size(); i++) {
            Detection d = detections.get(i);
            rawd[i*12] = d.classId;
            rawd[i*12+1] = d.tx;
            rawd[i*12+2] = d.ty;
            rawd[i*12+3] = d.ta;
        }
        data.put("rawdetections", rawd);
        
        String[] rawb = new String[barcodes.size()];
        for (int i=0; i<barcodes.size(); i++) {
            rawb[i] = barcodes.get(i).data;
        }
        data.put("rawbarcodes", rawb);

        data.put("/SmartDashboard/limelight_Interface", "http://127.0.0.1:5801");
        data.put("/SmartDashboard/limelight_Stream", "http://127.0.0.1:5800");
        data.put("/SmartDashboard/limelight_PipelineName", "Simulation");

        return data;
    }

    public List<AprilTag> getTargets() {
        return targets;
    }

    public List<Detection> getDetections() {
        return detections;
    }

    public List<Classification> getClassifications() {
        return classifications;
    }

    public List<Barcode> getBarcodes() {
        return barcodes;
    }
}
