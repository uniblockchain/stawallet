# Stawallet

Stawallet is a different type of wallet. Something between hot and cold...

## Warning!
* Using Stawallet in production might results to loose all of your money. Please use it **only** if you have deep knowledge of cryptocurrencies and related stack.
* Stawallet is still in alpha and has not been used in production.
* Always have a cold backup from all your wallets.

## Specification
* Stawallet (is going to) supports any kind of crypto assets (based on SLIP-0044).
* Hot wallet: Automatically accept deposit or process withdraw requests.
* Cold wallet: It's your primary wallet. Stawallet might only send transaction to it.
* BIP-0044 address derivation methodology.
* Cold wallet could be managed by any BIP-0044-compatible wallets (both software or hardware wallet).
* Prevent hot wallet to keep more than UCL.
* Notify you whenever your hot wallet's balance get lower than LCL.

## DSL
* Cold wallet: cryptocurrency wallet which has no connection to the internet.
* Hot wallet: cryptocurrency wallet which has ability to sign and push transactions by itself.
* UCL (Upper Control Limit): Maximum value of the hot wallet.
* LCL (Lower Control Limit): Minimum value of the hot wallet.

## Philosophy:
* Encapsulate the cryptocurrency side of the wallet as much as possible.
* Support all cryptocurrencies
* Only use official blockchain node daemons
* Do NOT let the blockchain daemon keeps any private key
* Limited send, Unlimited receive
* Has no idea about your cold wallet private keys

## Supported cryptocurrencies:
* Bitcoin (BTC)
* Litecoin (LTC)
* Ethereum (ETH)
* Ripple (XRP)

## Introduction
Stawallet is a service to simplify the deposit and withdraw of cryptocurrency assets...

## Getting started
1. Make 2 wallet seed and backup from BIP-0039 mnemonic words (Don't skip the backup section, or you will loose anything).
2. Start blockchain rpc-enabled daemon of all supported cryptocurrencies (docker is great for it).
3. For each cryptocurrency, choose an address as the base address in your cold wallet.
4. For each cryptocurrency, make `XPRV` derivation using `m/44'/coin_type'/account_index'/` as path (`account_index` could be anything, choose `0` for beginning)
5. Configure the `application.conf` by obtained addresses and keys.
6. Configure the `application.conf` by the rpc server IPs and Ports.
7. Start Stawallet and enjoy it. Take care!

## UseCases
* Exchange internal wallet
* Custodian wallet providers
* Payment processing of any **dynamic** and *enterprise** merchant app

