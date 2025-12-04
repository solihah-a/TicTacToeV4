package clarkson.ee408.tictactoev4.socket;

import java.util.Collections;
import java.util.List;

import clarkson.ee408.tictactoev4.model.*;

/**
 * Models the server's response to an UPDATE_PAIRING request in the TicTacToe game.
 */
public class PairingResponse extends Response {

    /**
     * Represents players that are available to receive game invitations.
     */
    private List<User> availableUsers;

    /**
     * Represents a game invitation from another user.
     */
    private Event invitation;

    /**
     * Represents a response to a game invitation previously sent by the current user.
     */
    private Event invitationResponse;

    /**
     * Default constructor that creates a {@code PairingResponse} with default values.
     */
    public PairingResponse() {
        this(Collections.emptyList(), null, null);
    }

    /**
     * Creates a new instance of {@code PairingResponse}.
     *
     * @param availableUsers a list of players that are available to receive game invitations
     * @param invitation a game invitation from another user
     * @param invitationResponse a response to a game invitation
     */
    public PairingResponse(List<User> availableUsers, Event invitation, Event invitationResponse) {
        super();
        this.availableUsers = availableUsers;
        this.invitation = invitation;
        this.invitationResponse = invitationResponse;
    }

    /**
     * Returns the list of available users that can receive game invitations.
     *
     * @return the available users
     */
    public List<User> getAvailableUsers() {
        return this.availableUsers;
    }

    /**
     * Returns the game invitation from another user.
     *
     * @return the game invitation
     */
    public Event getInvitation() {
        return this.invitation;
    }

    /**
     * Returns the game invitation response.
     *
     * @return the game invitation
     */
    public Event getInvitationResponse() {
        return this.invitationResponse;
    }

    /**
     * Sets the list of available users that can receive game invitations.
     *
     * @param availableUsers the available users to set
     */
    public void setAvailableUsers(List<User> availableUsers) {
        this.availableUsers = availableUsers;
    }

    /**
     * Sets the game invitation from another user.
     *
     * @param invitation the invitation to set
     */
    public void setInvitation(Event invitation) {
        this.invitation = invitation;
    }

    /**
     * Sets the game invitation response.
     *
     * @param invitationResponse the game invitation response to set
     */
    public void setInvitationResponse(Event invitationResponse) {
        this.invitationResponse = invitationResponse;
    }
}