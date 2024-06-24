// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
import {initializeApp} from "firebase-admin/app";
//import {getAuth} from "firebase-admin/auth";
// The Firebase Admin SDK to access the Firebase Realtime Database.
import {getDatabase} from "firebase-admin/database";
import {getMessaging} from "firebase-admin/messaging";
import {log, warn} from "firebase-functions/logger";
import {onValueWritten} from "firebase-functions/v2/database";

initializeApp();
//const auth = getAuth();
const db = getDatabase();
const messaging = getMessaging();

/**
 * Triggers when a user gets a new follower and sends a notification.
 *
 * Followers add a flag to `/followers/{followedUid}/{followerUid}`.
 * Users save their device notification tokens to `/users/{followedUid}/notificationTokens/{notificationToken}`.
 */
export const sendStorageUnitNotification = onValueWritten(
    "/notification/{notificationUid}",
    async (event) => {
        // If un-follow we exit the function.
        if (!event.data.after.val()) {
            log(`Notification ${event.params.notificationUid} removed`);
            return;
        }

        const notificationUid = event.params.notificationUid;
        log("We have a new log UID:", notificationUid);

        // Get the list of users.
        const users = await db.ref("/users").get();

        if (!users.hasChildren()) {
            log("There are no users to send notifications to.");
            return;
        }

        // Get User Tokens

        // The Map containing all the tokens -> userId map.
        let tokensMap = new Map()

        users.forEach((user) => {
            var userKey = user.key
            log("User userKey: ", userKey);
            log("User : ", {user: user.val()});
            // Listing all users as an array.
            var userTokens = Object.keys(user.child("notificationTokens").val());
            userTokens.forEach((token) => { tokensMap.set(token, userKey) })
        });
        log("Fetched tokensMap count: ", tokensMap.size);

        // The array containing all the user`s tokens.
        let notificationTokens = tokensMap.keys()

        // Check if there are any device tokens.
        if (notificationTokens.size <= 0) {
            return log("There are no notification tokens to send to.");
        }

        // Get the follower profile.
        const unitNotification = await db.ref(`/notification/${notificationUid}`).get();
        log("The unitNotification that was received", {unitNotification: unitNotification.val()});

        var notificationDate = new Date(unitNotification.child("lastUpdateTime").val());

        // Notification details.
        const notification = {
            title: unitNotification.child("name").val(),
            body: "Value: " + unitNotification.child("value").val().toString() + "\n" + notificationDate.toUTCString(),
            priority: 'high',
            channelId: unitNotification.child("type").val(),
            eventTimestamp: notificationDate
        };
        const androidConfig = {
                    notification: notification,
                    priority: 'high',
                    collapseKey: unitNotification.child("type").val()
                };

        log("The notification to send:", {notification: notification});

        // Send notifications to all tokens.
        const messages = [];
        notificationTokens.forEach((token) => {
            messages.push({
                token: token,
                android: androidConfig,
            });
        });
        const batchResponse = await messaging.sendEach(messages);


        if (batchResponse.failureCount != notificationTokens.size) {
            log("Removing sent notification")
            unitNotification.ref.remove()
        }

        if (batchResponse.failureCount < 1) {
            // Messages sent sucessfully. We're done!
            log("Messages sent.");
            return;
        }

        warn(`${batchResponse.failureCount} messages weren't sent.`,
          batchResponse);

        // Clean up the tokens that are not registered any more.
        for (let i = 0; i < batchResponse.responses.length; i++) {
            const errorCode = batchResponse.responses[i].error?.code;
            const errorMessage = batchResponse.responses[i].error?.message;
            if ((errorCode === "messaging/invalid-registration-token") ||
                (errorCode === "messaging/registration-token-not-registered") ||
                (errorCode === "messaging/invalid-argument" &&
                  errorMessage ===
                  "The registration token is not a valid FCM registration token")) {
              log(`Removing invalid token: ${messages[i].token} from ${tokensMap.get(messages[i].token)}`);
              await db.ref(`/users/${tokensMap.get(messages[i].token)}/notificationTokens`).child(messages[i].token).remove()
            }
        }

    });