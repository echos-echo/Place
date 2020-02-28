package place.client.network;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.client.model.ClientModel;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static place.network.PlaceExchange.*;

/**
 * The client side network interface to a Reddit April Fool game server.
 * Each of the players in a game gets its own connection to the server.
 * This class represents the controller part of a model-view-controller.
 *
 * @author Robert St Jacques @ RIT SE
 * @author Sean Strout @ RIT CS
 * @author Eve Cho
 * @author Yu Qi Wu
 */
public class NetworkClient {

    /**
     * The number of milliseconds a user must wait before they can send their next tile.
     */
    private final static int WAIT_TIME = 500;

    /**
     * A boolean that indicates if this client is wait time after placing a tile.
     *
     * If it is, the client cannot send a new piece. If it is true, and a client tries to send a PlaceTile,
     * it displays an error.
     */
    private boolean wait;

    /**
     * Turn on if standard output debug messages are desired.
     */
    private static final boolean DEBUG = false;

    /**
     * Print method that does something only if DEBUG is true
     *
     * @param logMsg the message to log
     */
    private static void dPrint( Object logMsg ) {
        if ( NetworkClient.DEBUG ) {
            System.out.println( logMsg );
        }
    }

    /**
     * The {@link Socket} used to communicate with the PlaceServer.
     */
    private Socket sock;

    /**
     * The {@link ObjectInputStream} used to read requests from the PlaceServer.
     */
    private ObjectInputStream networkIn;

    /**
     * The {@link java.io.ObjectOutputStream} used to write responses to the PlaceServer.
     */
    private ObjectOutputStream networkOut;

    /**
     * The {@link PlaceBoard} used to keep track of the state of the game.
     */
    private ClientModel board;

    /**
     * Sentinel used to control the main game loop.
     */
    private boolean go;
    /**
     * String of all possible user input of colors
     */
    public static final String color = "0123456789abcdef";

    /**
     * Accessor that takes multithreaded access into account
     *
     * @return whether it ok to continue or not
     */
    public boolean goodToGo() {
        return this.go;
    }

    /**
     * Multithread-safe mutator
     */
    private synchronized void stop() {
        this.go = false;
    }

    /**
     * Hooks up with a Place server already running and waiting for
     * clients to connect. It waits and listens to the ServerSocket it
     * connects to.
     *
     * @param hostname the name of the host running the server program
     * @param port     the port of the server socket on which the server is
     *                 listening
     * @param username the username
     * @throws PlaceException If there is a problem opening the connection
     */
    public NetworkClient(String hostname, int port, String username, ClientModel board)
            throws PlaceException {
        try {
            this.sock = new Socket( hostname, port );
            this.networkOut = new ObjectOutputStream( sock.getOutputStream() );
            networkOut.flush();
            this.networkIn = new ObjectInputStream( sock.getInputStream() );
            this.board = board;

            this.networkOut.writeUnshared(new PlaceRequest<String>(PlaceRequest.RequestType.LOGIN, username));

            PlaceRequest req = (PlaceRequest) this.networkIn.readUnshared();
            NetworkClient.dPrint( "Connected to server " + this.sock );
            switch (req.getType())
            {
                case LOGIN_SUCCESS:
                    System.out.println("You have successfully login!");
                    break;
                case ERROR:
                    error("Failed to join Place server.");
                    error("Server response: " + req.getData() + ".");
                    this.close();
                    throw new PlaceException("Unable to join.");
                default:
                    error("Bad response received from server. Disconnecting.");
                    this.close();
                    throw new PlaceException("Unable to join.");
            }

            req = (PlaceRequest) this.networkIn.readUnshared();
            if(req.getType() == PlaceRequest.RequestType.BOARD)
                this.board.initialize( (PlaceBoard) req.getData() );
            else
                throw new PlaceException("No board received.");

            this.go = true;
        }
        catch( IOException | ClassNotFoundException e ) {
            throw new PlaceException( e );
        }
    }

    /**
     * Start the thread
     */
    public void start()
    {
        new Thread(this::run).start();
    }


    /**
     * Called when the server sends a message saying that
     * the Place board is damaged. Shuts down connection.
     *
     * @param arguments The error message sent from the PlaceServer
     */
    public void error( String arguments ) {
        NetworkClient.dPrint( '!' + ERROR + ',' + arguments );
        dPrint( "Fatal error: " + arguments );
        System.err.println( arguments );
        this.stop();
    }

    /**
     * This method should be called at the end of the game to
     * close the client connection.
     */
    public void close() {
        try {
            this.sock.close();
            this.networkOut.close();
            this.networkIn.close();
        }
        catch( IOException e ) {
            // squash
            System.err.println("???");
        }
    }

    /**
     * UI wants to send a new move to the server.
     *
     * @param tile the tile
     */
    public synchronized void sendTile(PlaceTile tile) {
        if (! this.wait) {
            try {
                this.networkOut.writeUnshared(new PlaceRequest<PlaceTile>(PlaceRequest.RequestType.CHANGE_TILE, tile));
                this.networkOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread(this::waiting).start();
        }
        else{
            System.out.println("You must wait 0.5 second between each change");
        }
    }

    private void waiting(){
        this.wait = true;
        try
        {
            Thread.sleep(WAIT_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.wait = false;
    }

    private void tileChanged(PlaceTile tile){
        this.board.tileChanged(tile);
    }

    /**
     * Run the main client loop. Intended to be started as a separate
     * thread internally. This method is made private so that no one
     * outside will call it or try to start a thread on it.
     */
    private void run() {
        while ( this.goodToGo() ) {
            try {
                PlaceRequest req = (PlaceRequest) this.networkIn.readUnshared();
                NetworkClient.dPrint( "Net message in = \"" + req.getType() + '"' );

                switch ( req.getType() ) {
                    case TILE_CHANGED:
                        // remember to sleep(500)!!!
                        System.out.println("Tile changed");
                        /**
                         * the return is a tile, so now user can modify their board
                         *  according to the data (tile) in req
                         */
                        tileChanged( (PlaceTile) req.getData() );
                        break;
                    case BOARD:
                        error("Did not expect this response.");
                        break;
                    case CHANGE_TILE:
                        error("Did not expect this response.");
                        break;
                    case LOGIN:
                        error("Did not expect this response.");
                        break;
                    case LOGIN_SUCCESS:
                        error("Did not expect this response.");
                        break;
                    case ERROR:
                        error((String)req.getData());
                        break;
                    default:
                        error("Did not expect this response.");
                        break;
                }
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("You are disconnected.");
                this.stop();
            }
        }
        System.out.println("going to close the socket");
        this.close();
    }

    public static PlaceColor whatColor(String color){
        switch (color) {
            case "0":
                return PlaceColor.BLACK;
            case "1":
                return PlaceColor.GRAY;
            case "2":
                return PlaceColor.SILVER;
            case "3":
                return PlaceColor.WHITE;
            case "4":
                return PlaceColor.MAROON;
            case "5":
                return PlaceColor.RED;
            case "6":
                return PlaceColor.OLIVE;
            case "7":
                return PlaceColor.YELLOW;
            case "8":
                return PlaceColor.GREEN;
            case "9":
                return PlaceColor.LIME;
            case "a":
                return PlaceColor.TEAL;
            case "b":
                return PlaceColor.AQUA;
            case "c":
                return PlaceColor.NAVY;
            case "d":
                return PlaceColor.BLUE;
            case "e":
                return PlaceColor.PURPLE;
            case "f":
                return PlaceColor.FUCHSIA;
            default:
                return PlaceColor.WHITE;
        }
    }

}
