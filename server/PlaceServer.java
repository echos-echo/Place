package place.server;

import place.PlaceException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class PlaceServer implements Closeable {
    /**
     * The {@link ServerSocket} used to wait for incoming client connections.
     */
    private ServerSocket server;
    /**
     * Sentinel used to control the main game loop.
     */
    private boolean go;
    /**
     * The connection to the client through a NetworkServer.
     */
    private NetworkServer networkServer;

    /**
     * To get the status of the program.
     *
     * @return boolean
     */
    private synchronized boolean go(){
        return this.go;
    }

  /**
   * Creates a new {@link PlaceServer} that listens for incoming
   * connections on the specified port.
   *
   * @param port The port on which the server should listen for incoming
   *             connections.
   */
  public PlaceServer(int DIM, int port) {
    try {
        this.server = new ServerSocket(port);
        this.networkServer = new NetworkServer(DIM);
        System.out.println("Now the user can login...");
    }
    catch (IOException e){
      System.out.print(e);
      System.exit(0);
    }
    this.go = true;
  }

  /**
   * Closes the client {@link Socket}.
   */
  @Override
  public void close() {
      try {
          System.out.println("Shutting down server");
          this.server.close();
      } catch (IOException e) {
          System.err.println(e);
      }
  }

  /**
   * This server is not threaded-NOW IT IS, so only a single game can be played.
   * The server terminates if an exception is raised, or the game ends.
   *
   */
  public void run() throws PlaceException {
      while(this.go()){
          try{
              new PlaceClientThread(server.accept(), this.networkServer).start();
          }
          catch(IOException e){
              System.err.print(e);
          }
      }
  }

  /**
   * Starts a new {@link PlaceServer}.
   *
   * @param args Used to specify the port on which the server should listen
   *             for incoming client connections.
   */
  public static void main(String[] args) {
      if(args.length != 2)
      {
          System.err.println("Usage: java PlaceServer port DIM");
          return;
      }

      int dim = Integer.parseInt(args[1]);
      int port = Integer.parseInt(args[0]);

      try ( PlaceServer server = new PlaceServer(dim, port) )
      {
          server.run();
      }
      catch (PlaceException e)
      {
          System.err.println(e);
      }
  }
}
