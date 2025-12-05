# Milestone 4 Report

* Link to recording for test procedure: https://drive.google.com/file/d/1ABu1wgRRjZgHrmeeSaqCRM2cTHek77xz/view?usp=drive_link
* Explain why we must send a REQUEST_MOVE request even if it is the current user’s turn to move.
   - Even when it is the current player’s turn, REQUEST_MOVE is sent so the client can confirm with the server that the game and the opponent are still active.
     The server’s reply will indicate if the game has been terminated (for example, because the opponent disconnected), allowing the app to end the game immediately,
     update the interface accordingly, and prevent the user from making moves in a game that has already finished.
* If two users A and B are in the middle of a game, what happens when user B's device suddenly goes off (i.e., shuts down); either it crashes or the battery
  drains? Explain what will happen, from how the server will know user B is offline to how user A will know the game is inactive.
   - When user B’s device suddenly shuts down, the activity lifecycle does not complete normally, so no explicit “abort game” request is sent to the server.
     From the server’s point of view, user B simply stops sending messages and remains marked as in-game until a timeout is reached on their pending move.
     While this is happening, user A’s client continues to send periodic move-status requests to the server. Once the server times out waiting for user B’s move,
     it flags the game as inactive or aborted. On the next status or REQUEST_MOVE call from user A, the server responds that the game is no longer active,
     allowing user A’s app to update the UI (for example, showing that the opponent has disconnected, disabling the board, and ending the match).
* Can the grid in the MainActivity class (i.e., the TicTacToe board) be designed
  using XML layout instead of by code? Support your answer.
  -  The board can be defined in an XML layout file (for example, activity_main.xml) by declaring a GridLayout with nine Button elements for the 3×3 grid and a TextView for the status
     line. Each button gets a unique ID, and MainActivity simply calls setContentView(R.layout.activity_main) in onCreate() and uses findViewById() to access the buttons,
     eliminating the need for buildGuiByCode(). This XML-based design makes it much easier to adjust spacing, colors, or layout structure later, because UI changes
     are done by editing the XML without modifying Java logic.
  

