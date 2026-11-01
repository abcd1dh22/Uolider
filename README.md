# Git Folder Uploader

Android অ্যাপ: ফোন থেকে পুরো প্রজেক্ট ফোল্ডার, ফোল্ডার-কাঠামো ঠিক রেখে, এক কমিটে GitHub-এ আপলোড করে।

## APK বানানোর দুটি উপায়

**১. Android Studio:** ফোল্ডারটি Open করুন → Build → Build APK(s)।

**২. GitHub Actions (পিসি ছাড়া):** এই প্রজেক্ট নিজের GitHub রিপোতে পুশ করুন (Termux-এ `git` দিয়ে)।
Actions ট্যাবে "Build APK" চলবে, শেষে **Artifacts** থেকে `app-debug.apk` ডাউনলোড করুন।

## টোকেন

GitHub → Settings → Developer settings → Personal access tokens
- Fine-grained: ওই রিপোতে **Contents: Read and write**
- Classic: **repo** স্কোপ

## কীভাবে কাজ করে

Git Data API: `blobs` → `trees` → `commits` → `refs`।
সব ফাইল একটাই কমিটে যায়, তাই ফোল্ডার-স্ট্রাকচার হুবহু থাকে।

## সীমাবদ্ধতা

- প্রতি ফাইল সর্বোচ্চ ২৫MB (এর বেশি হলে বাদ দিয়ে লগে দেখায়)।
- GitHub-এর rate limit (ঘণ্টায় ~৫০০ কন্টেন্ট রিকোয়েস্ট) আছে; বেশি ফাইল হলে অ্যাপ নিজে অপেক্ষা করে আবার চেষ্টা করে।
- Android-এ Download বা স্টোরেজের রুট ফোল্ডার বাছাই করা যায় না; ভেতরের ফোল্ডার বাছাই করুন।
