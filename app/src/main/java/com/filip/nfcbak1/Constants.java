package com.filip.nfcbak1;

/**
 * Autor: Filip Dian
 *
 * Konstanty.
 */
public interface Constants {

    //mody HCE sluzby
    int NOT_LOGGED_IN_STATE = 1;
    int NOT_REGISTERED_STATE = 2;
    int REGISTERED_STATE = 3;

    //pre testovacie ucely je pouzita platnost 30 sekund
    long VALID_LOGIN_TIME = 30000; //24 hodin = 86 400 000

    //akcie intentov pre spustenie aktivity/sluzby
    String STARTED_FROM_ACTIVITY = "STARTED_FROM_ACTIVITY";
    String RESTARTED_FROM_BROADCAST = "RESTARTED_FROM_BROADCAST";
    String NOTIFICATION_START = "NOTIFICATION_START";
    String LOGIN_TIMEOUT_START = "LOGIN_TIMEOUT_START";

    //akcie intentov odosielanymi HCE sluzbou
    String REGISTRATION_SUCCESFUL = "REGISTRATION_SUCESSFUL";
    String AUTHENTICATION_SUCCESFUL = "AUTHENTICATION_SUCCESFUL";

}
