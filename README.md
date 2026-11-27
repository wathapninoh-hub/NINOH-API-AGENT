# Ninoh API Agent

Android API chat app with model discovery and a selectable model.

## Features
- Enter an OpenAI-compatible API base URL and API key.
- Fetch models from the provider's `/models` endpoint.
- Select a model and chat through `/chat/completions`.
- API key and base URL are saved locally in app preferences; never commit your key to this repository.

## Compatible providers
Works with providers that expose OpenAI-compatible `GET /models` and `POST /chat/completions` endpoints. Example base URLs:
- OpenRouter: `https://openrouter.ai/api/v1`
- Groq: `https://api.groq.com/openai/v1`

Other providers may require a different API format or headers and may not work just by changing the URL. This first version does not directly support Gemini's native API format.

## Build
A GitHub Actions workflow builds a debug APK on pushes to `main` and on manual workflow dispatch. Open **Actions**, choose **Build Android APK**, then download the `Ninoh-API-Agent-debug` artifact from a successful run. The artifact is a debug APK, not a Play Store release.

Requires Android 6.0+ (API 23); designed to run on Android 11.
