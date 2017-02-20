# cbord-api

Provides access to the CBORD Get dining services web application through JSON for easier development of scripts and apps. Basically, this simulates a browser's HTTP requests since there is no actual API available and scrapes the response.

This app uses a hard-coded value, "grinnell", to set the part of the URL that presumably refers to the institution. That is because I can't actually test any other possible institutions since I don't have normal access. If you wish to use this outside of Grinnell, feel free to change the `institution` value in `cbord-api.api`, just make sure to test it first.

## Endpoints

On success, the status code will be 200 and the JSON's `status` value will be `ok`. Otherwise, the status code will be in the 400s and the `status` value will give a brief description of the problem.

If a user's login has timed out and the request you made does not specify a username and a password the request will return an error with a `login timed out` status.

Parameters for POST requests should be in the normal/`x-www-form-urlencoded` format. Parameters for GET requests should be as query strings.

**Please note:** currently, every request to an endpoint causes the server to do some form of web scraping against the CBORD Get website. This is because every POST request potentially changes something (even in the case of login, the session information is refreshed) and every GET request is non-deterministic since something could've changed between API calls. As such, *do not expect any request to run quickly*. You should always assume that for web services, but that is especially true in basically every case for this API.

### POST /login [username password]

Tries to login a user given the username and password. If successful, the session data is stored until it needs to be updated (ie. when a timeout occurs).

Note: in the future, the cache will invalidate and remove old items to prevent eventual memory errors.

### GET /balances [username]

Checks the user's balances, returning an object with `meals`, `campus`, `dining`, and `guest` values corresponding to those balances.

### POST /all [username] [password]

Performs, in order, the `login` and `balances` requests and returns the combined result.

Note: in the future, this will chain with other calls that use the GET method, such as recent transactions.

## Features

* login
* current balance

## To be implemented

* adjust usage of check-timed-out to be inline
* tests

* better implementation of login session cache
* recent transactions
* full transaction history
* HTTPS
* adding funds
* adding credit card

## Installation/Running

The easiest way to run this is to install [Leiningen](https://leiningen.org/) and execute `lein run`. You can then make it more distributable by executing `lein uberjar` and then running the result as you would any other jar file.

## Updates/PRs

Any additions you can make are greatly appreciated! Feel free to fork this, play around with it, and/or critique my abuse of Clojure. Just keep in mind the...

## License

This project is MIT licensed. Please don't use it for evil.
