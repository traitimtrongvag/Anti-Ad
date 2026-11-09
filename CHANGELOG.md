# Changelog

All notable changes to [Anti-Ad](https://github.com/traitimtrongvag/Anti-Ad) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).


## [Unreleased]

### Fixed

- #8: Fixed Spotify launch fallback when both service launch attempts fail.

### Added

 - #10: Reduced battery use by holding the partial wake lock only during ad processing.
 
 - #11: Added a troubleshooting info button beside the Force-Stop Delay setting.


## [0.1.0] - 2026-11-08

This release focuses on improving notification listener reliability, Spotify playback detection, Shizuku authorization, and automated testing.

### Fixed

 - #2: Fixed notification listener binding retries when Android does not reconnect the service.
 
 - #3: Fixed buffering being treated as confirmed Spotify playback.
 
 - #4: Fixed first-time Shizuku authorization by defaulting Anti-Ad to disabled and waiting for Shizuku's binder before requesting permission.
 
 - #6: Fixed stale Shizuku binder state visibility and prevented concurrent duplicate initialization.

### Added

 - #1: Added automated unit tests for notification parsing and log management.
