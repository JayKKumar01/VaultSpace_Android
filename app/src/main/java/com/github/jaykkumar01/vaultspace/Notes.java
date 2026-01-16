package com.github.jaykkumar01.vaultspace;

public class Notes {
    // animation on album click so that activity transition looks like opening actual album
    // blocking overlay > loading state view >
    // fix the edge case when access is revoked manually
    // don't let user loose the session because of internet
    // fix the touch back, and all, and so many thing, on backpress why it is showing again and again, maybe handle the event also same as the state
    // backpress should remove, on touch, on cancel does not work for now, on touch listener should be added.. add everthing one by one
    // some bugs are there for the backpress and the outside touch, clickable true to false
    // how onDismissedData is going to help me
    //dashboard > cancel setup or wait, while retrying

    // use the retry on the same upload status view after failed upload, so normalize the cancel button

    // notification: uploads completed || uploads failed, check albums for more info
    // add a warning badge in the albums, move them to top. user can check and retry
    // on retry... there should be two buttons one for cancel also
    // show failed media also
    // fix the backpress album
    //think of a design how will you place retry and cancel both

    // still there is a glitch, on entering for the first time it will return false 2nd time it works, check the modal what is wrong
    // add the counts only one entry per album
    //after logout service should stop

    //2 issue: hashmap , ondestroy not called by user dismiss
    // a seperate ui for upload

    // album activity null observer issue

    // two things> stop on dismissed, stop on exit if no uploads is in progress

    // remove the progress for the completed albums... let others render

    // use two colors in the progess bar to show successful and failed upload

    // ondestory inform orchestrator > remove the handler that will call stop self if it gets start command meanwhile


    // on network connection drop in dashboard it does not reconnect sometimes, fix that
}
