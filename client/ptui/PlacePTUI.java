package place.client.ptui;

import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.client.model.ClientModel;
import place.client.network.NetworkClient;

import java.io.*;
import java.util.*;


/**
 * This is the PTUI of the Reddit April Fool game.
 * Users are going to connect to the server via NetwortClient.
 * This class represents the controller part of a model-view-controller
 * triumvirate, in that part of its purpose is to forward user actions
 * to the remote server.
 *
 * @author Yu Qi Wu
 * @author Eve Cho
 */
public class PlacePTUI extends ConsoleApplication implements Observer {
    /**
     * The model which is used to house the board.
     */
    private ClientModel model;
    /**
     * The username a user wants to use.
     */
    public static String username;
    /**
     * Used to control the main game loop.
     */
    private boolean go;
    /**
     * The connection to the server through a NetworkClient.
     */
    private NetworkClient serverConn;

    /**
     * A simple synchronized go method to get the status of the program.
     *
     * @return the status of this.go, true if application should keep going, false otherwise.
     */
    private synchronized boolean goStat() {
        return this.go;
    }

    /**
     * Setting up the connection and the model
     */
    @Override
    public void init() {
        super.init();

        List<String> args = super.getArguments();

        String host = args.get(0);
        int port = Integer.parseInt(args.get(1));
        this.username = args.get(2);

        this.model = new ClientModel();

        try {
            this.serverConn = new NetworkClient(host, port, username, model);
        } catch (PlaceException e) {
            this.serverConn.close();
            e.printStackTrace();
        }

        this.serverConn.start();

        this.model.addObserver(this);

        this.go = true;
    }

    /**
     * The run function that is going to take user input to change the tile
     * @param userIn the user input
     * @param userOut the output to user
     */
    @Override
    public void go(Scanner userIn, PrintWriter userOut) {
        this.printBoard();
        userOut.println("Change tile: row col color?");

        while (this.goStat() && this.serverConn.goodToGo()) {
            String[] command = userIn.nextLine().trim().split(" ");

            if (command[0].equals("-1")) {
                this.serverConn.error("Exit command. Exiting PTUI.");
                this.go = false;
            } else if (command.length != 3) {
                this.serverConn.error("Please enter a valid command.");
                this.go = false;
            } else {
                try {
                    int row = Integer.parseInt(command[0]);
                    int col = Integer.parseInt(command[1]);
                    if (serverConn.color.contains(command[2])) {
                        PlaceColor color = serverConn.whatColor(command[2]);

                        PlaceTile tile = new PlaceTile(row, col, this.username, color, System.currentTimeMillis());

                        this.serverConn.sendTile(tile);
                    } else {
                        this.serverConn.error("Please enter a color value between 0 to 9 or a to f.");
                    }
                } catch (NumberFormatException e) {
                    this.serverConn.error("For row and column, number only.");
                }
            }
        }
    }

    /**
     * PTUI is closing, so close the network connection. Client will
     * get the message.
     */
    @Override
    public void stop() {
        this.serverConn.close();
    }

    /**
     * Refresh the board if any user made a change, then as this
     * user if s/he want to make any changes
     */
    public void refresh() {
        printBoard();
        System.out.println("Change tile: row col color?");
    }


    /**
     * Updating the board base on the model (Observable)
     * @param o The observer
     * @param arg The argument
     */
    @Override
    public void update(Observable o, Object arg) {
        assert o == this.model: "Update from non-board Observable";

        this.refresh();

    }

    /**
     * Prints the state of the current board
     */
    private void printBoard() {
        System.out.println(this.model.getBoard());
    }

    /**
     * The main function
     * @param args arguments
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceClient host port username");
            System.exit(0);
        }
        else{
            ConsoleApplication.launch(PlacePTUI.class, args);
        }
    }
}
