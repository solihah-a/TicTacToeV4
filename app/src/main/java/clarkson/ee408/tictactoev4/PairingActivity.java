package clarkson.ee408.tictactoev4;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import clarkson.ee408.tictactoev4.client.*;
import clarkson.ee408.tictactoev4.model.*;
import clarkson.ee408.tictactoev4.socket.*;

public class PairingActivity extends AppCompatActivity {

    private final String TAG = "PAIRING";

    private Gson gson;

    private TextView noAvailableUsersText;
    private RecyclerView recyclerView;
    private AvailableUsersAdapter adapter;

    private Handler handler;
    private Runnable refresh;

    private boolean shouldUpdatePairing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        Log.e(TAG, "App is now created");
      
        gson = new GsonBuilder().serializeNulls().create();


        //Setting the username text
        TextView usernameText = findViewById(R.id.text_username);

        String username = getIntent().getStringExtra("username");
        usernameText.setText(username);


        //Getting UI Elements
        noAvailableUsersText = findViewById(R.id.text_no_available_users);
        recyclerView = findViewById(R.id.recycler_view_available_users);

        //Setting up recycler view adapter
        adapter = new AvailableUsersAdapter(this, this::sendGameInvitation);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateAvailableUsers(null);

        handler = new Handler();
        refresh = () -> {
            //call getPairingUpdate if shouldUpdatePairing is true
            if (shouldUpdatePairing) {
                getPairingUpdate();
            }
            handler.postDelayed(refresh, 1000);
        };
        handler.post(refresh);
    }

    /**
     * Send UPDATE_PAIRING request to the server
     */
    private void getPairingUpdate() {
        //Send an UPDATE_PAIRING request to the server. If SUCCESS call handlePairingUpdate(). Else, Toast the error
        Request request = new Request();
        request.setType(Request.RequestType.UPDATE_PAIRING);

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                PairingResponse pr = SocketClient.getInstance().sendRequest(request, PairingResponse.class);

                if ((pr == null) || (pr.getStatus() == Response.ResponseStatus.FAILURE)) {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, "Pairing update failed.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                AppExecutors.getInstance().mainThread().execute(() ->
                        handlePairingUpdate(pr)
                );

            } catch (Exception e) {
                Log.e(TAG, "Error updating pairing", e);
            }
        });
    }

    /**
     * Handle the PairingResponse received form the server
     * @param response PairingResponse from the server
     */
    private void handlePairingUpdate(PairingResponse response) {
        //handle availableUsers by calling updateAvailableUsers()
        updateAvailableUsers(response.getAvailableUsers());

        //handle invitationResponse. First by sending acknowledgement calling sendAcknowledgement()
        //If the invitationResponse is ACCEPTED, Toast an accept message and call beginGame
        //If the invitationResponse is DECLINED, Toast a decline message
        Event invitationResponse = response.getInvitationResponse();
        if (invitationResponse != null) {

            sendAcknowledgement(invitationResponse);

            if (invitationResponse.getStatus() == Event.EventStatus.ACCEPTED) {
                Toast.makeText(this, invitationResponse.getOpponent() + " accepted your request!", Toast.LENGTH_SHORT).show();
                beginGame(invitationResponse, 1);
            } else if (invitationResponse.getStatus() == Event.EventStatus.DECLINED) {
                Toast.makeText(this, invitationResponse.getOpponent() + " declined your request.", Toast.LENGTH_SHORT).show();
            }
        }

        //handle invitation by calling createRespondAlertDialog()
        if (response.getInvitation() != null) {
            createRespondAlertDialog(response.getInvitation());
        }
    }

    /**
     * Updates the list of available users
     * @param availableUsers list of users that are available for pairing
     */
    public void updateAvailableUsers(List<User> availableUsers) {
        adapter.setUsers(availableUsers);
        if (adapter.getItemCount() <= 0) {
            //show noAvailableUsersText and hide recyclerView
            noAvailableUsersText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            //hide noAvailableUsersText and show recyclerView
            noAvailableUsersText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sends game invitation to an
     * @param userOpponent the User to send invitation to
     */
    private void sendGameInvitation(User userOpponent) {
        //Send an SEND_INVITATION request to the server. If SUCCESS Toast a success message. Else, Toast the error
        Request request = new Request();
        request.setType(Request.RequestType.SEND_INVITATION);
        request.setData(userOpponent.getUsername());

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Response ir = SocketClient.getInstance().sendRequest(request, Response.class);

                if (ir == null) {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, "Failure sending invitation.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                if (ir.getStatus() == Response.ResponseStatus.SUCCESS) {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, "Invitation sent to " + userOpponent.getUsername(), Toast.LENGTH_SHORT).show());
                } else {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, ir.getMessage(), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending invitation", e);
            }
        });
    }

    /**
     * Sends an ACKNOWLEDGE_RESPONSE request to the server
     * Tell server i have received accept or declined response from my opponent
      */
    private void sendAcknowledgement(Event invitationResponse) {
        //Send an ACKNOWLEDGE_RESPONSE request to the server.
        if (invitationResponse == null) {
            Toast.makeText(this, "Invalid event.", Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request();
        request.setType(Request.RequestType.ACKNOWLEDGE_RESPONSE);
        request.setData(String.valueOf(invitationResponse.getEventId()));

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Response response = SocketClient.getInstance()
                        .sendRequest(request, Response.class);

                if (response == null) {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, "Acknowledge failed.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                if (response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, "Acknowledge sent successfully.", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Error acknowledging response", e);
            }
        });
    }

    /**
     * Create a dialog showing incoming invitation
     * @param invitation the Event of an invitation
     */
    private void createRespondAlertDialog(Event invitation) {
        shouldUpdatePairing = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Game Invitation");
        builder.setMessage(invitation.getSender() + " has requested to play with you");
        builder.setPositiveButton("Accept", (dialogInterface, i) -> acceptInvitation(invitation));
        builder.setNegativeButton("Decline", (dialogInterface, i) -> declineInvitation(invitation));
        builder.show();
    }

    /**
     * Sends an ACCEPT_INVITATION to the server
     * @param invitation the Event invitation to accept
     */
    private void acceptInvitation(Event invitation) {
        //Send an ACCEPT_INVITATION request to the server. If SUCCESS beginGame() as player 2. Else, Toast the error
        if (invitation == null) {
            Toast.makeText(this, "Invalid invitation.", Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request();
        request.setType(Request.RequestType.ACCEPT_INVITATION);
        request.setData(String.valueOf(invitation.getEventId()));

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Response response = SocketClient.getInstance()
                        .sendRequest(request, Response.class);

                if (response == null) {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, "Accept invitation failed.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                if (response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            beginGame(invitation, 2)
                    );
                } else {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Error accepting invitation", e);
            }
        });
    }

    /**
     * Sends an DECLINE_INVITATION to the server
     * @param invitation the Event invitation to decline
     */
    private void declineInvitation(Event invitation) {
        //Send a DECLINE_INVITATION request to the server. If SUCCESS response, Toast a message, else, Toast the error
        //set shouldUpdatePairing to true after DECLINE_INVITATION is sent.
        if (invitation == null) {
            Toast.makeText(this, "Invalid invitation.", Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request();
        request.setType(Request.RequestType.DECLINE_INVITATION);
        request.setData(String.valueOf(invitation.getEventId()));

        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Response response = SocketClient.getInstance()
                        .sendRequest(request, Response.class);

                if (response == null) {
                    AppExecutors.getInstance().mainThread().execute(() ->
                            Toast.makeText(this, "Decline invitation failed.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (response.getStatus() == Response.ResponseStatus.SUCCESS) {
                        Toast.makeText(this, "Invitation declined.", Toast.LENGTH_SHORT).show();
                        shouldUpdatePairing = true;
                    } else {
                        Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error declining invitation", e);
            }
        });
    }

    /**
     *
     * @param pairing the Event of pairing
     * @param player either 1 or 2
     */
    private void beginGame(Event pairing, int player) {
        //set shouldUpdatePairing to false
        //start MainActivity and pass player as data
        shouldUpdatePairing = false;
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("player", player);
        intent.putExtra("pairing", gson.toJson(pairing));
        startActivity(intent);

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        shouldUpdatePairing = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);

        shouldUpdatePairing = false;


        //logout by calling close() function of SocketClient
        //will cause some problems so commented out
        // SocketClient.getInstance().close();
    }

}