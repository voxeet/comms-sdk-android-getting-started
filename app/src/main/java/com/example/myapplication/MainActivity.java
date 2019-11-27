package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.voxeet.android.media.MediaStream;
import com.voxeet.android.media.MediaStreamType;
import com.voxeet.sdk.VoxeetSdk;
import com.voxeet.sdk.events.v2.StreamAddedEvent;
import com.voxeet.sdk.events.v2.StreamRemovedEvent;
import com.voxeet.sdk.events.v2.StreamUpdatedEvent;
import com.voxeet.sdk.events.v2.UserAddedEvent;
import com.voxeet.sdk.events.v2.UserUpdatedEvent;
import com.voxeet.sdk.json.UserInfo;
import com.voxeet.sdk.models.Conference;
import com.voxeet.sdk.models.User;
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
import eu.codlab.simplepromise.solve.ErrorPromise;
import eu.codlab.simplepromise.solve.PromiseExec;
import eu.codlab.simplepromise.solve.Solver;

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
        return new ErrorPromise() {
            @Override
            public void onError(@NonNull Throwable error) {
                Toast.makeText(MainActivity.this, "ERROR...", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
                updateViews();
            }
        };
    }

    private void setEnabled(@NonNull List<View> views, boolean enabled) {
        for (View view : views) view.setEnabled(enabled);
    }

    @OnClick(R.id.login)
    public void onLogin() {
        VoxeetSdk.session().open(new UserInfo(user_name.getText().toString(), "", ""))
                .then(new PromiseExec<Boolean, Object>() {
                    @Override
                    public void onCall(@Nullable Boolean result, @NonNull Solver<Object> solver) {
                        Toast.makeText(MainActivity.this, "started...", Toast.LENGTH_SHORT).show();
                        updateViews();
                    }
                })
                .error(error());
    }

    @OnClick(R.id.logout)
    public void onLogout() {
        VoxeetSdk.session().close()
                .then(new PromiseExec<Boolean, Object>() {
                    @Override
                    public void onCall(@Nullable Boolean result, @NonNull Solver<Object> solver) {
                        Toast.makeText(MainActivity.this, "logout done", Toast.LENGTH_SHORT).show();
                        updateViews();
                    }
                }).error(error());
    }

    @OnClick(R.id.join)
    public void onJoin() {
        VoxeetSdk.conference().create(conference_name.getText().toString())
                .then(new PromiseExec<CreateConferenceResult, Conference>() {
                    @Override
                    public void onCall(@Nullable CreateConferenceResult result, @NonNull Solver<Conference> solver) {
                        if (null != result)
                            solver.resolve(VoxeetSdk.conference().join(result.conferenceId));
                        else Toast.makeText(MainActivity.this, "Ooops", Toast.LENGTH_SHORT).show();
                    }
                })
                .then(new PromiseExec<Conference, Object>() {
                    @Override
                    public void onCall(@Nullable Conference result, @NonNull Solver<Object> solver) {
                        Toast.makeText(MainActivity.this, "started...", Toast.LENGTH_SHORT).show();
                        updateViews();
                    }
                })
                .error(error());
    }

    @OnClick(R.id.leave)
    public void onLeave() {
        VoxeetSdk.conference().leave()
                .then(new PromiseExec<Boolean, Object>() {
                    @Override
                    public void onCall(@Nullable Boolean result, @NonNull Solver<Object> solver) {
                        updateViews();
                    }
                }).error(error());
    }

    @OnClick(R.id.startVideo)
    public void onStartVideo() {
        VoxeetSdk.conference().startVideo()
                .then(new PromiseExec<Boolean, Object>() {
                    @Override
                    public void onCall(Boolean result, Solver<Object> solver) {
                        updateViews();
                    }
                })
                .error(error());
    }

    @OnClick(R.id.stopVideo)
    public void onStopVideo() {
        VoxeetSdk.conference().stopVideo()
                .then(new PromiseExec<Boolean, Object>() {
                    @Override
                    public void onCall(Boolean result, Solver<Object> solver) {
                        updateViews();
                    }
                })
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
    public void onEvent(UserAddedEvent event) {
        updateUsers();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(UserUpdatedEvent event) {
        updateUsers();
    }

    private void updateStreams() {
        for (User user : VoxeetSdk.conference().getUsers()) {
            boolean isLocal = user.getId().equals(VoxeetSdk.session().getUserId());
            MediaStream stream = user.streamsHandler().getFirst(MediaStreamType.Camera);

            VideoView video = isLocal ? this.video : this.videoOther;

            if (null != stream && !stream.videoTracks().isEmpty()) {
                video.setVisibility(View.VISIBLE);
                video.attach(user.getId(), stream);
            }
        }
    }

    public void updateUsers() {
        List<User> participants = VoxeetSdk.conference().getUsers();
        List<String> names = new ArrayList<>();


        for (User participant : participants) {
            names.add(participant.getUserInfo().getName());
        }

        this.participants.setText(TextUtils.join(",", names));
    }
}
