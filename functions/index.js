// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');
admin.initializeApp();

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });

/**
 * Triggers when a user gets a new follower and sends a notification.
 *
 * Followers add a flag to `/followers/{followedUid}/{followerUid}`.
 * Users save their device notification tokens to `/users/{followedUid}/notificationTokens/{notificationToken}`.
 */
exports.sendStorageUnitNotification = functions.database.ref('/notification/{notificationUid}')
    .onCreate((change, context) => {
        const notificationUid = context.params.notificationUid;
        console.log('We have a new log UID:', notificationUid);

        // Get the list of users.
        const getUsersPromise = admin.database().ref(`/users`).once('value');

        // Get the follower profile.
        const getNotificationPromise = admin.database().ref(`/notification/${notificationUid}`).once('value');

        // The snapshot to the users.
        let usersSnapshot;

        // The array containing all the user's tokens.
        let tokens
        // The Map containing all the tokens -> userId map.
        let tokensMap = new Map()

        return Promise.all([getUsersPromise, getNotificationPromise]).then(results => {
            usersSnapshot = results[0];
            unitNotification = results[1];

            // Check if there are any users.
            if (!usersSnapshot.hasChildren()) {
                return console.log('There are no users.');
            }
            console.log('There are', usersSnapshot.numChildren(), 'users.');
            console.log('Fetched unitNotification ', unitNotification.val());

            usersSnapshot.forEach((user) => {
                var userKey = user.key
                console.log('User userKey: ', userKey);
                console.log('User : ', user.val());
                //console.log('User user: ', usersSnapshot.child(userKey).val());
                // Listing all users as an array.
                //var userTokens = Object.keys(usersSnapshot.child(userKey).child("notificationTokens").val());
                var userTokens = Object.keys(user.child("notificationTokens").val());
                userTokens.forEach((token) => {
                    tokensMap.set(token, userKey)
                })
            });

            console.log('Fetched tokensMap count', tokensMap.size);
            // Check if there are any device tokens.
            if (tokensMap.size <= 0) {
                return console.log('There are no notification tokens to send to.');
            }

            // Notification details.
            const payload = {
                notification: {
                    title: 'New Event!',
                    body: `Event from ${unitNotification.child("name").val()}; Value: ${unitNotification.child("value").val()}.`
                }
            };
            tokens = [...tokensMap.keys()]
            // Send notifications to all tokens.
            return admin.messaging().sendToDevice(tokens, payload);
        }).then((response) => {
            // For each message check if there was an error.
            const tokensToRemove = [];
            response.results.forEach((result, index) => {
                const error = result.error;
                if (error) {
                    console.error('Failure sending notification to', tokens[index], error);
                    // Cleanup the tokens who are not registered anymore.
                    if (error.code === 'messaging/invalid-registration-token' ||
                        error.code === 'messaging/registration-token-not-registered') {
                        //tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove());
                        tokensToRemove.push(usersSnapshot.ref.child(tokensMap.get(tokens[index])).child("notificationTokens").child(tokens[index]).remove());
                    }
                }
            });
            return Promise.all([tokensToRemove, unitNotification.ref.remove()]);
        });
    });