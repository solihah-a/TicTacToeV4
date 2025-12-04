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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import clarkson.ee408.tictactoev4.client.*;
import clarkson.ee408.tictactoev4.socket.*;

public class MainActivity extends AppCompatActivity {
    private TicTacToe tttGame;
    private Button[][] buttons;
    private TextView status;
    private Gson gson;
    private boolean shouldRequestMove;
    private SocketClient socketClient;
    private Handler handler;
    private GameMoveRunnable gameMoveTaskRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get player value from PairingActivity (default to 1 if not found)
        int player = getIntent().getIntExtra("player", 1);

        this.tttGame = new TicTacToe(player);
        this.gson = new GsonBuilder().serializeNulls().create();
        socketClient = SocketClient.getInstance();
        shouldRequestMove = true;

        buildGuiByCode();
        updateTurnStatus();

        handler = new Handler();
        gameMoveTaskRunnable = new GameMoveRunnable(this, handler);
        handler.post(gameMoveTaskRunnable);
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

                        // Check if game is not active
                        if (!response.isActive()) {
                            // Game is inactive - end the game
                            status.setText(response.getMessage());
                            status.setBackgroundColor(Color.RED);
                            enableButtons(false);
                            shouldRequestMove = false;
                            tttGame = null;
                            return; // Exit early, don't process moves
                        }

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

    /**
     * Sends ABORT_GAME request to server when user leaves an ongoing game
     */
    private void abortGame() {
        Log.d("MainActivity", "Sending ABORT_GAME request");

        // Create a Request object with type ABORT_GAME
        Request request = new Request();
        request.setType(Request.RequestType.ABORT_GAME);

        // Send request asynchronously using AppExecutors
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                // Don't send an ABORT_GAME request if the game is already inactive
                if (checkGameIsInactive()) {
                    return;
                }

                Response response = socketClient.sendRequest(request, Response.class);

                // Process response in main thread to show Toast
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                        Toast.makeText(MainActivity.this,
                                "Game aborted successfully",
                                Toast.LENGTH_SHORT).show();
                        Log.d("MainActivity", "Game aborted successfully");
                    } else {
                        String errorMsg = "Failed to abort game";
                        if (response != null && response.getMessage() != null) {
                            errorMsg = response.getMessage();
                        }
                        Toast.makeText(MainActivity.this,
                                errorMsg,
                                Toast.LENGTH_SHORT).show();
                        Log.e("MainActivity", errorMsg);
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Error aborting game", e);
                // Show Toast in main thread
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(MainActivity.this,
                            "Error sending abort game request",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Sends COMPLETE_GAME request to server when user leaves after game completion
     */
    private void completeGame() {
        Log.d("MainActivity", "Sending COMPLETE_GAME request");

        // Create a Request object with type COMPLETE_GAME
        Request request = new Request();
        request.setType(Request.RequestType.COMPLETE_GAME);

        // Send request asynchronously using AppExecutors
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                // Don't send a COMPLETE_GAME request if the game is already inactive
                if (checkGameIsInactive()) {
                    return;
                }

                Response response = socketClient.sendRequest(request, Response.class);

                // Process response in main thread to show Toast
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                        Toast.makeText(MainActivity.this,
                                "Game completed successfully",
                                Toast.LENGTH_SHORT).show();
                        Log.d("MainActivity", "Game completed successfully");
                    } else {
                        String errorMsg = "Failed to complete game";
                        if (response != null && response.getMessage() != null) {
                            errorMsg = response.getMessage();
                        }
                        Toast.makeText(MainActivity.this,
                                errorMsg,
                                Toast.LENGTH_SHORT).show();
                        Log.e("MainActivity", errorMsg);
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Error completing game", e);
                // Show Toast in main thread
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(MainActivity.this,
                            "Error sending complete game request",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean checkGameIsInactive() throws IOException {
        Request request = new Request();
        request.setType(Request.RequestType.REQUEST_MOVE);
        GamingResponse response = socketClient.sendRequest(request, GamingResponse.class);
        return response == null || !response.isActive();
    }

    @Override
    protected void onDestroy() {
        // Call parent's onDestroy first
        super.onDestroy();

        // Stop the repetitive Handler
        if (handler != null) {
            // Remove the specific runnable
            if (gameMoveTaskRunnable != null) {
                handler.removeCallbacks(gameMoveTaskRunnable); // Use specific runnable
            }
            // Also remove any other callbacks/messages
            handler.removeCallbacksAndMessages(null);
        }

        // Check game state and call appropriate method
        if (tttGame != null) {
            if (tttGame.isGameOver()) {
                completeGame(); // Game ended normally
            } else {
                abortGame(); // Game was aborted
            }
        } else {
            // If tttGame is null, the game was already ended. So it is safe to call abort just in case
            abortGame();
        }

        Log.d("MainActivity", "Activity destroyed");
    }

    private boolean isMyTurn() {
        return this.tttGame.getPlayer() == this.tttGame.getTurn();
    }

    private void updateTurnStatus() {
        runOnUiThread(() -> {
            if (isMyTurn()) {
                status.setText("Your Turn");
                enableButtons(true);
                requestMove();
            } else {
                status.setText("Waiting for Opponent");
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

                enableButtons(true);
                resetButtons();
                shouldRequestMove = true;
                status.setBackgroundColor(Color.GREEN);
                status.setText(tttGame.result());
                updateTurnStatus();
            } else if (id == -2) // NO button
                MainActivity.this.finish();
        }
    }
}