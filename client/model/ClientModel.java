package place.client.model;

import java.util.Observable;
import place.PlaceBoard;
import place.PlaceException;
import place.PlaceTile;

public class ClientModel extends Observable {
    private PlaceBoard board;
    private int DIM;

    public ClientModel(){
        // constructor
    }

    public void initialize(PlaceBoard board) throws PlaceException{
        this.board = board;
        this.DIM = this.board.DIM;
    }

    public int getDIM(){ return this.DIM; }

    public PlaceBoard getBoard(){ return this.board;}

    public PlaceTile getTile(int row, int col){
        return this.board.getTile(row, col);
    }

    public void tileChanged(PlaceTile tile){
        this.board.setTile(tile);

        super.setChanged();
        super.notifyObservers(tile);
    }



}
