import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class GameServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/aimove", new AIMoveHandler());
        server.setExecutor(null);
        System.out.println("Backend Server is LIVE on http://localhost:8080");
        server.start();
    }

    static class AIMoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable Cross-Origin Resource Sharing (CORS) for the frontend
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("board=")) {
                String boardState = query.split("=")[1];
                char[] board = boardState.toCharArray();

                int bestMove = calculateBestMove(board, 'O');
                if (bestMove == -1) bestMove = getRandomMove(board);

                String response = String.valueOf(bestMove);
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        private int calculateBestMove(char[] board, char player) {
            int[][] combos = {{0,1,2}, {3,4,5}, {6,7,8}, {0,3,6}, {1,4,7}, {2,5,8}, {0,4,8}, {2,4,6}};
            // Try to win or block
            for (char symbol : new char[]{'O', 'X'}) {
                for (int[] c : combos) {
                    if (board[c[0]] == symbol && board[c[1]] == symbol && board[c[2]] == '-') return c[2];
                    if (board[c[0]] == symbol && board[c[2]] == symbol && board[c[1]] == '-') return c[1];
                    if (board[c[1]] == symbol && board[c[2]] == symbol && board[c[0]] == '-') return c[0];
                }
            }
            return -1;
        }

        private int getRandomMove(char[] board) {
            for (int i = 0; i < 9; i++) if (board[i] == '-') return i;
            return -1;
        }
    }
}