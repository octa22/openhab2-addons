# Hisense TV Binding

This binding controls Hisense televisions that expose the RemoteNOW MQTT service on the local network.
It uses the protocol implemented by [newAM/hisensetv](https://github.com/newAM/hisensetv).

## Supported Things

The `tv` Thing supports remote control keys, volume and input selection.
Add it manually and enter the TV's hostname or IP address.

## Pairing

The pairing page lists Hisense TV Things already configured in openHAB.
If the list is empty, first add a Thing under **Settings → Things** or define a `hisensetv:tv` Thing in a `.things` file (see the [full example](#full-example)).
Then reopen the pairing page.

If the television requires pairing:

1. Turn on the TV and open `/hisensetv/pairing` on your openHAB server.
1. Sign in as an openHAB administrator, select the Thing and click **Request PIN**.
1. Enter the four digits shown on the TV and click **Pair TV** within two minutes.

The TV stores the pairing against the Thing's configured remote ID.
The default remote ID also matches [newAM/hisensetv](https://github.com/newAM/hisensetv), so an existing pairing with that tool can be reused.
Use one client with that remote ID at a time.

The TV must have its MQTT service running to receive commands.
Sending `KEY_POWER` behaves like pressing the power button on the remote.
A TV that shuts down its network service in standby cannot be woken by that command.

## Thing Configuration

| Parameter                | Default               | Description |
|--------------------------|-----------------------|-------------|
| `host`                   |                       | Hostname or IP address of the TV. |
| `port`                   | `36669`               | MQTT port on the TV. |
| `tls`                    | `true`                | Use TLS. Set to `false` for TVs that require the Python tool's `--no-ssl` flag. |
| `certificateFingerprint` |                       | SHA-256 fingerprint of the TV's TLS certificate, with optional colon separators. |
| `username`               | TV's standard account | MQTT username. |
| `password`               | TV's standard account | MQTT password. |
| `remoteId`               | `XX:XX:XX:XX:XX:XY`    | Remote app identity. Use the default to reuse pairing from the Python tool. |
| `refreshInterval`        | `30`                  | Volume polling interval in seconds. |

TLS uses normal certificate and hostname verification when no fingerprint is configured.
For a self-signed certificate, obtain the fingerprint while connected to a trusted TV and network, then configure it for this Thing.
For example, with the TV on:

```shell
openssl s_client -connect 192.168.1.50:36669 </dev/null 2>/dev/null |
  openssl x509 -noout -fingerprint -sha256
```

Enter the value after `=` as `certificateFingerprint`.
The binding then verifies the exact certificate and uses it to identify the TV.

## Channels

| Channel   | Item Type | Description |
|-----------|-----------|-------------|
| `key`     | String    | Sends a remote key such as `KEY_HOME`, `KEY_OK`, `KEY_RETURNS`, `KEY_MUTE` or `KEY_POWER`. |
| `volume`  | Dimmer    | Reads or sets the volume from 0 to 100. Also accepts `INCREASE`, `DECREASE` and `REFRESH`. |
| `sources` | String    | Read-only JSON list of TV inputs, including each `sourceid` and `sourcename`. Accepts `REFRESH`. |
| `source`  | String    | Selects an input by its `sourceid` from the list. Accepts `REFRESH` to update that list. |

## Full Example

```java
Thing hisensetv:tv:livingroom "Living Room TV" [ host="192.168.1.50" ]
```

```java
String LivingRoomTVKey     { channel="hisensetv:tv:livingroom:key", autoupdate="false" }
Dimmer LivingRoomTVVolume  { channel="hisensetv:tv:livingroom:volume", autoupdate="false" }
String LivingRoomTVInputs  { channel="hisensetv:tv:livingroom:sources" }
String LivingRoomTVSource  { channel="hisensetv:tv:livingroom:source", autoupdate="false" }
```
