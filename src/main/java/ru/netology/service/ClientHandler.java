package ru.netology.service;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static ru.netology.service.Server.logger;

public class ClientHandler implements Runnable {
    private static final ConcurrentHashMap<PrintWriter, Boolean> clientWriters = new ConcurrentHashMap<>();
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
        out.println("Введите ваше имя: ");
        clientName = in.readLine();
        if (clientName != null) {
            logger.info("Клиент " + clientName + " присоединился.");
            out.println("Добро пожаловать " + clientName + "! Для выхода из чата введите \"/exit\"");
            messageSending("Клиент " + clientName + " присоединился.");
        } else {
            logger.warning("Клиент отключился до ввода имени.");
        }

        //Возможно нужно сделать перенос строки, нужно посмотреть при тестировании.
    }

    private void messageDistribution() throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            if (message.equalsIgnoreCase("/exit")) {
                logger.info("Клиент " + clientName + " отключился.");
                messageSending("Клиент " + clientName + " отключился.");
                break;
            }
            String timeMessage = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String logMessage = String.format("[%s] %s: %s", timeMessage, clientName, message);
            logMessageToFile(logMessage);
            messageSending(logMessage);
        }
    }

    private void logMessageToFile(String logMessage) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("File.log", true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при записи сообщения в файл: ", e);
        }
    }

    private synchronized void messageSending(String logMessage) {
        for (PrintWriter writer : clientWriters.keySet()) {
            writer.println(logMessage);
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