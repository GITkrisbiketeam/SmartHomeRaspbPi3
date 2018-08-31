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
exports.sendLogNotification = functions.database.ref('/log/{logUid}')
    .onWrite((change, context) => {
        const logUid = context.params.logUid;
        console.log('We have a new log UID:', logUid);

        // Get the list of users.
        const getUsersPromise = admin.database().ref(`/users`).once('value');

        // Get the follower profile.
        const getLogPromise = admin.database().ref(`/log/${logUid}`).once('value');

        // The snapshot to the users.
        let usersSnapshot;

        // The array containing all the user's tokens.
        let tokens
        // The Map containing all the tokens -> userId map.
        let tokensMap = new Map()

        return Promise.all([getUsersPromise, getLogPromise]).then(results => {
            usersSnapshot = results[0];
            const unitLog = results[1];

            // Check if there are any users.
            if (!usersSnapshot.hasChildren()) {
                return console.log('There are no users.');
            }
            console.log('There are', usersSnapshot.numChildren(), 'users.');
            console.log('Fetched unitLog ', unitLog.val());

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
                    body: `Event from ${unitLog.child("name").val()}; Value: ${unitLog.child("value").val()}; on ${unitLog.child("localtime").val()}.`
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
            return Promise.all(tokensToRemove);
        });
    });

/**
 * Triggers when a user gets a new follower and sends a notification.
 *
 * Followers add a flag to `/followers/{followedUid}/{followerUid}`.
 * Users save their device notification tokens to `/users/{followedUid}/notificationTokens/{notificationToken}`.
 */
exports.sendFollowerNotification = functions.database.ref('/followers/{followedUid}/{followerUid}')
    .onWrite((change, context) => {
      const followerUid = context.params.followerUid;
      const followedUid = context.params.followedUid;
      // If un-follow we exit the function.
      if (!change.after.val()) {
        return console.log('User ', followerUid, 'un-followed user', followedUid);
      }
      console.log('We have a new follower UID:', followerUid, 'for user:', followedUid);

      // Get the list of device notification tokens.
      const getDeviceTokensPromise = admin.database()
          .ref(`/users/${followedUid}/notificationTokens`).once('value');

      // Get the follower profile.
      const getFollowerProfilePromise = admin.auth().getUser(followerUid);

      // The snapshot to the user's tokens.
      let tokensSnapshot;

      // The array containing all the user's tokens.
      let tokens;

      return Promise.all([getDeviceTokensPromise, getFollowerProfilePromise]).then(results => {
        tokensSnapshot = results[0];
        const follower = results[1];

        // Check if there are any device tokens.
        if (!tokensSnapshot.hasChildren()) {
          return console.log('There are no notification tokens to send to.');
        }
        console.log('There are', tokensSnapshot.numChildren(), 'tokens to send notifications to.');
        console.log('Fetched follower profile', follower);

        // Notification details.
        const payload = {
          notification: {
            title: 'You have a new follower!',
            body: `${follower.displayName} is now following you.`,
            icon: follower.photoURL
          }
        };

        // Listing all tokens as an array.
        tokens = Object.keys(tokensSnapshot.val());
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
              tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove());
            }
          }
        });
        return Promise.all(tokensToRemove);
      });
    });
