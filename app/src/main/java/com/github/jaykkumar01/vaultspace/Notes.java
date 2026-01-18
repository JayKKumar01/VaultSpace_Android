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

    // on netwrok problem need to fix the failed retry adding.
    //should not remove the cache till session, on retry just enqueu the retries once user clicks ok then only remove the cache or sometime later auto\
    // on restart check if upload cache is there otherwise show only the retries
    // need to do something on just hide (dismissed from right top corner)
    // handle indivisual cancel notification

    // debug with just one album

    //don't clear cache on in the session... to track previous uploads, until user cancels it or hides it using that popup

    // indivisual cancells are working but the finalize notification is skipped, there is no way of informing the notification in the middle
    // debug for one album

    // on logout handle everything... no notification needed just immediate stop it
    // check the uri if accesible then only include
    //add retry to all retries

    // extra reties are getting added
    //add one more red warning for permanently failed uploads, show the details button if all failed are permanent

    // get a permanent solution of how will you upload properly... maybe take the permission in foreground and run the upload also on foreground always

    // app can be stopped in the middle somehow so store all the info in the start only, on success just mark it done... or else mark pending... or keep it marked pending on finalized

    // retries list is there already then picked... enqueue new selections right
    // finished with success not working

    // run edge cases with 2 albums.... for notification.. if snapshot is not cleared... then even if it is not running
    // it gets prioriry in notification... becasue it was added earlier... completed... moved down,
    // 2nd album started... moves up... but now when
    // 1st albums get queue in again.. it gets prioriry back becasue it was still in snapshot. fix this

    //handle cancelled for stores








}
