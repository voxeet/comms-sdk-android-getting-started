package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.voxeet.VoxeetSDK;
import com.voxeet.android.media.MediaStream;
import com.voxeet.android.media.stream.MediaStreamType;
import com.voxeet.promise.solve.ErrorPromise;
import com.voxeet.promise.solve.ThenPromise;
import com.voxeet.sdk.events.v2.ParticipantAddedEvent;
import com.voxeet.sdk.events.v2.ParticipantUpdatedEvent;
import com.voxeet.sdk.events.v2.StreamAddedEvent;
import com.voxeet.sdk.events.v2.StreamRemovedEvent;
import com.voxeet.sdk.events.v2.StreamUpdatedEvent;
import com.voxeet.sdk.events.promises.ServerErrorException;
import com.voxeet.sdk.json.RecordingStatusUpdatedEvent;
import com.voxeet.sdk.json.ParticipantInfo;
import com.voxeet.sdk.json.internal.ParamsHolder;
import com.voxeet.sdk.models.Conference;
import com.voxeet.sdk.models.Participant;
import com.voxeet.sdk.services.builders.ConferenceCreateOptions;
import com.voxeet.sdk.services.builders.ConferenceJoinOptions;
import com.voxeet.sdk.services.conference.information.ConferenceInformation;
import com.voxeet.sdk.services.screenshare.RequestScreenSharePermissionEvent;
import com.voxeet.sdk.services.screenshare.ScreenCapturerService;
import com.voxeet.sdk.views.VideoView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    protected List<View> buttonsInOwnVideo = new ArrayList<>();

    @NonNull
    protected List<View> buttonsNotInOwnVideo = new ArrayList<>();

    @NonNull
    protected List<View> buttonsInOwnScreenShare = new ArrayList<>();

    @NonNull
    protected List<View> buttonsNotInOwnScreenShare = new ArrayList<>();

    @Bind(R.id.user_name)
    EditText user_name;

    @Bind(R.id.conference_name)
    EditText conference_name;

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

        // Initialize the Voxeet SDK
        // WARNING: It is best practice to use the VoxeetSDK.initialize function with an Access Token to initialize the SDK.
        // Please read the documentation at:
        // https://docs.dolby.io/interactivity/docs/initializing
        throw new IllegalStateException("<---- Remove this line and set your keys below to use this sample !!");
        VoxeetSDK.initialize("", "");

        //adding the user_name, login and logout views related to the open/close and conference flow
        add(views, R.id.login);
        add(views, R.id.logout);

        add(buttonsNotLoggedIn, R.id.login);
        add(buttonsNotLoggedIn, R.id.user_name);

        add(buttonsInConference, R.id.logout);

        add(buttonsNotInConference, R.id.logout);

        String[] avengersNames = {
                "Thor",
                "Cap",
                "Tony Stark",
                "Black Panther",
                "Black Widow",
                "Hulk",
                "Spider-Man",
        };
        Random r = new Random();
        user_name.setText(avengersNames[r.nextInt(avengersNames.length)]);

        // Add the join button and enable it only when not in a conference
        add(views, R.id.join);
        add(buttonsNotInConference, R.id.join);

        // Set a default conference name
        conference_name.setText("Avengers meeting");

        // Add the leave button and enable it only while in a conference
        add(views, R.id.leave);
        add(buttonsInConference, R.id.leave);

        //adding the startVideo in the flow
        add(views, R.id.startVideo);
        add(buttonsInConference, R.id.startVideo);
        add(buttonsNotInOwnVideo, R.id.startVideo);

        //adding the stopVideo in the flow
        add(views, R.id.stopVideo);
        add(buttonsInConference, R.id.stopVideo);
        add(buttonsInOwnVideo, R.id.stopVideo);

        //adding the startScreenShare in the flow
        add(views, R.id.startScreenShare);
        add(buttonsInConference, R.id.startScreenShare);
        add(buttonsNotInOwnScreenShare, R.id.startScreenShare);

        //adding the stopScreenShare in the flow
        add(views, R.id.stopScreenShare);
        add(buttonsInConference, R.id.stopScreenShare);
        add(buttonsInOwnScreenShare, R.id.stopScreenShare);

        //adding the start recording in the flow
        add(views, R.id.start_recording);
        add(buttonsInConference, R.id.start_recording);

        //adding the stop recording in the flow
        add(views, R.id.stop_recording);
        add(buttonsInConference, R.id.stop_recording);
    }

    private MainActivity add(List<View> list, int id) {
        list.add(findViewById(id));
        return this;
    }

    @Override
    protected void onPause() {
        //register the current activity in the SDK
        VoxeetSDK.instance().unregister(this);

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ScreenCapturerService.register(this);

        updateViews();

        //register the current activity in the SDK
        VoxeetSDK.instance().register(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 0x20);
        }

        VoxeetSDK.screenShare().consumeRightsToScreenShare();
    }

    @Override
    protected void onDestroy() {
        ScreenCapturerService.unregisterActivity();

        super.onDestroy();
    }

    private void updateViews() {
        //this method will be updated step by step
        //disable every views
        setEnabled(views, false);

        //if the user is not connected, we will only enabled the not logged
        if (!VoxeetSDK.session().isSocketOpen()) {
            setEnabled(buttonsNotLoggedIn, true);
            return;
        }

        ConferenceInformation current = VoxeetSDK.conference().getCurrentConference();
        //we can now add the logic to manage our basic state
        if (null != current && VoxeetSDK.conference().isLive()) {
            setEnabled(buttonsInConference, true);
        } else {
            setEnabled(buttonsNotInConference, true);
        }
        if (null != current) {
            if (current.isOwnVideoStarted()) {
                setEnabled(buttonsInOwnVideo, true);
                setEnabled(buttonsNotInOwnVideo, false);
            } else {
                setEnabled(buttonsInOwnVideo, false);
                setEnabled(buttonsNotInOwnVideo, true);
            }
        }
        if (null != current) {
            if (current.isScreenShareOn()) {
                setEnabled(buttonsInOwnScreenShare, true);
                setEnabled(buttonsNotInOwnScreenShare, false);
            } else {
                setEnabled(buttonsInOwnScreenShare, false);
                setEnabled(buttonsNotInOwnScreenShare, true);
            }
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
        VoxeetSDK.session().open(new ParticipantInfo(user_name.getText().toString(), "", ""))
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "log in successful", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error(error());
    }

    @OnClick(R.id.logout)
    public void onLogout() {
        VoxeetSDK.session().close()
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "logout done", Toast.LENGTH_SHORT).show();
                    updateViews();
                }).error(error());
    }

    @OnClick(R.id.join)
    public void onJoin() {
        ParamsHolder paramsHolder = new ParamsHolder();
        paramsHolder.setDolbyVoice(true);

        ConferenceCreateOptions conferenceCreateOptions = new ConferenceCreateOptions.Builder()
                .setConferenceAlias(conference_name.getText().toString())
                .setParamsHolder(paramsHolder)
                .build();

        VoxeetSDK.conference().create(conferenceCreateOptions)
                .then((ThenPromise<Conference, Conference>) conference -> {
                    ConferenceJoinOptions conferenceJoinOptions = new ConferenceJoinOptions.Builder(conference)
                            .build();

                    return VoxeetSDK.conference().join(conferenceJoinOptions);
                })
                .then(conference -> {
                    Toast.makeText(MainActivity.this, "started...", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error((error_in) -> {
                    Toast.makeText(MainActivity.this, "Could not create conference", Toast.LENGTH_SHORT).show();
                });
    }

    @OnClick(R.id.leave)
    public void onLeave() {
        VoxeetSDK.conference().leave()
                .then((result, solver) -> {
                    updateViews();
                    Toast.makeText(MainActivity.this, "left...", Toast.LENGTH_SHORT).show();
                }).error(error());
    }

    @OnClick(R.id.startVideo)
    public void onStartVideo() {
        VoxeetSDK.conference().startVideo()
                .then((result, solver) -> updateViews())
                .error(error());
    }

    @OnClick(R.id.stopVideo)
    public void onStopVideo() {
        VoxeetSDK.conference().stopVideo()
                .then((result, solver) -> updateViews())
                .error(error());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamAddedEvent event) {
        updateStreams();
        updateViews();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamUpdatedEvent event) {
        updateStreams();
        updateViews();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StreamRemovedEvent event) {
        updateStreams();
        updateViews();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ParticipantAddedEvent event) {
        updateParticipants();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ParticipantUpdatedEvent event) {
        updateParticipants();
    }

    private void updateStreams() {
        for (Participant user : VoxeetSDK.conference().getParticipants()) {
            boolean isLocal = user.getId().equals(VoxeetSDK.session().getParticipantId());
            MediaStream stream = user.streamsHandler().getFirst(MediaStreamType.Camera);

            VideoView video = isLocal ? this.video : this.videoOther;

            if (null != stream && !stream.videoTracks().isEmpty()) {
                video.setVisibility(View.VISIBLE);
                video.attach(user.getId(), stream);
            }
        }

        // Screen shares take precedence over videos
        for (Participant user : VoxeetSDK.conference().getParticipants()) {
            boolean isLocal = user.getId().equals(VoxeetSDK.session().getParticipantId());
            MediaStream stream = user.streamsHandler().getFirst(MediaStreamType.ScreenShare);

            VideoView video = isLocal ? this.video : this.videoOther;

            if (null != stream && !stream.videoTracks().isEmpty()) {
                video.setVisibility(View.VISIBLE);
                video.attach(user.getId(), stream);
            }
        }
    }

    public void updateParticipants() {
        List<Participant> participantsList = VoxeetSDK.conference().getParticipants();
        List<String> names = new ArrayList<>();

        for (Participant participant : participantsList) {
            if (participant.streams().size() > 0)
                names.add(participant.getInfo().getName());
        }

        participants.setText(TextUtils.join(", ", names));
    }
    @OnClick(R.id.startScreenShare)
    public void onStartScreenShare() {
        VoxeetSDK.screenShare().sendRequestStartScreenShare();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RequestScreenSharePermissionEvent event) {
        VoxeetSDK.screenShare().sendUserPermissionRequest(this);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean managed = false;

        if (null != VoxeetSDK.screenShare()) {
            managed = VoxeetSDK.screenShare().onActivityResult(requestCode, resultCode, data);
        }

        if (!managed) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        updateViews();
    }

    @OnClick(R.id.stopScreenShare)
    public void onStopScreenShare() {
        VoxeetSDK.screenShare().stopScreenShare().then((result, solver) -> {
            //screenshare has been stopped locally and remotely
            updateViews();
        }).error(error -> {
            //screenshare has been stopped locally but a network error occured
        });
    }

    @OnClick(R.id.start_recording)
    public void onStartRecording() {
        VoxeetSDK.recording().start()
                .then((result, solver) -> {
                })
                .error((error_in) -> {
                    String error_message = "Error";
                    if (((ServerErrorException)error_in).error.error_code == 303) {
                        error_message = "Recording already started";
                    }
                    updateViews();
                    Toast.makeText(MainActivity.this, error_message, Toast.LENGTH_SHORT).show();
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RecordingStatusUpdatedEvent event) {
        String message = null;
        switch (event.recordingStatus) {
            case "RECORDING": message = "Recording started"; break;
            case "NOT_RECORDING": message = "Recording stopped"; break;
            default: break;
        }
        if (null != message)
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.stop_recording)
    public void onStopRecording() {
        VoxeetSDK.recording().stop()
                .then((result, solver) -> {
                })
                .error(error());
    }
}
