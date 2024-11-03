package ru.netology;

import ru.netology.service.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.netology.service.Config.*;

public class ServerMain {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public static final Logger logger = Logger.getLogger(ServerMain.class.getName());

    public static void main(String[] args) {
        loadConfig();
        try (ServerSocket serverSocket = new ServerSocket(getPort())) {
            logger.info("Сервер запущен на порту: " + getPort());
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
}