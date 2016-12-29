package com.example.avinoam.boazmapot;

import android.location.Location;

import com.firebase.client.Firebase;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by avinoam on 27/12/16.
 */

public class Config {

    public static final String FIREBASE_URL = "https://mivne2-b2d4f.firebaseio.com/";
    private static FirebaseUser user;

    public static FirebaseUser getUser(){
        return user;
    }

    public static void setUser(FirebaseUser fbUser){
        user = fbUser;
    }

    public static Location myLoc;
    public static Location2D bleLoc;
    public static boolean isBLeLoc = false;


}
