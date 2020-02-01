package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.voxeet.android.media.MediaStream;
import com.voxeet.android.media.MediaStreamType;
import com.voxeet.promise.Promise;
import com.voxeet.promise.solve.ErrorPromise;
import com.voxeet.promise.solve.ThenPromise;
import com.voxeet.promise.solve.ThenValue;
import com.voxeet.promise.solve.ThenVoid;
import com.voxeet.sdk.VoxeetSdk;
import com.voxeet.sdk.events.v2.ParticipantAddedEvent;
import com.voxeet.sdk.events.v2.ParticipantUpdatedEvent;
import com.voxeet.sdk.events.v2.StreamAddedEvent;
import com.voxeet.sdk.events.v2.StreamRemovedEvent;
import com.voxeet.sdk.events.v2.StreamUpdatedEvent;
import com.voxeet.sdk.json.ParticipantInfo;
import com.voxeet.sdk.models.Conference;
import com.voxeet.sdk.models.Participant;
import com.voxeet.sdk.models.v1.CreateConferenceResult;
import com.voxeet.sdk.services.conference.information.ConferenceInformation;
import com.voxeet.sdk.views.VideoView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @NonNull
    protected List<View> views = new ArrayList<>();

    @NonNull
    protected List<View> buttonsNotLoggedIn = new ArrayList<>();

    @NonNull
    protected List<View> buttonsInConference = new ArrayList<>();

    @NonNull
    protected List<View> buttonsNotInConference = new ArrayList<>();

    @NonNull
    @Bind(R.id.conference_name)
    EditText conference_name;

    @Bind(R.id.user_name)
    EditText user_name;

    @Bind(R.id.video)
    protected VideoView video;

    @Bind(R.id.videoOther)
    protected VideoView videoOther;

    @Bind(R.id.participants)
    EditText participants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        throw new IllegalStateException("<---- Remove this line and set your keys below to use this sample !!");
        VoxeetSdk.initialize("", "");

        //adding the user_name, login and logout views related to the open/close and conference flow
        add(views, R.id.login);
        add(views, R.id.logout);

        add(buttonsNotLoggedIn, R.id.login);
        add(buttonsNotLoggedIn, R.id.user_name);

        add(buttonsInConference, R.id.logout);

        add(buttonsNotInConference, R.id.logout);

        add(views, R.id.join);

        add(buttonsNotInConference, R.id.join);

        add(views, R.id.leave);

        add(buttonsInConference, R.id.leave);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateViews();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 0x20);
        }

        VoxeetSdk.instance().register(this);
    }

    private MainActivity add(List<View> list, int id) {
        list.add(findViewById(id));
        return this;
    }

    private void updateViews() {
        //this method will be updated step by step
        //disable every views
        setEnabled(views, false);

        //if the user is not connected, we will only enabled the not logged
        if (!VoxeetSdk.session().isSocketOpen()) {
            setEnabled(buttonsNotLoggedIn, true);
            return;
        }

        ConferenceInformation current = VoxeetSdk.conference().getCurrentConference();
        //we can now add the logic to manage our basic state
        if (null != current && VoxeetSdk.conference().isLive()) {
            setEnabled(buttonsInConference, true);
        } else {
            setEnabled(buttonsNotInConference, true);
        }
    }

    private ErrorPromise error() {
        return error -> {
            Toast.makeText(MainActivity.this, "ERROR...", Toast.LENGTH_SHORT).show();
            error.printStackTrace();
            updateViews();
        };
    }

    private void setEnabled(@NonNull List<View> views, boolean enabled) {
        for (View view : views) view.setEnabled(enabled);
    }

    @OnClick(R.id.login)
    public void onLogin() {
        VoxeetSdk.session().open(new ParticipantInfo(user_name.getText().toString(), "", ""))
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "started...", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error(error());
    }

    @OnClick(R.id.logout)
    public void onLogout() {
        VoxeetSdk.session().close()
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "logout done", Toast.LENGTH_SHORT).show();
                    updateViews();
                }).error(error());
    }

    @OnClick(R.id.join)
    public void onJoin() {
        VoxeetSdk.conference().create(conference_name.getText().toString())
                .then((ThenPromise<CreateConferenceResult, Conference>) res -> VoxeetSdk.conference().join(res.conferenceId))
                .then(conference -> {
                    Toast.makeText(MainActivity.this, "started...", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error(error());
    }

    @OnClick(R.id.leave)
    public void onLeave() {
        VoxeetSdk.conference().leave()
                .then((result, solver) -> updateViews()).error(error());
    }

    @OnClick(R.id.startVideo)
    public void onStartVideo() {
        VoxeetSdk.conference().startVideo()
                .then((result, solver) -> updateViews())
                .error(error());
    }

    @OnClick(R.id.stopVideo)
    public void onStopVideo() {
        VoxeetSdk.conference().stopVideo()
                .then((result, solver) -> updateViews())
                .error(error());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamAddedEvent event) {
        updateStreams();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamUpdatedEvent event) {
        updateStreams();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamRemovedEvent event) {
        updateStreams();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ParticipantAddedEvent event) {
        updateUsers();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ParticipantUpdatedEvent event) {
        updateUsers();
    }

    private void updateStreams() {
        for (Participant user : VoxeetSdk.conference().getParticipants()) {
            boolean isLocal = user.getId().equals(VoxeetSdk.session().getParticipantId());
            MediaStream stream = user.streamsHandler().getFirst(MediaStreamType.Camera);

            VideoView video = isLocal ? this.video : this.videoOther;

            if (null != stream && !stream.videoTracks().isEmpty()) {
                video.setVisibility(View.VISIBLE);
                video.attach(user.getId(), stream);
            }
        }
    }

    public void updateUsers() {
        List<Participant> participants = VoxeetSdk.conference().getParticipants();
        List<String> names = new ArrayList<>();


        for (Participant participant : participants) {
            names.add(participant.getInfo().getName());
        }

        this.participants.setText(TextUtils.join(",", names));
    }
}
