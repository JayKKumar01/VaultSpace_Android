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

    // something is wrong with the retry logic, for the non retriables
    // lots of room for optimization in status view and the controller
    // conflict a bit, between the retry and status view rendering, logic is right just calculation for rendering is not good


    // without touching the upload manager and all, fix the controller and status view
    // too many renders are causing logic issue

    // add the no access bar also in the progress bar

    //use the reason retriable in retry also... so that won't have to check the uriutil again

    //update storage upload the uploads,not each but each selections

    // fix race conditions, wait for the trusted accounts to be fetched first



 // use parallel executor wherever possible

    // last render should always show.... multisegment progressbar issue on high ups

    // for recycler view, set max width .45 for smaller let them wrap content

    // started upload, exited, comes back, I loose the uploaded onces why, fix that


    // keep the album activity waiting until the storage view is loaded I mean the trusted accounts

    // save the things in usersession that does not change like the root folder id, save once on the first creation, not necessarily on login

    // trusted accounts might not be ready to be added, till the existing trusted accounts are loaded

    //✅ 2️⃣ Cache successful storage info for session (MEDIUM VALUE)
    //
    //Once an account succeeds:
    //
    //Cache it in memory
    //
    //Don’t refetch quota every time
    //
    //This avoids repeated flaky calls.
    //
    //You already decided:
    //
    //Drive permissions are source of truth
    //So caching quota within session is safe.


    // cache the storage quota while uploading for each uploaditem on success, or simply minus that much
    //in the final but calculations does not take time so simply cache individuals

    // save the thumbnail or just upload the thumbnails directly to drive and get it via cache for video

    // video thumbs are big
    

    //Drive API rule (VaultSpace):
    //When calling Drive APIs in loops or across accounts, always use parallel execution with workers = min(tasks, cpu - 1).
    // Create one Drive client per task, aggregate results using thread-safe structures, and apply one bounded retry with a small delay (~250ms) on failure.
    // Fail soft per task; never block others or retry aggressively.



    // before adding https://www.googleapis.com/auth/drive, make sure you delete the files first from owner's id

    //update the count of storage on success

    // later need to handle the storage cache update, efficiently
    // need to optimize upload speed in uploaddrivehelper

    // get the live update for storage while upload

    // make the uplaods parrallel

    // vault space folder issue

    // two folders are getting created, fix that issue

    //fix trusted account cache, it should never be accessible from outside TrustedRepo

    // before publishing make sure you clear everything on session

    // on session clear make sure to clear the store

    //crosscheck all the failed test cases with fake upload

    // it supports resume so make sure to update the progress based on that on retry

    // and also do something with overall progress view

    // add a max height to the stack progress view, there are glitches when you select 130 files at once, something goes wrong
    //all tasks gets submitted at once so fix that, and yeah on retry total size it not shown so fix that too
    //in fact we don't get any uploaded bytes progress also
    // so basically on resume you need to fix the progress bar differently









}
