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


    //if there is any error to any file it needs to get removed,
    // resume case is very messy fix it



    // make only items progress overflow, keep the overall progress as a solid view

    //on cancel hide the item progress, actually clear it
    //actually hide both the view

    // once cancelled, notificaiton always shows some items were not uploaded, fix that

    // something is wrong in delete, loosing storage somehow

    // listing the media in album fails, not correct, some counts are missing

    // maybe that's why deleting is a problem


    // resume almost fix, just getting some duplicates, need to think why

    // get the email from retry but, validate eligibility if not then simply restart for that upload
    //using another email

    //maybe only direct uploads are happening like this, just check the chunk size while taking email from store
    // there is no resuming it is getting simply restart, debug the email whether it is taking the email correctly
    //or not


    // it throws error instantly on network change fix it (upload drive)





    // on every install ask users to add the email account to the app once

    // on login ask use to give consent to all the accounts they have already added as trusted users,
    // then only move ahead


    // for each trusted accounts
    //ACCESSIBLE
    //NOT_ON_DEVICE
    //CONSENT_REQUIRED

    //fix the sweep, if open album once it will start sweeping again so fix that

    // make sure only the owner of the folder will be considered as primary not the writers

    // there is a continious loop in the add trusted method, fix that
    // folder creation is messed! fix that too

    // drive does not allow creating folder with same name in the root so
    // smartly fix that maybe add the name inside the root


    //fix the continuous loop

    //once the setup is done we will handle the back pressed with modal

    // I can still keep the LIMITED, will have to decide the state wheather it should go to oauth or
    // limited from the resolve itself



    // they need to know what is being uplaoded so add the thumnail in the progress bar, keeping it tight
    // on failed individual change the bar color or remove from list

    //on network change it throws error intantly, give retry window of 5 seconds


    // upload does not update the album ui, but the counts gets update

    //same optimization needed for the stack view also, bind and update

    //while retrying you messed the upload id

    // change the stack to recylerview


    // sometimes cancel button does not work, problem on cancel thread for downloading, use flag to avoid late callbacks



    // upload glitch,
    // album content view
    //thumbnail size in util


    //once cancelled you won't get update it means you need to get update from upload manager directly,
    // do not break the generation logic, fix the callback things from ui side snapshot side


    // album creation glitch, if VaultSpace is created then next time fetch is failing




    // from external sd card it might give uri permission error once activity changed

    // group days for past, make sure each band has at least two after a certain past



    //come to bug later, first optimize pairing engine
    
    
    // 500 mb reserve for dumb users
    //
    // cache so that per upload no need of o (N)

    // per file limit.. allow more then 15 gb basically no limit


    // just backtrack one more item

    // divide in column for the upload progress


    // description and pseudo code
    //Perfect. I’ve cross-checked **the current class line-by-line** against the last description/pseudo-code.
    //Below is the **corrected, exact description and pseudo-code**, matching the class **1:1**, with **only real behaviors that exist in code** (nothing imagined, nothing missing).
    //
    //---
    //
    //# ✅ CORRECTED DESCRIPTION + PSEUDO CODE (1:1 with current class)
    //
    //---
    //
    //## **setMedia**
    //
    //### Description (exact behavior)
    //
    //* Clears all existing state
    //* If input list is null or empty:
    //
    //  * returns an empty layout list
    //* Copies all media
    //* Creates a time bucketizer
    //* Groups media by time bucket key
    //* Groups are **not ordered while grouping**
    //* Groups are then **sorted by time priority**
    //* For each group, in order:
    //
    //  * pairs media
    //  * creates band layouts
    //  * **sets time label only on the first band layout**
    //  * assigns layout start and count
    //  * appends layouts to the flat list
    //* Returns the full layout list
    //
    //### Pseudo-code
    //
    //```
    //clear all state
    //
    //if media list is null or empty
    //    return empty layout list
    //
    //copy media list
    //create time bucketizer
    //
    //for each media item
    //    find its time key
    //    add media to its group
    //
    //sort groups by time priority
    //
    //layoutCursor = 0
    //for each group in order
    //    set group.layoutStart = layoutCursor
    //    pair media in group
    //    create band layouts
    //    show label only on first layout
    //    set group.layoutCount
    //    append layouts to flat list
    //    layoutCursor += group.layoutCount
    //
    //return full layout list
    //```
    //
    //---
    //
    //## **addMedia**
    //
    //### Description (exact behavior)
    //
    //* If media is null:
    //
    //  * returns no-op replace range
    //* Inserts media into global media list (sorted by time)
    //* Finds the time group
    //
    //**If group does not exist**
    //
    //* Creates a new group
    //* Adds media to it
    //* Inserts group at correct time priority position
    //* Computes its layout start
    //* Builds layouts for the group
    //* **Sets label only on first layout**
    //* Inserts layouts into flat list
    //* Shifts layoutStart of following groups
    //* Returns inserted range
    //
    //**If group exists**
    //
    //* Inserts media into group (sorted)
    //* Rebuilds layouts for that group
    //* **Label is reassigned so only first layout shows it**
    //* Diffs old vs new layouts
    //* If no prefix or suffix matches:
    //
    //  * removes entire old group layout
    //  * inserts full new layout
    //* Otherwise:
    //
    //  * removes only changed middle
    //  * inserts only changed middle
    //* Updates group layout count
    //* Shifts following groups
    //* Returns changed range
    //
    //### Pseudo-code
    //
    //```
    //if media is null
    //    return no change
    //
    //insert media into global list
    //
    //find time group
    //
    //if group does not exist
    //    create group
    //    add media to group
    //    insert group by time priority
    //    calculate group layoutStart
    //    pair media
    //    create band layouts
    //    show label only on first layout
    //    insert layouts into flat list
    //    shift following groups
    //    return inserted range
    //
    //else
    //    insert media into group
    //    pair media
    //    create new layouts
    //    show label only on first layout
    //    diff old layouts vs new layouts
    //
    //    if no prefix and no suffix matches
    //        replace entire group layout
    //    else
    //        replace only changed middle part
    //
    //    update group layout count
    //    shift following groups
    //    return changed range
    //```
    //
    //---
    //
    //## **removeMedia**
    //
    //### Description (exact behavior)
    //
    //* If media is null:
    //
    //  * returns no-op replace range
    //* Finds the time group
    //* If group not found or media not in group:
    //
    //  * returns no-op
    //* Removes media from group and global list
    //
    //**If group becomes empty**
    //
    //* Captures group layout start and count
    //* Removes group from group lists
    //* Removes all its layouts from flat list
    //* Shifts following groups
    //* Returns removed range
    //
    //**If group still has media**
    //
    //* Rebuilds layouts for group
    //* **Label moves to new first layout**
    //* Diffs old vs new layouts
    //* Either full replace or partial replace
    //* Updates group layout count
    //* Shifts following groups
    //* Returns changed range
    //
    //### Pseudo-code
    //
    //```
    //if media is null
    //    return no change
    //
    //find time group
    //
    //if group not found or media not in group
    //    return no change
    //
    //remove media from group
    //remove media from global list
    //
    //if group is empty
    //    capture group layoutStart and layoutCount
    //    remove group
    //    remove all its layouts
    //    shift following groups
    //    return removed range
    //
    //else
    //    pair remaining media
    //    create new layouts
    //    show label only on first layout
    //    diff old layouts vs new layouts
    //
    //    if no prefix and no suffix matches
    //        replace entire group layout
    //    else
    //        replace only changed middle part
    //
    //    update group layout count
    //    shift following groups
    //    return changed range
    //```
    //
    //---
    //
    //## ✅ Final confirmation
    //
    //This description and pseudo-code now correctly covers:
    //
    //* time priority ordering
    //* sorted insertion of media
    //* group insertion/removal
    //* label-only-on-first-layout rule
    //* diff fallback vs partial replace
    //* shifting of following groups
    //* exact no-op cases
    //
    //Nothing extra. Nothing missing.
    //When you’re ready, next step is cleanly extracting the **helper** using this as the contract.




    //add a delay to notify

    // I need to cancel the load on view hide




    // add the add media button in album activity




    // if player does not start soon, fallback to download using input stream

    //fix the recylerview for some glitched thumbnails album media

    // invalidate caches memory caches on starting dashboard


    // based on codec + rotation do that








    //  Today is not sorted properly also add level Time:


    // first check cahce exists
    // if yes play from that
    // if no, start the default url
    // default url works within time > ok cool
    // url fails with timeout> switch to download


    // init > (if small wait 2 second else wait 10)
    // while waiting if (we get another init > switch to download)
    // while waiting if (start > before 10, okay)

    //need to check data consistency also, because of this
    //[1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] health window started
    //2026-02-10 03:39:42.505 25576-26799 UrlPlaybackObserver     com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] INIT #1
    //2026-02-10 03:39:46.993 25576-26799 UrlPlaybackObserver     com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] START received
    //2026-02-10 03:39:47.003 25576-26799 UrlPlaybackObserver     com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] DATA received (3 bytes)
    //2026-02-10 03:39:47.003 25576-26799 UrlPlaybackObserver     com.github.jaykkumar01.vaultspace    I  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] DECISION = HEALTHY (transfer, 4591 ms)
    // video does not start at all, waited too much




    // optimize the url, range request
    // build proxy
    // feed exoplayer the ranges
    // figure out how many bytes it needs for the first time
    // how we can store that while upload


    //just cache it as we wait for the drive sdk for the bad codecs, and also show the progress while doing the drive sdk

    // make it fast 2026-02-11 01:55:18.049 12399-14069 Video:DriveDS           com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] open → SDK @0
    //2026-02-11 01:55:23.539 12399-14069 Video:DriveDS           com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] close @36
    //2026-02-11 01:55:23.642 12399-14069 Video:DriveDS           com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] open → HTTP @40127682
    //2026-02-11 01:55:25.725 12399-14069 Video:DriveDS           com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] close @40140848
    //2026-02-11 01:55:25.726 12399-14069 Video:DriveDS           com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] open → HTTP @36
    //2026-02-11 01:55:40.291 12399-14069 Video:DriveDS           com.github.jaykkumar01.vaultspace    D  [1BuPZdULn_IyDTDVOnHhPR-0rKIbEWGJj] close @40122690

    // use the sdk for low ranges like 36 and all, or actually if it less then 1 mb or decide the threshold

    //while uploading observe this behaviour for each video and save the range to app properties
    // so that we can cache these ranges before actually calling the exoplayer

    //then find out what is there in that file for that range then using short stream reverse enginner it
    // so that wihtout exo plyayer we can save the data

    // first take the request range.. then save to cache then fail or do something so that exo player will try catch
    // or do this... first take request fill the cache
    // take the request, always fill 256 kb from the position then fail, send it back


    // decide which one is better sdk or the http
    // then do this... one continious steam that will get close only by controller
    // other streams check the position if availble take from the main stream, if main stream is not reached yet
    // get it from there
    // also handle in a way that you don't have to download same bytes again and again
    // fill the main bytes[] with continious 256 kb bytes.. keep checking what is not filled based on that request the
    // next range, lock it no other system

    // let it read some batch at one not one by one


    // once the main thread is done... close all temp


    // constantly fill the cache using the circular buffer until Player Ready state, after that we only do what exo player requests

    // so it requests 0, I gave it 8 mb, it read only first 36 bytes, then closes, it requests at 80%, connections open, previous gone (what happened to extra buffer)
    // so the optimization will be that I either keep track of buffer not discard it so on next request I can feed it
    // or I make the buffer less based on file size or something
    // what are the factors I need to correctly decide buffer size


    // maybe let exo read all remaining bytes before opening the next connection make it wait/ or send to cache somehow fast

    // to minimize the waste, we need to limit the reading speed and the current position of the player
    // there is one more catch, you are looking at the reader and producer but think aobut the player current posibtion
    // that is the only thing that is used from user pov( al though caching will fix the waste becasue all reading will be cached)


}
