package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;
import place.network.PlaceRequest.RequestType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;


/**
 * The server is going to connect with the User via this class.
 * This class represents the controller part of a model-view-controller.
 *
 * @author Yu Qi Wu
 * @author Eve Cho.
 */
public class NetworkServer {
    /**
     * used to format the time stamp when a tile is changed
     */
    private final static SimpleDateFormat TIME_STAMP = new SimpleDateFormat("MM-dd-yyyy 'at' HH:mm:ss");

    /**
     * Map that contains connec ted users in real-time
     * key = a username string
     * value = user's ObjectOutputStream
     */
    private Map<String, ObjectOutputStream> users;

    /**
     * The PlaceBoard that is used to send to new users.
     */
    private PlaceBoard board;

    /**
     * Constructs a NetworkServer used to communicate with clients.
     *
     * @param dim the dimension of the board once it is set up
     */
    public NetworkServer(int dim)
    {
        this.users = new HashMap<>();
        this.board = new PlaceBoard(dim);
    }

    /**
     * Logs in a user.
     * @param usernameRequest username for a user
     * @param out output stream for the user
     */
    public synchronized boolean login(String usernameRequest, ObjectOutputStream out)
    {
        try
        {
            if(users.containsKey(usernameRequest))
            {
                out.writeUnshared(new PlaceRequest<>(RequestType.ERROR, "Username taken"));
            }
            else
            {
                this.users.put(usernameRequest, out);
                System.out.println(usernameRequest + " has login");
                out.writeUnshared(new PlaceRequest<>(RequestType.LOGIN_SUCCESS, usernameRequest));
                out.writeUnshared(new PlaceRequest<>(RequestType.BOARD, this.board));
                return true;
            }
        }
        catch(IOException e)
        {
            System.err.println(e);
        }
        return false;
    }

    /**
     * If the user provides a invalid request, we tell them that and then shut them down
     *
     * @param username username of the user that provided invalid request
     * @param type type of request that gave us the issue
     *
     * @throws IOException
     */
    public void badRequest(String username, String type) throws IOException
    {
        ObjectOutputStream out = this.users.get(username);

        out.writeObject(new PlaceRequest<>(
                PlaceRequest.RequestType.ERROR, "Invalid request: " + type + ". Shutting down connection.")
        );

        out.flush();
    }

    /**
     * Logs a user out
     *
     * @param username username of the user logging out
     */
    public void logout(String username)
    {
        System.out.println(username + " has logout");
        users.remove(username);
    }

    /**
     * Alerts users who are logged in that a tile change request occurred
     *
     * @param tile the PlaceTile request that was made
     */
    public synchronized boolean tileChangeRequest(String username, PlaceTile tile)
    {
        if(!isValid(tile))
            return false;
        PlaceRequest<PlaceTile> changedTile = new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tile);
        for (ObjectOutputStream out : users.values()) {
            try
            {
                out.writeUnshared(changedTile);
                this.board.setTile(tile);
            }
            catch (IOException e) {
                System.err.println(e);
            }
        }
        return true;
    }

    /**
     * Checks to see if a move is valid or not before requesting a tile change.
     *
     * @param tile the PlaceTile that is being checked for validity.
     *
     * @return A boolean. True if the PlaceTile is valid for the board; false otherwise.
     */
    private boolean isValid(PlaceTile tile)
    {
        return this.board.isValid(tile);
    }

    /**
     * Returns a time stamp for the current time.
     *
     * @return A string of the format: MM/dd/YY at HH:MM:SS
     */
    private String now()
    {
        return TIME_STAMP.format(System.currentTimeMillis());
    }

    /**
     * If a server error occurs, we need to tell the clients that there's an error and shut down the connections.
     *
     */
    public void serverError()
    {
        PlaceRequest<String> error = new PlaceRequest<>(PlaceRequest.RequestType.ERROR,
                "An error has occurred in the server; shutting down connections... uwu...");
        for( ObjectOutputStream out : users.values() )
        {
            try
            {
                out.writeObject(error);
            }
            catch(IOException e)
            {
                System.err.println(e);
            }
        }
    }
}
