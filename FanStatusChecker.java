import tinyb.*;
import java.util.List;
import java.io.IOException;

public class FanStatusChecker {
    private BluetoothDevice device;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;

    public FanStatusChecker(String address) throws InterruptedException, IOException {
        BluetoothManager manager = BluetoothManager.getBluetoothManager();
        
        // Start discovery only if the device is not found
        List<BluetoothDevice> devices = manager.getDevices();
        if (devices.isEmpty()) {
            manager.startDiscovery();
            while ((devices = manager.getDevices()).isEmpty()) {
                Thread.sleep(500);
            }
            manager.stopDiscovery(); // Stop discovery once devices are found
        }

        for (BluetoothDevice dev : devices) {
            if (dev.getAddress().equals(address)) {
                device = dev;
                break;
            }
        }

        if (device == null) {
            throw new IOException("Device not found");
        }

        // Ensure proper disconnection before attempting to connect
        if (device.getConnected()) {
            device.disconnect();
        }

        // Check if the device is already connected
        if (!device.getConnected()) {
            // Retry logic for connecting to the device with a shorter delay
            boolean connected = false;
            int retries = 10;
            while (retries > 0 && !connected) {
                try {
                    device.connect();
                    connected = true;
                } catch (BluetoothException e) {
                    System.err.println("Connection failed, retrying... (" + retries + " retries left)");
                    retries--;
                    try {
                        Thread.sleep(200); // Wait for 0.2 seconds before retrying
                    } catch (InterruptedException ie) {
                        System.err.println("Thread sleep interrupted: " + ie.getMessage());
                    }
                }
            }

            if (!connected) {
                throw new IOException("Failed to connect to the device after multiple attempts");
            }
        }

        List<BluetoothGattService> services = device.getServices();
        for (BluetoothGattService service : services) {
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                if (characteristic.getUUID().equals("0000e001-0000-1000-8000-00805f9b34fb")) { // WRITE_UUID
                    this.writeCharacteristic = characteristic;
                } else if (characteristic.getUUID().equals("0000e002-0000-1000-8000-00805f9b34fb")) { // READ_UUID
                    this.readCharacteristic = characteristic;
                }
            }
        }

        if (this.writeCharacteristic == null || this.readCharacteristic == null) {
            throw new IOException("Characteristics not found");
        }
    }

    private byte computeChecksum(byte[] command) {
        int sum = 0;
        for (int i = 0; i < command.length - 1; i++) {
            sum += command[i];
        }
        return (byte) (sum & 0xFF);
    }

    private byte[] createCommand(byte commandType) {
        byte[] command = new byte[10];
        command[0] = 0x53;  // Start byte
        command[1] = commandType;
        command[2] = 0;  // Fan speed (not used in status request)
        command[3] = 0;  // Fan direction (not used in status request)
        command[4] = 0;  // Uplight intensity (not used)
        command[5] = 0;  // Downlight intensity (not used)
        command[6] = 0;  // Min remaining low byte (not used)
        command[7] = 0;  // Min remaining high byte (not used)
        command[8] = 0;  // Fan type (not used)
        command[9] = computeChecksum(command);  // Checksum
        return command;
    }

    public String getFanStatus() throws IOException {
        if (!device.getConnected()) {
            boolean connected = false;
            int retries = 10;
            while (retries > 0 && !connected) {
                try {
                    device.connect();
                    connected = true;
                } catch (BluetoothException e) {
                    System.err.println("Reconnection failed, retrying... (" + retries + " retries left)");
                    retries--;
                    try {
                        Thread.sleep(200); // Wait for 0.2 seconds before retrying
                    } catch (InterruptedException ie) {
                        System.err.println("Thread sleep interrupted: " + ie.getMessage());
                    }
                }
            }

            if (!connected) {
                throw new IOException("Failed to reconnect to the device after multiple attempts");
            }
        }

        byte[] command = createCommand((byte) 0x30); // GET_FAN_STATUS command
        writeCharacteristic.writeValue(command);

        // Read the response
        byte[] response = readCharacteristic.readValue();
        return interpretStatus(response);
    }

    private String interpretStatus(byte[] response) {
        // Interpret the response bytes
        int speed = response[2]; // Assuming speed is at index 2
        int direction = response[3]; // Assuming direction is at index 3
        boolean isOn = speed > 0;

        String directionStr = (direction == 0) ? "Forward" : "Reverse";
        return "Fan Status - Power: " + (isOn ? "On" : "Off") + ", Speed: " + speed + ", Direction: " + directionStr;
    }

    public void close() throws IOException {
        if (device.getConnected()) {
            device.disconnect();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java FanStatusChecker <MAC_ADDRESS>");
            return;
        }

        String macAddress = args[0];

        try {
            FanStatusChecker checker = new FanStatusChecker(macAddress);
            String status = checker.getFanStatus();
            System.out.println(status);
            checker.close();
        } catch (IOException e) {
            System.err.println("Failed to retrieve fan status: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
