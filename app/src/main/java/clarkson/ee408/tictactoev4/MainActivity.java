package clarkson.ee408.tictactoev4;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import clarkson.ee408.tictactoev4.client.*;
import clarkson.ee408.tictactoev4.socket.*;

public class MainActivity extends AppCompatActivity {

    private static final int STARTING_PLAYER_NUMBER = 1;

    private TicTacToe tttGame;
    private Button[][] buttons;
    private TextView status;
    private Gson gson;
    private boolean shouldRequestMove;
    private SocketClient socketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.tttGame = new TicTacToe(STARTING_PLAYER_NUMBER);
        this.gson = new GsonBuilder().serializeNulls().create();
        socketClient = SocketClient.getInstance();
        shouldRequestMove = false;

        buildGuiByCode();
        updateTurnStatus();

        Handler handler = new Handler();
        GameMoveRunnable runnable = new GameMoveRunnable(this, handler);
        handler.post(runnable);
    }

    /**
     * Sends a request to the server to ask for a game move made by the other player.
     */
    public void requestMove() {
        if (!shouldRequestMove) {
            return; // Only request moves when it's our turn
        }

        // Create Request object with type REQUEST_MOVE
        Request request = new Request();
        request.setType(Request.RequestType.REQUEST_MOVE);
        // You might want to include game state or player info
        request.setData(""); // Add any necessary data

        // Use SocketClient to send request in networkIO thread
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                GamingResponse response = socketClient.sendRequest(request, GamingResponse.class);

                // Process response in main thread
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                        // Get the move from GamingResponse (already parsed)
                        int moveValue = response.getMove();

                        // Validate move value
                        if (moveValue >= 0 && moveValue < TicTacToe.SIDE * TicTacToe.SIDE) {
                            // Convert single integer move to row and column
                            int row = moveValue / TicTacToe.SIDE;
                            int col = moveValue % TicTacToe.SIDE;

                            // Utilize update() function to add changes to the board
                            update(row, col);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Error requesting move", e);
            }
        });
    }

    /**
     * Sends the player's move to the server.
     * @param move The move position (0-8) to send.
     */
    public void sendMove(int move) {
        // Create a Request object with type SEND_MOVE
        Request request = new Request();
        request.setType(Request.RequestType.SEND_MOVE);
        request.setData("" + move);

        // Send request asynchronously using AppExecutors
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                GamingResponse response = socketClient.sendRequest(request, GamingResponse.class);
                Log.d("MainActivity", "Sent move: " + move + ", response: " + response);

                // Handle response (optional)
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                        Log.d("MainActivity", "Move acknowledged by server");
                    } else {
                        Log.e("MainActivity", "Failed to send move to server");
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Error sending move", e);
            }
        });
    }

    private boolean isMyTurn() {
        return this.tttGame.getPlayer() == this.tttGame.getTurn();
    }

    private void updateTurnStatus() {
        runOnUiThread(() -> {
            if (isMyTurn()) {
                status.setText("Your Turn");
                shouldRequestMove = false;
                enableButtons(true);
                requestMove();
            } else {
                status.setText("Waiting for Opponent");
                shouldRequestMove = true;
                enableButtons(false);
            }
        });
    }

    public void buildGuiByCode() {
        // Get width of the screen
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int w = size.x / TicTacToe.SIDE;

        // Create the layout manager as a GridLayout
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(TicTacToe.SIDE);
        gridLayout.setRowCount(TicTacToe.SIDE + 2);

        // Create the buttons and add them to gridLayout
        buttons = new Button[TicTacToe.SIDE][TicTacToe.SIDE];
        ButtonHandler bh = new ButtonHandler();

        gridLayout.setUseDefaultMargins(true);

        for (int row = 0; row < TicTacToe.SIDE; row++) {
            for (int col = 0; col < TicTacToe.SIDE; col++) {
                buttons[row][col] = new Button(this);
                buttons[row][col].setTextSize((int) (w * .2));
                buttons[row][col].setOnClickListener(bh);
                GridLayout.LayoutParams bParams = new GridLayout.LayoutParams();

                bParams.topMargin = 0;
                bParams.bottomMargin = 10;
                bParams.leftMargin = 0;
                bParams.rightMargin = 10;
                bParams.width = w - 10;
                bParams.height = w - 10;
                buttons[row][col].setLayoutParams(bParams);
                gridLayout.addView(buttons[row][col]);
            }
        }

        // set up layout parameters of 4th row of gridLayout
        status = new TextView(this);
        GridLayout.Spec rowSpec = GridLayout.spec(TicTacToe.SIDE, 2);
        GridLayout.Spec columnSpec = GridLayout.spec(0, TicTacToe.SIDE);
        GridLayout.LayoutParams lpStatus
                = new GridLayout.LayoutParams(rowSpec, columnSpec);
        status.setLayoutParams(lpStatus);

        // set up status' characteristics
        status.setWidth(TicTacToe.SIDE * w);
        status.setHeight(w);
        status.setGravity(Gravity.CENTER);
        status.setBackgroundColor(Color.GREEN);
        status.setTextSize((int) (w * .15));
        status.setText(tttGame.result());

        gridLayout.addView(status);

        // Set gridLayout as the View of this Activity
        setContentView(gridLayout);
    }

    public void update(int row, int col) {
        int play = tttGame.play(row, col);
        if (play == 1)
            buttons[row][col].setText("X");
        else if (play == 2)
            buttons[row][col].setText("O");
        if (tttGame.isGameOver()) {
            if (this.tttGame.getPlayer() == this.tttGame.whoWon()) {
                status.setBackgroundColor(Color.GREEN);
            } else {
                status.setBackgroundColor(Color.RED);
            }
            enableButtons(false);
            status.setText(tttGame.result());
            showNewGameDialog();    // offer to play again
        } else {
            updateTurnStatus();
        }
    }

    public void enableButtons(boolean enabled) {
        for (int row = 0; row < TicTacToe.SIDE; row++)
            for (int col = 0; col < TicTacToe.SIDE; col++)
                buttons[row][col].setEnabled(enabled);
    }

    public void resetButtons() {
        for (int row = 0; row < TicTacToe.SIDE; row++)
            for (int col = 0; col < TicTacToe.SIDE; col++)
                buttons[row][col].setText("");
    }

    public void showNewGameDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(tttGame.result());
        alert.setMessage("Do you want to play again?");
        PlayDialog playAgain = new PlayDialog();
        alert.setPositiveButton("YES", playAgain);
        alert.setNegativeButton("NO", playAgain);
        alert.show();
    }

    private class ButtonHandler implements View.OnClickListener {
        public void onClick(View v) {
            Log.d("button clicked", "button clicked");

            for (int row = 0; row < TicTacToe.SIDE; row++) {
                for (int column = 0; column < TicTacToe.SIDE; column++) {
                    if (v == buttons[row][column]) {
                        // Calculate move index
                        int move = row * TicTacToe.SIDE + column;

                        // 1 - Send move to server first
                        sendMove(move);

                        // 2 - Then update board locally
                        update(row, column);
                    }
                }
            }
        }
    }

    private class PlayDialog implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int id) {
            if (id == -1) /* YES button */ {
                tttGame.resetGame();
                int currentPlayer = tttGame.getPlayer();
                int nextPlayer = (currentPlayer == 1) ? 2 : 1;
                tttGame.setPlayer(nextPlayer);
                enableButtons(true);
                resetButtons();
                status.setBackgroundColor(Color.GREEN);
                status.setText(tttGame.result());
                updateTurnStatus();
            } else if (id == -2) // NO button
                MainActivity.this.finish();
        }
    }
}