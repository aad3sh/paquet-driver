package com.example.paquetdriver;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Interpolator;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    public static boolean scrolledUp = false;
    TextView brand_name, tagline, first_name, last_name;
    EditText email_register, password_register, email_login, password_login;
    ScrollView scroll_view;
    ImageView logo;

    Button register_button, login_button;

    private FirebaseAuth firebaseAuth;
    GoogleApiClient mGoogleApiClient;
    SignInButton signInButton;

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        brand_name = (TextView) findViewById(R.id.brand_name);
        tagline = (TextView) findViewById(R.id.tagline);
        logo = (ImageView) findViewById(R.id.logo);
        first_name = (TextView) findViewById(R.id.first_name);
        last_name = (TextView) findViewById(R.id.last_name);
        email_register = (EditText) findViewById(R.id.email_register);
        password_register = (EditText) findViewById(R.id.password_register);
        email_login = (EditText) findViewById(R.id.email_login);
        password_login = (EditText) findViewById(R.id.password_login);
        register_button = (Button) findViewById(R.id.register_button);
        login_button = (Button) findViewById(R.id.login_button);

        scroll_view = (ScrollView) findViewById(R.id.scroll_view);

        InitGoogleSignIn();
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.googlesignin_button:
                GoogleSignIn();
                break;
        }
    }

    private void InitGoogleSignIn(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signInButton = (SignInButton) findViewById(R.id.googlesignin_button);
        signInButton.setOnClickListener(this);

    }

    public void GoogleSignIn(){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result){
        Log.d(TAG, "handleSignInResult: " + result.isSuccess());
        if(result.isSuccess()){
            GoogleSignInAccount acct = result.getSignInAccount();

            //firebaseAuth.signInWithCustomToken(acct.getIdToken());
            Intent i = new Intent(this, Main2Activity.class);
            startActivity(i);
            Toast.makeText(this, acct.getEmail(), Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "failed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){
        Log.d(TAG, "onConnectonFailed: " + connectionResult);
    }

    public void login(View view){
        final Intent i = new Intent(this, Main2Activity.class);

        if(!scrolledUp){
            firebaseAuth.signInWithEmailAndPassword(email_login.getText().toString(), password_login.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        if (firebaseAuth.getCurrentUser().isEmailVerified()) {
                            //startActivity(new Intent(getApplicationContext(), Main2Activity.class));
                            Thread thread = new Thread(){
                                public void run(){
                                    try{
                                        sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    finally{
                                        startActivity(i);
                                        finish();
                                    }
                                }
                            };
                            thread.start();
                        }else{
                            Toast.makeText(LoginActivity.this, "Please verify your E-Mail Address", Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }else {
            ScrollDown();
            HideRegisterFields();
        }

    }

    private void ScrollUp(){
        final ConstraintLayout cl = (ConstraintLayout) findViewById(R.id.constraintLayout);
        final int newBottomMargin = 1000;
        Animation a = new Animation() {

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cl.getLayoutParams();
                //Toast.makeText(getApplication(), "This is my Toast message!" + params.bottomMargin,Toast.LENGTH_LONG).show();
                params.bottomMargin= (int)(newBottomMargin+(newBottomMargin*interpolatedTime));
                cl.setLayoutParams((ViewGroup.MarginLayoutParams)params);
            }
        };

        a.setDuration(500); // in ms
        cl.startAnimation(a);

        brand_name.setVisibility(View.GONE);
        tagline.setVisibility(View.GONE);
        logo.setVisibility(View.GONE);

        scrolledUp = true;
    }

    private void ScrollDown() {
        final ConstraintLayout cl = (ConstraintLayout) findViewById(R.id.constraintLayout);
        final int newBottomMargin = 1000;
        Animation a = new Animation() {

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cl.getLayoutParams();
                //Toast.makeText(getApplication(), "This is my Toast message!" + params.bottomMargin,Toast.LENGTH_LONG).show();
                params.bottomMargin= (int)(newBottomMargin+(newBottomMargin*interpolatedTime));
                cl.setLayoutParams((ViewGroup.MarginLayoutParams)params);
            }
        };
        a.setInterpolator(new ReverseInterpolator());
        //a.setRepeatMode(Animation.REVERSE);
        a.setDuration(500); // in ms
        cl.startAnimation(a);

        brand_name.setVisibility(View.VISIBLE);
        tagline.setVisibility(View.VISIBLE);
        logo.setVisibility(View.VISIBLE);

        scrolledUp = false;
    }

    public void ShowRegisterFields(){
        scroll_view.setVisibility(View.INVISIBLE);

        first_name.setVisibility(View.VISIBLE);
        last_name.setVisibility(View.VISIBLE);
        email_register.setVisibility(View.VISIBLE);
        password_register.setVisibility(View.VISIBLE);
    }

    public void HideRegisterFields(){
        first_name.setVisibility(View.GONE);
        last_name.setVisibility(View.GONE);
        email_register.setVisibility(View.INVISIBLE);
        password_register.setVisibility(View.INVISIBLE);

        scroll_view.setVisibility(View.VISIBLE);
    }

    public void register(View view){
        if(!scrolledUp){
            ScrollUp();
            ShowRegisterFields();

        }else{
            firebaseAuth.createUserWithEmailAndPassword(email_register.getText().toString(), password_register.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        firebaseAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(LoginActivity.this, "Registered Successfully, Please check your E-Mail for verification", Toast.LENGTH_LONG).show();
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(first_name.getText().toString() + " " + last_name.getText().toString()).build();

                                    firebaseAuth.getCurrentUser().updateProfile(profileUpdates);
                                }else{
                                    Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        Toast.makeText(LoginActivity.this, "Registered Successfully", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            //ScrollDown();
            //HideRegisterFields();
        }
    }
}

class ReverseInterpolator implements Interpolator {
    @Override
    public float getInterpolation(float paramFloat) {
        return Math.abs(paramFloat -1f);
    }
}