package game.action;

import game.exception.ActionException;
import game.model.User;
import gameClient.SceneDirector;

/**
 * Created by andrea on 23/10/16.
 */

/**
 * Shows modal on client
 */
public class Alert extends Action {
    public enum MessageType {
        INFO,
        WARNING,
        DANGER;
    }

    private String message;
    private MessageType messageType;

    public Alert(String message, MessageType messageType) {
        this.message = message;
        this.messageType = messageType;
    }

    @Override
    public void doAction(User user) throws ActionException {
        SceneDirector.showModal(messageType.toString(),message);
    }
}
