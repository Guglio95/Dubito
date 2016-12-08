package game.action;

import game.exception.ActionException;
import game.model.Match;
import game.model.User;
import game.model.UserState;
import server.controller.GameLogic;
import utils.MyLogger;

/**
 * Created by andrea on 23/10/16.
 */

/**
 * Action sent from client to server right after the connection to set the username
 */
public class JoinServer extends Action {
    private String username;

    /**
     * Prepares payload
     *
     * @param username
     */
    public JoinServer(String username) {
        this.username = username;
    }

    /**
     * Tries to login into the server and ask for updated data
     *
     * @param user
     * @throws ActionException
     */
    @Override
    public void doAction(User user) throws ActionException {//Called on server when we login to it.
        if (GameLogic.getInstance().usernameExists(this.username)){
            GameLogic.getInstance().sendInfoMessageTo((server.model.User) user, "This username is already taken");//Username already taken
        }else {
            user.setUsername(this.username);//Set username
            user.setUserState(UserState.LOBBY);//Set user state to (in)LOBBY, this will also update client GUI view
            GameLogic.getInstance().changeView((server.model.User)user, ChangeView.GoTo.SELECT_ROOM);//Changes user's view to SELECT_ROOM
            GameLogic.getInstance().sendUpdateUserTo((server.model.User) user);//Sends user an updated snapshot of himself, including new username
            GameLogic.getInstance().sendMatchesListTo((server.model.User) user);//Sends user a list of all active matches
        }
    }
}
