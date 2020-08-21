import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLOutput;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

@ClientEndpoint
public class ChatClientEndpoint {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private static CountDownLatch latch;
    private String username = "";
    private Thread messenger;

    private class sendMessage extends Thread {
        private Session session;

        public sendMessage(Session session) {
            this.session = session;
        }

        public void run() {
            while(true) {
                try {
                    String newMessage;
                    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
                    newMessage = r.readLine();
                    if (newMessage.equals("!exit")) {
                        session.getBasicRemote().sendText("!exit" + username);
                        session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "User logged out."));
                        break;
                    }
                    session.getBasicRemote().sendText(newMessage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Welcome. Please enter a username.");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            username = r.readLine();
            session.getBasicRemote().sendText(String.format("new_user:%s", username));
            logger.info("Enter !exit to leave the chatroom, or !quit to shut down the chatroom.");
            messenger = new sendMessage(session);
            messenger.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        System.out.println(message);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session ended by user."));
        latch.countDown();
        messenger.interrupt();
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
