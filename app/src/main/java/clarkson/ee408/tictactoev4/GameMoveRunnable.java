package clarkson.ee408.tictactoev4;

import android.os.Handler;

/**
 * A {@link Runnable} to periodically poll the server for an updated game state.
 */
public class GameMoveRunnable implements Runnable {

    public static final int POLL_DELAY_MILLIS = 1000; // 1 second

    private final MainActivity mainActivity;
    private final Handler handler;

    /**
     * Creates a new instance of {@code GameMoveTaskRunnable}.
     *
     * @param mainActivity the instance of MainActivity
     * @param handler the instance of Handler that is executing this
     */
    public GameMoveRunnable(MainActivity mainActivity, Handler handler) {
        this.mainActivity = mainActivity;
        this.handler = handler;
    }

    @Override
    public void run() {
        this.mainActivity.requestMove();
        this.handler.postDelayed(this, POLL_DELAY_MILLIS);
    }
}
