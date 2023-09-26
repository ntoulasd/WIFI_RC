import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int UDP_SERVER_PORT = 12345;
    private static final String UDP_SERVER_IP = "192.168.0.1"; // Replace with your server IP

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private SeekBar throttleSeekBar;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        throttleSeekBar = findViewById(R.id.throttleSeekBar);
        sendButton = findViewById(R.id.sendButton);

        throttleSeekBar.setMax(100);
        throttleSeekBar.setProgress(0);

        sendButton.setOnClickListener(view -> {
            float throttleValue = throttleSeekBar.getProgress();

            // Send UDP data
            new UDPSender().execute(throttleValue);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);

        float pitch = (float) Math.toDegrees(orientation[1]);
        float roll = (float) Math.toDegrees(orientation[2]);

        Log.d("MainActivity", "Pitch: " + pitch + ", Roll: " + roll);

        // Send UDP data
        new UDPSender().execute(pitch, roll);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private class UDPSender extends AsyncTask<Float, Void, Void> {

        @Override
        protected Void doInBackground(Float... params) {
            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName(UDP_SERVER_IP);

                float pitch = params[0];
                float roll = params[1];

                String message = "Pitch: " + pitch + ", Roll: " + roll;
                byte[] buf = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, UDP_SERVER_PORT);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
