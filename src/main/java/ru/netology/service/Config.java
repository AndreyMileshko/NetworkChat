package ru.netology.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static ru.netology.ServerMain.logger;

public class Config {

    private static int port;
    private static final int defaultPort = 8080;
    private static final String settingsFileName = "src\\main\\java\\ru\\netology\\service\\settings.txt";


    public static void loadConfig() {
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

    public static int getPort() {
        return port;
    }
}