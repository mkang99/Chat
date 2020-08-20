import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

@ServerEndpoint(value = "/chat")
public class ChatServerEndpoint {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private static Map<String, String> users = new HashMap<String, String>();
    private static Set<Session> chatClients = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        logger.info("Connected: " + session.getId());
        chatClients = session.getOpenSessions();
    }

    @OnMessage
    public String onMessage(Session session, String message) {
        System.out.println(message);
        if (message.equals("!quit")) {
            logger.info("Closing the chatroom.");
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Chatroom closed."));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (message.equals("!exit")) {
            chatClients.remove(session);
            messageAll("User logged out.");
        } else if (message.length() > 8 && message.substring(0, 8).equals("new_user")) {
            users.put(session.getId(), message.substring(8));
            System.out.println("New user added");
        } else {
            String finalMessage = String.format("%s: " + message, users.get(session.getId()));
            return messageAll(message);
        }
        return "start";
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
    }

    public static String messageAll(String chatMessage) {
        for (Session s : chatClients) {
            try {
                System.out.println(chatMessage);
                System.out.println(s.getId());
                s.getBasicRemote().sendText(chatMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return chatMessage;
    }
}
