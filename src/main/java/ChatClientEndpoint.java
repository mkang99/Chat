import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

@ClientEndpoint
public class ChatClientEndpoint {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private static CountDownLatch latch;
    private String username = "";

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Welcome. Please enter a username.");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            username = r.readLine();
            session.getBasicRemote().sendText(String.format("new_user:%s", username));
            logger.info("Enter !exit to leave the chatroom, or !quit to shut down the chatroom.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnMessage
    public String onMessage(Session session, String message) {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
//        logger.info(message);
        String newMessage;
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            newMessage = "";
            newMessage = r.readLine();
            if (newMessage.equals("!exit")) {
                session.getBasicRemote().sendText("!exit");
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "User logged out."));
            }
            session.getBasicRemote().sendText(newMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(newMessage);
        return newMessage;
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session ended by user."));
        latch.countDown();
    }

    public static void main(String[] args) {
        latch = new CountDownLatch(1);
        ClientManager client = ClientManager.createClient();
        try {
            client.connectToServer(ChatClientEndpoint.class, new URI("ws://localhost:8025/websockets/chat"));
            latch.await();
        } catch (DeploymentException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
