# Stawallet
Stawallet is a different type of coins. Something between hot and cold...

## Warning!
* Using Stawallet in production might results to loose all of your money. Please use it **only** if you have deep knowledge of cryptocurrencies and related stack.
* Stawallet is still in alpha and has not been used in production.
* Always have a cold backup from all your wallets.

## Specification
* Stawallet (is going to) supports any kind of crypto assets (based on SLIP-0044).
* Hot coins: Automatically accept deposit or process withdraw requests.
* Cold coins: It's your primary coins. Stawallet might only send transaction to it.
* `BIP-0044` address derivation methodology.
* Cold coins could be managed by any `BIP-0044`-compatible wallets (both software or hardware coins).
* Prevent hot coins to keep more than UCL.
* Notify you whenever your hot coins's balance get lower than LCL.

## DSL
* Cold coins: cryptocurrency coins which has no connection to the internet.
* Hot coins: cryptocurrency coins which has ability to sign and push transactions by itself.
* UCL (Upper Control Limit): Maximum value of the hot coins.
* LCL (Lower Control Limit): Minimum value of the hot coins.

## Philosophy:
* Encapsulate the cryptocurrency side of the coins as much as possible.
* Support all cryptocurrencies
* Only use official blockchain node daemons
* Do NOT let the blockchain daemon keeps any private key
* Limited send, Unlimited receive
* Has no idea about your cold coins private keys

## Supported cryptocurrencies:
* Bitcoin (BTC)
* Litecoin (LTC)
* Ethereum (ETH)
* Ripple (XRP)

## Introduction
Stawallet is a service to simplify the deposit and withdraw of cryptocurrency assets...

## Getting started
1. Make 2 coins seed and backup from `BIP-0039` mnemonic words (Don't skip the backup section, or you will loose anything).
2. Start blockchain rpc-enabled daemon of all supported cryptocurrencies (docker is great for it).
3. For each cryptocurrency, choose an address as the base address in your cold coins.
4. For each cryptocurrency, make `XPRV` derivation using `m/44'/coin_type'/account_index'/` as path (`account_index` could be anything, choose `0` for beginning)
5. Configure the `application.conf` by obtained addresses and keys.
6. Configure the `application.conf` by the rpc server IPs and Ports.
7. Start Stawallet and enjoy it. Take care!

## Cli
Stawallet provides a simple and useful "Command Line Interface".

#### Install CLI
```bash
./gradlew instalDist
alias stawallet=./build/install/stawallet/bin/stawallet
```

Make sure everything are ok using the following command:
```bash
stawallet -h
```

#### Create Database
```bash
sudo -u postgres psql -c "$(stawallet database create 2> /dev/null)"
```

use `-f` flag to drop the existing database (if exists)

#### Create Database Schema
```bash
stawallet database init
```

#### Populate Database (using your configuration file)
```bash
stawallet database populate
```

#### Run blockchain watcher for each wallet
```bash
stawallet watch my-lovely-btc-wallet
```

#### Start REST Api server
```bash
stawallet serve
```

## Data Types
* We store ALL amount related values (including fee values) as non-floating-point `DECIMAL` data type. It is equal to `BigInteger` in Kotlin and Java languages.
* We dedicate `30` prec for all amount values in database. It means that the equivalent sql data type is `DECIMAL(30,0)`. 
* All amount will be rendered as `string` in `json` outputs. (Because some third-party json libraries does not support long numbers)
* All values will be in the lowest possible unit in each cryptocurrency or token (for example `sat` instead of `btc`, `wei` instead of `eth`...)
* There is NO floating-point for amounts, at all.
* DateTimes are stored in database as sql `DateTime` type.
* DateTimes will be rendered in `UTC` human-readable format in json outputs.
* `block height`s (or `block number`s) are always stores and used as long.
* `confirmation` numbers are always in `int` type.

## UseCases
* Exchange internal coins
* Custodian coins providers
* Payment processing of any **dynamic** and **enterprise** merchant app

