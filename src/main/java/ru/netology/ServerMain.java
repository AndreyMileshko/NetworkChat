package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMain {
    private static int port;
    private static final int defaultPort = 8080;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    private static final Logger logger = Logger.getLogger(ServerMain.class.getName());
    private static final ConcurrentHashMap<PrintWriter, Object> clientWriters = new ConcurrentHashMap<>();
    private static final String settingsFileName = "src\\main\\java\\ru\\netology\\settings.txt";

    public static void main(String[] args) {
        loadConfig();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер запущен на порту " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при запуске сервера: ", e);
        } finally {
            executorService.shutdown();
        }
    }

    private static void loadConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(settingsFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("port")) {
                    String[] split = line.split(" ");
                    if (split.length == 2) {
                        port = Integer.parseInt(split[1]);
                    } else {
                        logger.warning("Неверный формат информации в файле " + settingsFileName +
                                ", используется порт по умолчанию: " + defaultPort);
                        port = defaultPort;
                    }
                }
            }
        } catch (IOException e) {
            logger.warning("Ошибка чтения файла " + settingsFileName +
                    ", используется порт по умолчанию: " + defaultPort);
            port = defaultPort;
        } catch (NumberFormatException e) {
            logger.warning("Ошибка формата порта, используется порт по умолчанию: " + defaultPort);
            port = defaultPort;
        }
    }

    public static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                createsStreams();
                requestClientName();
                messageDistribution();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Ошибка при обработке операций клиента: ", e);
            } finally {
                resourceClosing();
            }
        }


        private void createsStreams() throws IOException {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            clientWriters.put(out, Boolean.TRUE);
        }

        private void requestClientName() throws IOException {
            out.print("Введите ваше имя: ");
            clientName = in.readLine();
            logger.info("Клиент " + clientName + " присоединился.");
            //Возможно нужно сделать перенос строки, нужно посмотреть при тестировании.
        }

        private void messageDistribution() throws IOException {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/exit")) {
                    logger.info("Клиент " + clientName + " отключился.");
                    break;
                }
                String timeMessage = new SimpleDateFormat("HH:mm:ss").format(new Date());
                String logMessage = String.format("[%s] %s: %s", timeMessage, clientName, message);
                logMessageToFile(logMessage);
                out.println(logMessage);
            }
        }

        private void logMessageToFile(String logMessage) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("File.log"))) {
                writer.write(logMessage);
                writer.newLine();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Ошибка при записи сообщения в файл: ", e);
            }
        }

        private void resourceClosing() {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    clientWriters.remove(out);
                    out.close();
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Ошибка при закрытии ресурсов: ", e);
            }
        }
    }
}