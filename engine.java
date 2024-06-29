import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;

public class WindowTiler {

    // Method to check if a process is running as admin
    public static boolean isProcessElevated(WindowInfo windowInfo) {
        int processId = windowInfo.getProcessId();

        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "net session");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Method to tile windows
    public static void tileWindows() {
        // Get screen dimensions
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        Rectangle screenBounds = screens[0].getDefaultConfiguration().getBounds();

        // Enumerate all windows
        List<WindowInfo> windows = new ArrayList<>();
        Window[] allWindows = Window.getWindows();
        for (Window w : allWindows) {
            if (w.isVisible() && !(w instanceof Frame && !((Frame)w).isResizable())) {
                windows.add(new WindowInfo(w));
            }
        }

        // Calculate positions
        int numRows = (int) Math.ceil(Math.sqrt(windows.size()));
        int numCols = (int) Math.ceil((double) windows.size() / numRows);
        int windowWidth = screenBounds.width / numCols;
        int windowHeight = screenBounds.height / numRows;

        // Tile windows
        for (int i = 0; i < windows.size(); i++) {
            WindowInfo windowInfo = windows.get(i);
            if (isProcessElevated(windowInfo)) {
                System.out.println("Admin window detected: " + windowInfo.getTitle());
                continue;
            }

            int row = i / numCols;
            int col = i % numCols;
            int x = screenBounds.x + col * windowWidth;
            int y = screenBounds.y + row * windowHeight;

            windowInfo.getWindow().setBounds(x, y, windowWidth, windowHeight);
        }
    }

    // Main method
    public static void main(String[] args) {
        // Periodically tile windows (every 5 seconds)
        Timer timer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tileWindows();
            }
        });
        timer.setInitialDelay(0);
        timer.start();

        // Handle Ctrl+C exit behavior
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Display confirmation dialog on first Ctrl+C press
                JOptionPane.showMessageDialog(null, "Are you sure you want to quit?");
            }
        });

        // Wait for user to press Ctrl+C again to actually quit
        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

// Helper class to store window information
class WindowInfo {
    private Window window;
    private String title;
    private int processId;

    public WindowInfo(Window window) {
        this.window = window;
        this.title = window.getName();
        this.processId = getWindowProcessId(window);
    }

    public Window getWindow() {
        return window;
    }

    public String getTitle() {
        return title;
    }

    public int getProcessId() {
        return processId;
    }

    // Method to get process ID of a window
    private int getWindowProcessId(Window window) {
        long hwnd = Native.getComponentID(window);
        return Native.getProcessId(hwnd);
    }

    // Native methods to interact with Windows API
    private static class Native {
        static {
            System.loadLibrary("user32");
        }

        public static native long getComponentID(Window window);

        public static native int getProcessId(long hwnd);
    }
}
