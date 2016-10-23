package server.controller;

import game.action.Ack;
import game.action.UpdateMatchList;
import game.action.UpdateUser;
import server.model.Match;
import server.model.User;

/**
 * GameLogic(room) where users have to join/create a room to play in
 */
public class GameLogic {
    private static GameLogic gameLogic;

    private GameLogic() {
        new Match("Merda Room");
    }

    /**
     * Singleton pattern
     *
     * @return
     */
    public static GameLogic getInstance() {
        if (gameLogic == null) {
            gameLogic = new GameLogic();
        }
        return gameLogic;
    }

    public void sendWarningMessageTo(User user, String message) {
        Ack ack = new Ack(message, Ack.MessageType.WARNING);
        user.getSocketHandler().sendAction(ack);
    }

    public void sendDangerMessageTo(User user, String message) {
        Ack ack = new Ack(message, Ack.MessageType.DANGER);
        user.getSocketHandler().sendAction(ack);
    }

    public void sendInfoMessageTo(User user, String message) {
        Ack ack = new Ack(message, Ack.MessageType.INFO);
        user.getSocketHandler().sendAction(ack);
    }

    /**
     * Sends a list with all active matches to an user
     *
     * @param user receiver of all matches
     */
    public void sendMatchesListTo(User user) {
        UpdateMatchList updateMatchList = new UpdateMatchList(Match.getMatches());//Creates a new updateMatchList containing a list of all matches
        user.getSocketHandler().sendAction(updateMatchList);//Sends an updateMatchList to the specified user
    }

    /**
     * Syncs local user with server's one
     *
     * @param user
     */
    public void sendUpdateUserTo(User user) {
        UpdateUser updateUser = new UpdateUser(user);
        user.getSocketHandler().sendAction(updateUser);//Sync client's user data with server's
    }
}