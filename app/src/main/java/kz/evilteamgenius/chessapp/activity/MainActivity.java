package kz.evilteamgenius.chessapp.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.Arrays;

import kz.evilteamgenius.chessapp.BoardView;
import kz.evilteamgenius.chessapp.R;
import kz.evilteamgenius.chessapp.engine.Board;
import kz.evilteamgenius.chessapp.engine.Coordinate;
import kz.evilteamgenius.chessapp.engine.Game;
import kz.evilteamgenius.chessapp.engine.Match;
import kz.evilteamgenius.chessapp.fragments.GameFragment;
import kz.evilteamgenius.chessapp.fragments.NavigationPageFragment;
import kz.evilteamgenius.chessapp.models.MatchMakingMessage;
import kz.evilteamgenius.chessapp.models.MoveMessage;
import kz.evilteamgenius.chessapp.models.enums.MatchMakingMessageType;
import kz.evilteamgenius.chessapp.models.enums.MoveMessageType;
import timber.log.Timber;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompCommand;
import ua.naiksoftware.stomp.dto.StompHeader;
import ua.naiksoftware.stomp.dto.StompMessage;

@SuppressWarnings({"FieldCanBeLocal", "ResultOfMethodCallIgnored", "CheckResult"})

public class MainActivity extends AppCompatActivity implements NavigationPageFragment.OnFragmentInteractionListener {

    public static StompClient stompClient;
    Thread thread;
    private Fragment fragment;
    FirebaseDatabase database;
    DatabaseReference roomRef, gameRef, playerRef, gameIdRef;
    int mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragment = new NavigationPageFragment();
        replaceFragment(fragment);
        database = FirebaseDatabase.getInstance();
        roomRef = database.getReference().child("waitingRooms");
        gameRef = database.getReference().child("games");
        playerRef = database.getReference().child("players");
        gameIdRef = database.getReference().child("players").child(getUsername()).child("gameId");

        //roomRef.push().setValue(getUsername());

        // Read from the database
//        myRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                String value = dataSnapshot.getValue(String.class);
//                Log.d("REALTIME", "Value is: " + value);
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//                Log.w("REALTIME", "Failed to read value.", error.toException());
//            }
//        });
    }


    public void replaceFragment(Fragment fragment) {
        FragmentTransaction tr = this.getSupportFragmentManager().beginTransaction();
        tr.replace(R.id.frame, fragment);
        tr.commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    public void connectAndMakeMatch(int LAST_SELECTED_MATCH_MODE) {
        mode = LAST_SELECTED_MATCH_MODE;
//        Timber.d("Start connecting to server");
//        roomRef.child("1").child(getUsername()).removeValue();
//        roomRef.child("2").child(getUsername()).removeValue();
//        roomRef.child("3").child(getUsername()).removeValue();
//        roomRef.child("4").child(getUsername()).removeValue();
        roomRef.child(String.valueOf(mode)).push().child("player").setValue(getUsername());
        playerRef.child(getUsername()).child("gameId").setValue(null);
        playerRef.child(getUsername()).child("gameType").setValue(mode);
        gameIdRef.addValueEventListener(gameIdListener);

    }

    public void getMove(BoardView board) {
        stompClient.topic(Game.roomAdress).subscribe(stompMessage -> {
            MoveMessage message = new Gson().fromJson(stompMessage.getPayload(), MoveMessage.class);
            if (message.getPlayerID().equals(Game.myPlayerUserame))
                return;
            Timber.d("Received: *****\n %s ***** \n", message.toString());
            Coordinate pos1 = new Coordinate(message.getFrom_x(), message.getFrom_y(), Board.rotations);
            Coordinate pos2 = new Coordinate(message.getTo_x(), message.getTo_y(), Board.rotations);
            Board.moveWhenReceived(pos1, pos2);
            // Stuff that updates the UI
            this.runOnUiThread(board::invalidate);
        }, throwable -> Timber.e("Throwable %s", throwable.getMessage()));
    }

    ValueEventListener gameIdListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            String gameId = dataSnapshot.getValue(String.class);
            if(gameId != null){
                Match match = new Match(String.valueOf(System.currentTimeMillis()),
                        mode, true);
                Game.newGame(match, null, null, null);
                startGame(match.id);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w("gameIdListener", "loadPost:onCancelled", databaseError.toException());
            // ...
        }
    };

    //todo need to change this shit))
    public static void sendMove(Coordinate old_pos, Coordinate new_pos, boolean ifOver) {
        Timber.d("Send move!");
        old_pos = new Coordinate(old_pos.x, old_pos.y, Board.rotations);
        new_pos = new Coordinate(new_pos.x, new_pos.y, Board.rotations);
        MoveMessage message = new MoveMessage(old_pos.x, old_pos.y, new_pos.x, new_pos.y, Game.myPlayerUserame, MoveMessageType.OK);
        if (ifOver)
            message.setType(MoveMessageType.OVER);
        Gson gson = new Gson();
        String json = gson.toJson(message);
        stompClient.send(new StompMessage(
                // Stomp command
                StompCommand.SEND,
                // Stomp Headers, Send Headers with STOMP
                // the first header is required, and the other can be customized by ourselves
                Arrays.asList(
                        new StompHeader(StompHeader.DESTINATION, Game.roomAdress),
                        new StompHeader("authorization", Game.myPlayerUserame)
                ),
                // Stomp payload
                json)
        ).subscribe();
    }

    public String getUsername() {
        SharedPreferences preferences = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        return preferences.getString("username", null);
    }

    public void startGame(final String matchID) {
        Timber.d("startGame");
        fragment = new GameFragment();
        Bundle b = new Bundle();
        b.putString("matchID", matchID);
        fragment.setArguments(b);
        replaceFragment(fragment);
    }

    class ppl{
        String username;

        public ppl(String username) {
            this.username = username;
        }

        public ppl() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}