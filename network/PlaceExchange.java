package place.network;

/**
 * The {@link PlaceExchange} interface provides constants for all of the
 * messages that are communicated between the reversi.server and the
 * reversi.client's.
 *
 * Changes suggested for the future by JEH
 * <ul>
 *     <li>Include player no. in the MOVE messages.</li>
 *     <li>Instead of reporting a player's move, report board updates.</li>
 *     <li>This includes initial setup moves.</li>
 * </ul>
 *
 * @author Robert St Jacques @ RIT SE
 * @author Sean Strout @ RIT CS
 */
public interface PlaceExchange {
    /**
     * After a successful client login, the server will send the current
     * Board to the client.  This is only sent once - afterwards the
     * only information transmitted are the tile changes.
     */
    public static final String BOARD = "BOARD";

    /**
     * A client's request to the server to change a tile.  It will contain
     * a Tile object.  It is important to note that the client should not
     * change the tile in their board until it is acknowledged by the server
     * via the TILE_CHANGED request.
     */
    public static final String CHANGE_TILE = "CHANGE_TILE";

    /**
     * Used for the server to tell the client there was an error.  It will
     * contain a message about the error. One place this is used is to tell
     * the client a login failed (because the username already exists).  It
     * is also used to indicate the server is shutting down, or any other
     * unusual things happen.
     */
    public static final String ERROR = "ERROR";

    /**
     * Used by the client to login to the server.  It will contain a string
     * that is the desired username for the client.
     */
    public static final String LOGIN = "LOGIN";

    /**
     * Used by the server to indicate to the client the login succeeded.
     * It will contain a string indicating this.
     */
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";

    /**
     * Used by the server to indicate to all clients that a tile has
     * officially been changed.  It will contain the new Tile object.
     * The clients should update their view of the board each time
     * a tile change arrives.
     */
    public static final String TILE_CHANGED = "TILE_CHANGED";
}
