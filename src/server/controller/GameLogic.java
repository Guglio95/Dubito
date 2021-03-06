package server.controller;

import game.action.*;
import game.exception.ActionException;
import game.model.*;
import server.model.Deck;
import server.model.Match;
import server.model.User;
import utils.MyLogger;

import javax.jws.soap.SOAPBinding;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * GameLogic(room) where users have to join/create a room to play in
 */
public class GameLogic {
    private static GameLogic gameLogic;
    private static ArrayList<User> connectedUsers = new ArrayList<User>(10);//List containing all connected users

    private GameLogic() {
        new Match("Default Room");
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


    /**
     * Remotely changes user's GUI view
     * @param user user affected
     * @param goTo view to go to
     */
    public void changeView(User user, ChangeView.GoTo goTo) {
        Action action = new ChangeView(goTo);
        user.getSocketHandler().sendAction(action);
    }

    /**
     * shows Warning Modal on client
     *
     * @param user
     * @param message
     */
    public void sendWarningMessageTo(User user, String message) {
        Alert alert = new Alert(message, Alert.MessageType.WARNING);
        user.getSocketHandler().sendAction(alert);
    }

    /**
     * shows Danger Modal on client
     *
     * @param user
     * @param message
     */
    public void sendDangerMessageTo(User user, String message) {
        Alert alert = new Alert(message, Alert.MessageType.DANGER);
        user.getSocketHandler().sendAction(alert);
    }

    /**
     * shows Info Modal on client
     *
     * @param user
     * @param message
     */
    public void sendInfoMessageTo(User user, String message) {
        Alert alert = new Alert(message, Alert.MessageType.INFO);
        user.getSocketHandler().sendAction(alert);
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

    public void sendUpdateUserTo(Match match) {
        List<User> userList = match.getUsers();
        userList.forEach(u -> sendUpdateUserTo(u));
    }

    public void sendDiscardCardsTo(Match match, User performer, List<Card> cards) {
        List<User> userList = match.getUsers();
        userList.forEach(u -> sendDiscardCardsTo(u, performer, cards));
    }

    public void sendDiscardCardsTo(User user, User performer, List<Card> cards) {
        Croupier croupier = new Croupier(Croupier.ActionType.DISCARD_CARDS, performer, cards, null);
        user.getSocketHandler().sendAction(croupier);//Sync client's user data with server's
    }

    public void sendPickCardsTo(Match match, User cardsPicker, List<Card> cardsPicked) {
        List<User> userList = match.getUsers();
        userList.forEach(u -> sendPickCardsTo(u, cardsPicker, cardsPicked));
    }

    public void sendPickCardsTo(User user, User cardsPicker, List<Card> cardsPicked) {
        Croupier croupier = new Croupier(Croupier.ActionType.PICK_CARDS, cardsPicker, cardsPicked, null);
        user.getSocketHandler().sendAction(croupier);//Sync client's user data with server's
    }

    public void sendPutCards(User userWhoPuts, List<Card> cards, CardType cardType) {
        ArrayList<User> users = new ArrayList<>(userWhoPuts.getMatch().getUsers());
        users.remove(userWhoPuts);
        users.forEach((u) -> {
            Croupier croupier = new Croupier(Croupier.ActionType.PUT_CARDS, userWhoPuts, cards, cardType);
            u.getSocketHandler().sendAction(croupier);//Sync client's user data with server's
        });
    }

    /**
     * Sends to specified user a personalized copy of server's match obj (with his enemies etc..)
     *
     * @param user
     */
    public void sendUpdateMatchTo(User user) {
        UpdateMatch updateUser = new UpdateMatch(user.getMatch());
        user.getSocketHandler().sendAction(updateUser);//Sync client's user data with server's
    }

    /**
     * Sends to every match's user a personalized copy of server's match obj (with his enemies etc..)
     *
     * @param match
     */
    public void sendUpdateMatchTo(Match match) {
        List<User> userList = match.getUsers();
        userList.forEach(u -> sendUpdateMatchTo(u));
    }

    /**
     * Carries user out of match and brings him to Lobby
     *
     * @param user
     * @throws ActionException
     */
    public void leaveMatch(User user) throws ActionException {
        if (user.hasMatch()) {
            //// TODO: 26/10/16 send chat message with byebye
            user.getMatch().removeUser(user);//Remove user from match
            this.sendUpdateMatchTo(user.getMatch());//Update other users
            user.setMatch(null);//remove user reference to match
            user.setUserState(UserState.LOBBY);//bring him back to lobby
        } else throw new ActionException(user.getUsername() + " is not in a match");
    }

    /**
     * Starts the specified match if possible
     *
     * @param match
     * @throws ActionException
     */
    public void startMatch(Match match) throws ActionException {
        if (match == null) throw new ActionException("Game dosn't exist");
        if (match.getUsers().size() < 2)
            throw new ActionException("Bisogna almeno essere in due per giocare!");//A user can't play alone... it's sad!
        if (match.getMatchState() == MatchState.PLAYING)
            throw new ActionException("Match already started");//A user can't play alone... it's sad!

        match.setMatchState(MatchState.PLAYING);//Set match as started
        match.getUsers().forEach((user) -> ((User) user).setUserState(UserState.PLAYING));//set each user as playing


        Deck deck = new Deck();//Creates a new deck.
        deck.giveCards(match.getUsers());//divides cards

        match.nextTurn();//Starts turnments chain

        this.sendUpdateUserTo(match);//Send every match user the updated objects
        this.sendUpdateMatchTo(match);//Send every match user the updated objects
        match.getUsers().forEach((user) -> changeView((User)user, ChangeView.GoTo.GAME));//set each user as playing
        MyLogger.println("Match started");
    }

    /**
     * Called when user won
     * @param user
     */
    public void userWon(User user){
        List<User> users = new ArrayList<>(user.getMatch().getUsers());
        Match match = user.getMatch();

        users.forEach(u -> {
            u.setUserState(UserState.LOBBY);
            match.removeUser(u);
            u.setMatch(null);
            Action action = new GameOver(user);
            u.getSocketHandler().sendAction(action);
        });

        //Update all users' match list
        getConnectedUsers().forEach(u -> sendMatchesListTo(u));
    }

    /**
     * This establishes the loser when a 'DUBITO' move is played
     *
     * @param actualPlayer
     * @param lastMove
     */
    public void executeDubitoPlay(User actualPlayer, UserPlay lastMove) {
        //If last move's cards have card.Suit != pretendedType, actualPlayer wins, oldPlayer loses.
        User oldPlayer = lastMove.getPerformer();
        CardType pretendedType = lastMove.getCardsType();
        List<Card> cards = lastMove.getCards();//Cards played by oldPlayer

        User loser = actualPlayer;
        for (int i = 0; i < cards.size(); i++) {
            if (!cards.get(i).getCardType().equals(pretendedType)) {
                loser = oldPlayer;
                break;
            }
        }

        MyLogger.println(loser.getUsername() + " lost");
        sendPickCardsTo(actualPlayer.getMatch(), loser, actualPlayer.getMatch().getTableCardsList());//Syncs everyone GUI moving card to loser

        if (actualPlayer.getMatch().getTableCardsList().size() > 0) {
            Match userMatch = actualPlayer.getMatch();
            this.moveCardsToUser(loser);//Give all table covered cards to loser
            userMatch.getTableCardsList().clear();//Clears table cards
            userMatch.setLastMove(null);//Clears last action, we are starting new round.
        }
        if (actualPlayer.equals(loser)){
            actualPlayer.getMatch().nextTurn();//If actual player loses his turn is over
        }
    }

    /**
     * Moves cards from table to user's deck
     *
     * @param target Target user
     * @throws ActionException
     */
    public void moveCardsToUser(User target) {
        target.getCards().addAll(target.getMatch().getTableCardsList());
        target.getMatch().getTableCardsList().clear();
    }

    /**
     * Moves cards from source deck to table
     *
     * @param cards
     * @param source
     * @throws ActionException
     */
    public void moveCardsToTable(List<Card> cards, User source) throws ActionException {
        if (!source.getCards().containsAll(cards))
            throw new ActionException("Stai provando a giocare carte che non hai");
        source.getCards().removeAll(cards);
        source.getMatch().getTableCardsList().addAll(cards);
    }


    /**
     * Remove every reference to this user
     *
     * @param user
     */
    public void onUserDisconnect(User user) {
        try {
            this.leaveMatch(user);
        } catch (Exception e) {
            //e.printStackTrace();//Replies that user is not in a match...
        }
        connectedUsers.remove(user);
        MyLogger.println(user.getUsername() + " left");
    }

    /**
     * method called when user connects to server
     * @param user
     */
    public void onUserConnect(User user){
        connectedUsers.add(user);
    }

    /**
     * Returns if this username exists in server
     * @param username username to check
     * @return
     */
    public boolean usernameExists(String username){
        return connectedUsers.contains(new User(username));
    }

    public static List<User> getConnectedUsers() {
        return connectedUsers;
    }
}
