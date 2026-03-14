import {onCall, HttpsError} from "firebase-functions/v2/https";
import {defineSecret} from "firebase-functions/params";
import {logger} from "firebase-functions";

const spotifyClientId = defineSecret("SPOTIFY_CLIENT_ID");
const spotifyClientSecret = defineSecret("SPOTIFY_CLIENT_SECRET");

const SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";

export const getSpotifyAccessToken = onCall(
    {secrets: [spotifyClientId, spotifyClientSecret]},
    async (request) => {
    // 1. Authenticate the user
      if (!request.auth) {
        throw new HttpsError("unauthenticated", "User must be authenticated.");
      }

      // 2. Retrieve secrets using defineSecret()
      const clientId = spotifyClientId.value();
      const clientSecret = spotifyClientSecret.value();

      if (!clientId || !clientSecret) {
        logger.error("Missing Spotify credentials in environment.");
        throw new HttpsError("internal", "Service configuration error.");
      }

      try {
      // 3. Request token from Spotify
        const authHeader = "Basic " +
        Buffer.from(`${clientId}:${clientSecret}`).toString("base64");

        const response = await fetch(SPOTIFY_TOKEN_URL, {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": authHeader,
          },
          body: "grant_type=client_credentials",
        });

        if (!response.ok) {
          const errorText = await response.text();
          logger.error("Spotify API error",
              {status: response.status, body: errorText});
          throw new HttpsError("unavailable",
              "Failed to communicate with Spotify.");
        }

        const data = await response.json();

        // 4. Return the token
        // We only return the access_token and expires_in,
        // not the scope or type if not needed.
        return {
          access_token: data.access_token,
          expires_in: data.expires_in,
        };
      } catch (error) {
        logger.error("Spotify token fetch failed", error);
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", "Failed to retrieve access token.");
      }
    },
);
