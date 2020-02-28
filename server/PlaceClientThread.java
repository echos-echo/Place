package place.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;

import place.PlaceException;
import place.PlaceTile;

import place.network.PlaceRequest;
import place.network.PlaceRequest.RequestType;

/**
 * The PlaceClientThread is the server-sided class that listens to the client's input and relays it to the NetworkServer.
 *
 * @author Eve Cho
 * @author Yuqi Wu
 */
public class PlaceClientThread
{
    /**
     * Milliseconds a user must wait before they can place their next tile
     */
    private final static int COOLDOWN_TIME = 500;
    /**
     * The ObjectInputStream from the client
     */
    private ObjectInputStream in;
    /**
     * The ObjectOutputStream from the client
     */
    private ObjectOutputStream out;
    /**
     * The networkServer for user's Place board
     */
    private NetworkServer networkServer;
    /**
     * String that represents a user's username
     */
    private String username;
    /**
     * Indicates whether or not a thread should keep running or not
     */
    private boolean go;
    /**
     * A boolean that indicates if this client is cooling down after placing a tile.
     */
    private boolean coolDown;
    /**
     * Getter that is used by run to tell if it should keep going.
     *
     * @return A boolean. True if this.go is set to true; false otherwise.
     */
    private synchronized boolean go()
    {
        return this.go;
    }
    /**
     * Setter that is used to stop the thread in the event of need to stop.
     */
    private void stop()
    {
        this.go = false;
    }

    // ===============================================

    /**
     * Constructs a new thread for a player once they connect to the server.
     *
     * @param player The player socket
     * @param networkServer The NetworkServer to communicate with
     *
     * @throws PlaceException
     */
    PlaceClientThread(Socket player, NetworkServer networkServer)
    {
        try
        {
            this.out = new ObjectOutputStream( player.getOutputStream() );
            this.in = new ObjectInputStream( player.getInputStream() );
            this.networkServer = networkServer;
            this.go = true;
        }
        catch(IOException e)
        {
            System.err.println(e);
        }
    }

    /**
     * Starts a new thread used for the user using the run method of the class.
     */
    public void start()
    {
        new Thread(this::run).start();
    }

    /**
     * Runs the PlaceClientThread.
     */
    private void run()
    {
        while(this.go())
        {
            try
            {
                PlaceRequest<?> request = ( PlaceRequest<?> ) in.readUnshared();

                switch(request.getType())
                {
                    case LOGIN:
                        if(username == null)
                        {
                            String usernameRequest = (String) request.getData();
                            if(login(usernameRequest))
                                this.username = usernameRequest;
                        }
                        else
                        {
                            badRequest(RequestType.LOGIN.toString());
                        }
                        break;
                    case CHANGE_TILE:
                        PlaceTile tile = (PlaceTile) request.getData();
                        if(!this.coolDown)
                        {
                            if (tileChangeRequest(tile))
                            {
                                new Thread(this::coolDown).start();
                            }
                            else
                            {
                                badRequest("INVALID TILE");
                            }
                        }
                        break;
                    case BOARD:
                        badRequest(RequestType.BOARD.toString());
                        break;
                    case ERROR:
                        badRequest(RequestType.ERROR.toString());
                        break;
                    case TILE_CHANGED:
                        badRequest(RequestType.TILE_CHANGED.toString());
                        break;
                    case LOGIN_SUCCESS:
                        badRequest(RequestType.LOGIN_SUCCESS.toString());
                        break;
                    default:
                        badRequest("UNKNOWN");
                }
            }
            catch(ClassNotFoundException e)
            {
                System.err.println(e);
            }
            catch(IOException e)
            {
                System.err.println(e);
                this.stop();
            }
        }
        this.close();
    }

    /**
     * "Hey, NetworkServer, let us log in"
     *
     * @param usernameRequest The username that we want to have
     *
     * @return A boolean. True if login was successful; false otherwise.
     */
    private boolean login(String usernameRequest)
    {
        return networkServer.login(usernameRequest, this.out);
    }

    /**
     * A small sleeper thread class which makes it so a user cannot send any PlaceTile for 500ms.
     */
    private synchronized void coolDown()
    {
        this.coolDown = true;
        try
        {
            Thread.sleep(COOLDOWN_TIME);
        }
        catch(InterruptedException e)
        {
            System.err.println(e);
        }
        this.coolDown = false;
    }

    /**
     * If we receive a bad request from a client, we send a message to a clients about it.
     *
     * @param type The type of error that is run into for alerting user.
     *
     * @throws IOException
     */
    private void badRequest(String type) throws IOException
    {
        this.networkServer.badRequest(this.username, type);

        this.stop();
    }

    /**
     * Requests the NetworkServer change the tile that user wants to change.
     *
     * @param tile The PlaceTile that is being requested to change.
     */
    private boolean tileChangeRequest(PlaceTile tile)
    {
        return this.networkServer.tileChangeRequest(this.username, tile);
    }


    /**
     * Closes the connections so we can shut down.
     */
    private void close()
    {
        try
        {
            if(this.username != null)
                this.networkServer.logout(this.username);
            this.in.close();
            this.out.close();
        }
        catch(IOException e)
        {
            System.err.println(e);
        }
    }
}