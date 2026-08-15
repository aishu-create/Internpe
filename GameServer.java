import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Random;

public class GameServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/play", new GameLogicHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Backend Server is running!");
        System.out.println("Open your browser and go to: http://localhost:8080/index.html");
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html"; 

            File file = new File("." + path);
            if (file.exists() && !file.isDirectory()) {
                exchange.sendResponseHeaders(200, file.length());
                OutputStream os = exchange.getResponseBody();
                Files.copy(file.toPath(), os);
                os.close();
            } else {
                String response = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    static class GameLogicHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String[] params = query.split("&");
            int p1Choice = Integer.parseInt(params[0].split("=")[1]);
            int p2Choice = Integer.parseInt(params[1].split("=")[1]);

            int finalP2Choice = p2Choice;
            if (finalP2Choice == -1) {
                finalP2Choice = new Random().nextInt(3);
            }

            int winner = 0; // 0 = Tie, 1 = P1, 2 = P2

            // Determine Winner
            if (p1Choice == finalP2Choice) {
                winner = 0;
            } else if ((p1Choice == 0 && finalP2Choice == 2) || 
                       (p1Choice == 1 && finalP2Choice == 0) || 
                       (p1Choice == 2 && finalP2Choice == 1)) {
                winner = 1;
            } else {
                winner = 2;
            }

            // Create a JSON response
            String jsonResponse = String.format("{\"p2Choice\": %d, \"winner\": %d}", finalP2Choice, winner);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(jsonResponse.getBytes());
            os.close();
        }
    }
}