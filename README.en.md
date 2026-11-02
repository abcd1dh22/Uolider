[বাংলা](README.md) · **English**

<div align="center">

# 📤 Git Folder Uploader

**Push a whole code folder from your phone straight to GitHub, folder structure intact.**
It works like `cd` + `git push` in Termux, but in an app and without typing commands.

`Kotlin` · `Jetpack Compose` · `GitHub API` · `Android 7.0+`

</div>

---

## 📑 Contents

1. [What the app does](#-what-the-app-does)
2. [Features](#-features)
3. [Installing on your phone (phone only)](#-installing-on-your-phone-phone-only)
4. [Creating an access token](#-creating-an-access-token)
5. [How to use it](#-how-to-use-it)
6. [Settings page](#-settings-page)
7. [Builds & APK tab](#-builds--apk-tab)
8. [Updating the app](#-updating-the-app)
9. [How it works inside](#-how-it-works-inside)
10. [Security & privacy](#-security--privacy)
11. [Limitations](#-limitations)
12. [Troubleshooting](#-troubleshooting)
13. [Project structure](#-project-structure)

---

## 🎯 What the app does

On the GitHub website you can only upload files one at a time from a phone; folders are not supported. With Termux you can push a whole folder using `git`, but you need to know the commands.

This app does that job for you:

1. You enter an **access token**, choose a **repo**, and pick a **folder**.
2. Tap **Push** and the whole folder, with every subfolder, goes to GitHub in **one commit**.
3. On GitHub it appears arranged exactly like the folder on your phone.

---

## ✨ Features

### 📁 Upload
| Feature | Details |
|---|---|
| Whole-folder upload | Subfolders included, structure preserved |
| One commit for all files | Even 100 files make a single commit, so history stays clean |
| No commit if nothing changed | If the result is identical to what's already there, no empty commit is created |
| Pick a repo from a list | Shows every repo the token can reach, with search |
| Create a new repo | Creates the repo if it doesn't exist (public or private) |
| Works on empty repos | Initializes the repo automatically on the first push |
| Scan & preview | Before pushing, shows file count, total size and what will go (like `ls` / `git status`) |
| Exclude rules | `.git`, `node_modules`, `build` and more are excluded by default. Add your own names or extensions like `*.apk` |
| Skip `.github/workflows` switch | If your token lacks the `workflow` permission, turn this on so the upload isn't blocked |
| Upload into a subfolder | Send files into a folder inside the repo instead of its root |

### 🔔 Background upload
- After you tap Push, the upload **keeps running even if you close the app or lock the phone**.
- While it runs, a notification shows a **progress bar** and a **Cancel** button.
- A result notification appears at the end, with a **View on GitHub** button on success.
- On Android 13+ the app asks for notification permission the first time. If you decline, the upload still works; you just won't see notifications.

### 👥 Multiple accounts
- Save several tokens and switch accounts by tapping the `@name` chip at the top.
- The `×` on a chip removes only that token from the phone.

### 🔨 Build tracker
- See GitHub Actions build status inside the app (queued / running / success / failed).
- Download and install the APK of a successful build directly.
- Delete old builds and APKs to free space.

### 🎨 Look & feel
- Clean UI with icons. Finished steps collapse by themselves, and a Push button stays pinned at the bottom.
- **Bengali / English**, **Light / Dark / System**.

### 🔒 Logout & data removal
- Remove the token from the phone with one tap.
- An "Auto logout after upload" switch.
- "Delete all saved data" clears the token, repo, folder permission and everything else.

---

## 📲 Installing on your phone (phone only)

No computer and no Android Studio needed. GitHub builds the app for you.

### Step 1: Get a repo
Create a repo on GitHub (private is fine).

### Step 2: Add the workflow file
1. In the repo tap **Add file → Create new file**.
2. Name the file: `.github/workflows/build.yml`
3. Copy the **entire contents** of `build-apk-all-in-one.yml` and paste it in.
4. Tap **Commit changes**.

> 💡 This single file contains the whole app source. On the first build it unpacks everything into the repo by itself.

> ⚠️ A ZIP uploaded to a repo does not unpack itself, so use the single-file method above instead of a ZIP.

### Step 3: Wait for the build
1. Open the repo's **Actions** tab. The build starts automatically.
2. A green ✅ means it's finished (usually 5-10 minutes).

### Step 4: Download the APK
1. Tap the finished build.
2. Under **Artifacts**, download `GitFolderUploader-debug-apk`.
3. Unzip it and install `app-debug.apk`.
4. If your phone asks, allow "install from unknown sources".

> If the first build fails, open the failed build in Actions and read the red ❌ step. It will tell you why.

---

## 📦 APK in Releases

After every successful build, the workflow automatically creates a new release in the repo's **Releases** section (tags `v1`, `v2`, `v3`…) and attaches the APK.

- Where to find it: **Releases** on the right side of the repo page (scroll down on a phone), or `repo-link/releases`.
- Download `GitFolderUploader-vN.apk` straight from there and install it. No unzipping needed.
- For a private repo only you (when logged in) can see and download it. To share it with others, the repo has to be public.
- Re-running a build replaces the APK on the same tag.
- Delete old releases from GitHub's Releases page when you don't need them.


## 🔑 Creating an access token

GitHub → **Settings → Developer settings → Personal access tokens**

| Token type | Permissions needed |
|---|---|
| **Classic** (simplest) | ✅ `repo`<br>➕ `workflow` if you want to upload `.github/workflows` files |
| **Fine-grained** | ✅ Repository access: selected repo(s) or all<br>✅ **Contents: Read and write**<br>➕ **Workflows: Read and write** to upload workflow files<br>➕ **Actions: Read and write** for the Builds tab |

To create new repos, Classic `repo` is enough. Fine-grained tokens also need the account-level permission to create repositories.

> 🔐 Treat the token like a password. Don't share it or post it anywhere.

---

## 🚀 How to use it

When you open the app you see a compact header (name, account status, ⚙️ settings), the tabs below it, and four steps.

### 1️⃣ Account
1. Paste your token into **Access Token**.
2. Tap **Verify**. On success it shows `@yourname connected` and the step collapses.

### 2️⃣ Repository
- Tap **Choose from my repos** to get a list. Search and tap one; the repo and its default branch fill in automatically.
- Or type `owner/repo` or a repo link by hand.
- Set the **branch** (for example `main`) and a **commit message**.

### 3️⃣ Folder
1. Tap **Choose folder** and point to your project folder.
2. The app scans it and shows the **file count**, **total size**, and **large files skipped**.
3. Tap **Show file list** to see exactly what will be pushed.

> ⚠️ You can't pick **Download** or the storage root (an Android rule). Pick the project folder inside it.

### 4️⃣ Push
- Tap **Push to GitHub** at the bottom of the screen. It's enabled once the token, repo and folder are set.
- A progress bar and a terminal-style log appear (`✓` success, `!` warning, `✗` failure).
- At the end you see **Done!**, and **View on GitHub** opens the commit.
- You can tap **Cancel** while it runs. Cancelling creates no commit and the repo stays as it was.

---

## ⚙️ Settings page

Tap ⚙️ at the right of the header. Go back with the arrow at top-left or the phone's Back button.

| Card | Contents |
|---|---|
| **Account & security** | Remember token (encrypted) · Auto logout after upload · Logout button |
| **Repo creation** | Create repo if missing · Private repo |
| **Folder & filters** | Subfolder inside the repo · Excluded names (comma separated) · Skip workflows folder · Rescan |
| **Language & theme** | Bengali / English · System / Light / Dark |
| **Delete all saved data** | Removes token, repo, branch and folder permission |

> In English mode the on-screen text changes. The terminal log and notifications still appear in Bengali.

---

## 🔨 Builds & APK tab

The second tab, next to Upload. It works once a repo and token are set (the token needs the **Actions** permission).

- **Build list:** the latest builds with their status and how long ago they ran. If a build is running, the list refreshes itself every 8 seconds.
- **Download & install APK:** downloads the APK of a successful build inside the app, then installs it with one tap. The first time, it may open settings so you can allow installs.
- **Run again:** restarts a failed build.
- **Delete / Clean old:** removes builds and their APKs from GitHub. "Clean old" keeps the newest 3 completed builds and deletes the rest.
- **Downloaded APKs:** APKs saved on the phone, listed separately so you can install or delete them.

---

## 🔄 Updating the app

1. In the repo open `.github/workflows/build.yml` and tap ✏️ (Edit).
2. **Delete everything** inside and paste the contents of the new `build-apk-all-in-one.yml`, then commit.
3. When the build finishes in Actions, download and install the new APK from the app's **Builds & APK** tab.

> **Stable signing key:** the first build creates `app/debug.keystore` and saves it in the repo. From then on every APK is signed with the same key, so a new APK installs as an update over the old one without uninstalling. (If an older version signed without this key is installed, uninstall it once, then install the new one.)

---

## 🧠 How it works inside

The app doesn't run `git`. It talks directly to the **GitHub Git Data API**:

```
 Folder on phone
      │  (Storage Access Framework)
      ▼
 Build file list ── apply exclude rules
      │
      ▼
 Each file ──▶ POST /git/blobs      (base64)
      │
      ▼
 Combine blobs ──▶ POST /git/trees  (on top of the existing tree)
      │
      ▼
 Tree unchanged? ──yes──▶ no commit, "nothing changed"
      │ no
      ▼
 POST /git/commits ──▶ PATCH /git/refs/heads/<branch>
```

- **A single commit:** all files are added to one tree, so history gets one entry.
- **Safe to stop:** if the upload is interrupted or cancelled, the repo is untouched, because the branch only moves in the last step.
- **Rate-limit handling:** a pause between requests (about 0.85 s), at most 3 small files in parallel, and files over 2 MB uploaded one at a time. If GitHub rate-limits, the app waits and retries.
- **Background work:** the upload runs in a foreground service (type `dataSync`).

---

## 🛡️ Security & privacy

- The **token** is stored with `EncryptedSharedPreferences` (AES-256 backed by the Android Keystore). It is never sent anywhere except GitHub.
- The app only talks to `api.github.com`. There is no analytics and no third-party server.
- `allowBackup=false`, so the token isn't carried over through device backups.
- Folder access is **read-only**. The app never changes or deletes your files.
- No setting on your GitHub account is changed. The app only commits to the chosen branch of the chosen repo.
- Logging out or choosing "Delete all saved data" removes the token and saved info from the phone. Nothing changes on GitHub.

> Uploading like this with your own token is ordinary API use and shouldn't cause trouble for your account. If you make an unusually large number of requests in a short time, GitHub may apply a temporary rate limit, and the app pauses and waits by itself.

---

## ⛔ Limitations

| Topic | Details |
|---|---|
| Per-file limit | Files over **25 MB** are skipped automatically (shown with `!` in the preview) |
| Binary files | Images, APKs and so on upload fine, but large ones take longer |
| Deleting files | Files already in the repo are not deleted if they're missing from the phone folder (the app only adds or updates) |
| Storage root folders | Android doesn't allow picking Download or the storage root directly |
| Git history | The app doesn't read a `.git` folder from the phone (it's excluded by default) |
| English mode | The log and notifications are still in Bengali |

---

## 🧰 Troubleshooting

| Symptom | Cause and fix |
|---|---|
| **401 / Bad credentials** | Token is wrong or expired. Create a new one |
| **403 / Resource not accessible** | The token lacks `repo` (Classic) or Contents: Read and write (Fine-grained) |
| **404 Not Found** | Wrong repo name, or the token can't see that private repo |
| Uploading `.github/workflows` fails | The token lacks the `workflow` permission. Grant it, or turn on "Skip workflows folder" in settings |
| Repo isn't created | The token has no permission to create repos |
| Rate-limit warning | Just wait; the app retries by itself |
| Branch not found | Check the branch name (`main` or `master`) |
| "Actions permission" error in the Builds tab | Give the token Actions: Read and write |
| APK won't install | In phone settings, allow this app to install unknown apps |
| New APK fails to install over the old one | The old APK was signed with a different key. Uninstall once, then install the new one |
| Build fails in Actions | Read the red text of the failed step, and send it if you need help |

---

## 🗂️ Project structure

```
GitFolderUploader/
├── .github/workflows/build-apk.yml     GitHub Actions build
├── app/
│   ├── build.gradle.kts                dependencies and signing
│   └── src/main/
│       ├── AndroidManifest.xml         permissions, service, FileProvider
│       ├── res/                        icons, themes, strings
│       └── java/com/example/gitfolderuploader/
│           ├── MainActivity.kt         the whole UI (Compose)
│           ├── MainViewModel.kt        app state and logic
│           ├── GitHubUploader.kt       Git Data API upload, scan, repo list
│           ├── UploadService.kt        background service and notifications
│           ├── ActionsApi.kt           build list, APK download, deletion
│           ├── Accounts.kt             storing multiple tokens
│           ├── Lang.kt                 language and theme mode
│           ├── Theme.kt                colors and shapes
│           └── Icons.kt                icons
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

**Stack:** Kotlin 1.9 · Jetpack Compose (Material 3) · OkHttp · Coroutines · AndroidX Security Crypto · DocumentFile
**Package name:** `com.example.gitfolderuploader`
**Minimum Android:** 7.0 (API 24)

---

<div align="center">

Tell me about any problem or a feature you'd like. 💙

</div>
