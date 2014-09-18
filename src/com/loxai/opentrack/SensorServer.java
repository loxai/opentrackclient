package com.loxai.opentrack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImplFactory;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorServer {

	static final double TO_DEG = 180 / Math.PI;
    private SensorManager mSensorManager;
	boolean reset = false;
	boolean active = false;
	float centeredYaw, centeredPitch, centeredRoll;
	int msPerFrame;
	
	int movingYaw, movingPitch, movingRoll;
	
	DatagramSocket serverSocket = null;
	DataDeliver deliverer = new DataDeliver();
	InetAddress ipAddress;
	int port;
	public SensorServer(SensorManager mSensorManager, String ip, String portStr,int msPerFrame) throws Exception{
		this.msPerFrame = msPerFrame;
		try{
			port = Integer.parseInt(portStr);
		}catch(NumberFormatException nfe){
			Log.e(Consts.TAG, "Invalid port", nfe);
			throw new Exception("Invalid port");
		}
        try {
			ipAddress = InetAddress.getAllByName(ip)[0];
		} catch (UnknownHostException uhe) {
			Log.e(Consts.TAG, "Invalid IP", uhe);
			throw new Exception("Invalid IP");
		}
		try {
			serverSocket = new DatagramSocket(port);
		} catch (SocketException se) {
			Log.e(Consts.TAG, "Failed to create socket", se);
			throw new Exception("Failed to create socket");
		}
        this.mSensorManager = mSensorManager;
        boolean registered = mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
        if (!registered)
        	throw new Exception("Could not register sensor");
	}

	public void begin(){
        active = true;
        reset = true;
        deliverer.start();
	}
	public synchronized void end(){
		if (active){
			active = false;
			serverSocket.disconnect();
			serverSocket.close();
			mSensorManager.unregisterListener(mListener);
			Log.i(Consts.TAG, "Server disconnected");
		}
	}
	class DataDeliver extends Thread{
		float[] data;
		public void run(){
			Log.i(Consts.TAG, "Sensor delivery server started");
			while(active){
				if (data != null){
					long startTime = System.currentTimeMillis();
					deliverDataOpenTrack(data);
					data = null;
					long totalTime = startTime - System.currentTimeMillis();
					if (totalTime < msPerFrame)
						try {
							Thread.sleep(msPerFrame - totalTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
				} else
					try {
						Thread.sleep(2);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
			Log.i(Consts.TAG, "Sensor delivery server stopped");
		}
		public void addData(float[] data){
			synchronized(data){
				this.data = data;
				//queue.addLast(data);
			}
		}
	}
	static byte[] doubleToByte(double d){
		byte[] output = new byte[8];
		long lng = Double.doubleToLongBits(d);
		//for(int i = 0; i < 8; i++) output[i] = (byte)((lng >> ((7 - i) * 8)) & 0xff);
		for(int i = 0; i < 8; i++) output[7 - i] = (byte)((lng >> ((7 - i) * 8)) & 0xff);

		return output;
	}

	void deliverDataOpenTrack(final float[] data){
        if (serverSocket != null && ipAddress != null){
        	//double d = 65.43;
        	//byte[] output = new byte[52];
            byte[] sendData = new byte[52];
            //byte[] empty = new byte[8];
            
            ByteBuffer target = ByteBuffer.wrap(sendData);
            target.put(doubleToByte(data[0]));
            target.put(doubleToByte(data[1]));
            target.put(doubleToByte(data[2]));
            target.put(doubleToByte(data[3]));
            target.put(doubleToByte(data[4]));
            target.put(doubleToByte(data[5]));
            target.put(Consts.OT_END_BYTES);
        	
            sendData = target.array();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
			try {
				//System.out.println("BData to send " + data[0]+" "+data[1]+" "+data[2]);
				serverSocket.send(sendPacket);
			} catch (IOException e) {
				Log.e(Consts.TAG, "Failed to deliver sensor data " + e);
			}
            //System.out.println("Sent " + target);
        }
	}
	public void reset(){
		reset = true;
	}
    private final SensorEventListener mListener = new SensorEventListener() {
        private float[] orientation = new float[3];
        private float[] rotation = new float[16];
		
		public void onSensorChanged(SensorEvent event) {

		    SensorManager.getRotationMatrixFromVector(rotation, event.values.clone());
		    SensorManager.getOrientation(rotation, orientation);
		    
    		if (reset){
    			reset = false;
    			//System.out.println("Before " + centeredValues[0] + " " + centeredValues[1] + " " + centeredValues[2]);
    			centeredYaw = orientation[0];
//    			centeredPitch = orientation[2];
//    			centeredRoll = orientation[1];
    		}
    		
		    orientation[0] -= centeredYaw;

    		//System.out.println((orientation[0] * TO_DEG));
    		float yaw = (float)(orientation[0] * TO_DEG);
		    if (yaw > 180)
		    	yaw -= 360;
    		
		    deliverer.addData(new float[]{0,0,0, yaw, (float)(orientation[1] * TO_DEG), (float)(orientation[2] * TO_DEG)});
		    //deliverer.addData(new float[]{0,0,0, (float)(orientation[1] * TO_DEG), (float)(orientation[2] * TO_DEG), (float)(orientation[0] * TO_DEG)});
		}

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

}
