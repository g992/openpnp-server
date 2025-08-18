package org.openpnp.machine.reference.driver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

import org.simpleframework.xml.Attribute;

/**
 * Коммуникации для Klipper через UNIX domain socket (обычно
 * путь по умолчанию '/tmp/printer').
 */
public class KlipperCommunications extends ReferenceDriverCommunications {
    @Attribute(required = false)
    protected String socketPath = "/tmp/printer";

    @Attribute(required = false)
    protected String name = "KlipperCommunications";

    protected Object clientSocket;
    protected BufferedReader input;
    protected DataOutputStream output;

    @Override
    public synchronized void connect() throws Exception {
        disconnect();
        try {
            File socketFile = new File(socketPath);
            if (!socketFile.exists()) {
                throw new IOException("Klipper UNIX socket not found: " + socketPath
                        + ". Start Klipper or adjust the socket path.");
            }
            Class<?> socketClass = Class.forName("org.newsclub.net.unix.AFUNIXSocket");
            Class<?> addrClass = Class.forName("org.newsclub.net.unix.AFUNIXSocketAddress");

            Method newInstance = socketClass.getMethod("newInstance");
            Object sock = newInstance.invoke(null);

            Constructor<?> addrCtor = addrClass.getConstructor(File.class);
            Object addr = addrCtor.newInstance(new File(socketPath));

            Method connect = socketClass.getMethod("connect", addrClass);
            connect.invoke(sock, addr);

            Method getInputStream = socketClass.getMethod("getInputStream");
            Method getOutputStream = socketClass.getMethod("getOutputStream");

            clientSocket = sock;
            input = new BufferedReader(new InputStreamReader((InputStream) getInputStream.invoke(sock)));
            output = new DataOutputStream((OutputStream) getOutputStream.invoke(sock));
        } catch (ClassNotFoundException e) {
            throw new IOException("junixsocket library not found. Ensure junixsocket-core is on classpath.", e);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Klipper UNIX socket connection failed (reflection error): " + e.getMessage(), e);
        } catch (Throwable t) {
            // Любые ошибки соединения упакуем с понятным сообщением
            throw new IOException("Failed to connect to Klipper at '" + socketPath + "': "
                    + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()), t);
        }
    }

    @Override
    public synchronized void disconnect() throws Exception {
        if (clientSocket != null) {
            try {
                Method isConnected = clientSocket.getClass().getMethod("isConnected");
                boolean connected = (Boolean) isConnected.invoke(clientSocket);
                if (connected) {
                    Method close = clientSocket.getClass().getMethod("close");
                    close.invoke(clientSocket);
                }
            } catch (Exception ignore) {
            } finally {
                input = null;
                output = null;
                clientSocket = null;
            }
        }
    }

    @Override
    public String getConnectionName() {
        return (driverName != null ? driverName + ":" : "") + socketPath;
    }

    @Override
    public int read() throws TimeoutException, IOException {
        try {
            return input.read();
        } catch (NullPointerException ex) {
            throw new IOException("Trying to read from an unconnected Klipper UNIX socket.");
        }
    }

    @Override
    protected void writeBytes(byte[] data) throws IOException {
        output.write(data);
    }

    public String getSocketPath() {
        return socketPath;
    }

    public void setSocketPath(String socketPath) {
        this.socketPath = socketPath;
    }
}
