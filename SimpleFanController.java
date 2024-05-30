/**
 * Example Commands:
 * 
 * Turn the Fan On:
 * sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC on
 * 
 * Turn the Fan Off:
 * sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC off
 * 
 * Set Fan Speed:
 * sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC speed 10
 * 
 * Change Fan Direction to Forward:
 * sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC direction forward
 * 
 * Change Fan Direction to Reverse:
 * sudo java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController 78:04:73:19:77:BC direction reverse
 */
import tinyb.*;
import java.util.List;
import java.io.IOException;

public class SimpleFanController {
    private BluetoothDevice device;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;

    public SimpleFanController(String address) throws InterruptedException, IOException {
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

    private byte[] createCommand(byte commandType, byte fanSpeed, byte fanDirection) {
        byte[] command = new byte[10];
        command[0] = 0x53;  // Start byte
        command[1] = commandType;
        command[2] = fanSpeed;
        command[3] = fanDirection;
        command[4] = 0;  // Uplight intensity (not used)
        command[5] = 0;  // Downlight intensity (not used)
        command[6] = 0;  // Min remaining low byte (not used)
        command[7] = 0;  // Min remaining high byte (not used)
        command[8] = 0;  // Fan type (not used)
        command[9] = computeChecksum(command);  // Checksum
        return command;
    }

    private byte[] createStatusCommand() {
        byte[] command = new byte[10];
        command[0] = 0x53;  // Start byte
        command[1] = 0x30;  // GET_FAN_STATUS command
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

    private byte[] getCurrentFanStatus() throws IOException {
        byte[] command = createStatusCommand();
        writeCharacteristic.writeValue(command);
        return readCharacteristic.readValue();
    }

    public void setFanSpeed(int speed) throws IOException {
        byte[] command = createCommand((byte) 0x31, (byte) speed, (byte) 0);
        writeCharacteristic.writeValue(command);
    }

    public void setFanDirection(boolean forward) throws IOException {
        byte[] status = getCurrentFanStatus();
        byte currentSpeed = status[2];  // Assuming speed is at index 2
        byte direction = (byte) (forward ? 0 : 1); // 0 for forward, 1 for reverse
        byte[] command = createCommand((byte) 0x31, currentSpeed, direction);
        writeCharacteristic.writeValue(command);
    }

    public void setFanPower(boolean on) throws IOException {
        byte[] status = getCurrentFanStatus();
        byte currentSpeed = status[2];  // Assuming speed is at index 2
        byte currentDirection = status[3];  // Assuming direction is at index 3

        if (currentSpeed == 0 && on) {
            // Fan is off, set power on with speed 1
            byte speed = 1;
            byte[] command = createCommand((byte) 0x31, speed, currentDirection);
            writeCharacteristic.writeValue(command);
        } else if (currentSpeed > 0 && on) {
            // Fan is already on, skip the power on command
            System.out.println("Fan is already on, skipping power on command.");
        } else if (!on) {
            // Turn off the fan
            byte speed = 0;
            byte[] command = createCommand((byte) 0x31, speed, currentDirection);
            writeCharacteristic.writeValue(command);
        }
    }

    public void close() throws IOException {
        if (device.getConnected()) {
            device.disconnect();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SimpleFanController <MAC_ADDRESS> <COMMAND> [OPTIONS]");
            System.out.println("Commands:");
            System.out.println("  on");
            System.out.println("  off");
            System.out.println("  speed <value>");
            System.out.println("  direction <forward|reverse>");
            System.exit(1);
        }

        String macAddress = args[0];
        String command = args[1];

        try {
            SimpleFanController controller = new SimpleFanController(macAddress);

            switch (command) {
                case "on":
                    controller.setFanPower(true);
                    break;
                case "off":
                    controller.setFanPower(false);
                    break;
                case "speed":
                    if (args.length < 3) {
                        System.out.println("Missing speed value");
                        System.exit(1);
                    }
                    int speed = Integer.parseInt(args[2]);
                    controller.setFanSpeed(speed);
                    break;
                case "direction":
                    if (args.length < 3) {
                        System.out.println("Missing direction value");
                        System.exit(1);
                    }
                    String direction = args[2];
                    boolean forward = direction.equalsIgnoreCase("forward");
                    controller.setFanDirection(forward);
                    break;
                default:
                    System.out.println("Unknown command");
                    System.exit(1);
                    break;
            }

            controller.close();
            System.out.println("Success");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failure");
            System.exit(1);
        }
    }
}
