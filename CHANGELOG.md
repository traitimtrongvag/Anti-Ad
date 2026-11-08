# Changelog

All notable changes to [Anti-Ad](https://github.com/traitimtrongvag/Anti-Ad) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).


## [Unreleased]

### Fixed

 - #2: Fixed notification listener binding retries when Android does not reconnect the service.
 - #3: Fixed buffering being treated as confirmed Spotify playback.
 - #4: Fixed first-time Shizuku authorization by defaulting Anti-Ad to disabled and waiting for Shizuku's binder before requesting permission.
 - #6: Fixed stale Shizuku binder state visibility and prevented concurrent duplicate initialization.

### Added

 - #1: Added automated unit tests for notification parsing and log management.
