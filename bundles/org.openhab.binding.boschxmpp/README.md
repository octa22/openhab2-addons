# Bosch XMPP Binding

This binding connects Bosch EasyControl controllers to openHAB through the Bosch XMPP cloud service.
It implements the unofficial protocol documented by the [bosch-xmpp project](https://github.com/robertklep/bosch-xmpp) and does not require its Node.js application or HTTP bridge.

The implementation has been tested with a Bosch EasyControl CT200 running firmware 05.04.00.
Nefit, IVT and KM200 devices are not supported.

## Supported Things

| Thing Type UID | Kind | Description |
|----------------|------|-------------|
| `boschxmpp:easycontrol` | Bridge | One physical EasyControl controller and its XMPP connection |
| `boschxmpp:zone` | Thing | One controller heating zone |
| `boschxmpp:device` | Thing | One paired thermostat or radiator valve |
| `boschxmpp:dhw` | Thing | One domestic hot-water circuit |

Each controller has its own serial number and credentials and therefore requires its own `easycontrol` Bridge.
All child Things share the single persistent XMPP connection and serialized request queue of their Bridge.

## Discovery

The controller itself cannot be discovered on the local network and must be created manually.
Once its credentials are valid and the Bridge is online, openHAB discovery finds its zones, paired devices and domestic hot-water circuits.
Discovery uses `/zones/list`, `/devices/list` and `/dhwCircuits`; the cloud API does not provide an account-level `/gateways` collection.

## Bridge Configuration

| Name | Type | Description | Default | Required | Advanced |
|------|------|-------------|---------|----------|----------|
| `serialNumber` | text | Serial number printed on the controller | N/A | yes | no |
| `accessKey` | text | Access key printed on the controller | N/A | yes | no |
| `devicePassword` | text | Device password configured in the EasyControl app | N/A | yes | no |
| `refreshInterval` | integer | Polling interval in seconds, minimum 10 seconds | `30` | no | yes |
| `requestTimeout` | integer | XMPP request timeout in seconds | `5` | no | yes |
| `hostname` | text | Bosch XMPP service hostname | `xmpp.rrcng.ticx.boschtt.net` | yes | yes |
| `port` | integer | Bosch XMPP service port | `5222` | yes | yes |

The device password is not the Bosch SingleKey ID account password.
Set or change it in the EasyControl app under **Settings → Personal → Change password**.
The access key may contain hyphens; the binding removes them as required by the service.

Child configuration (`zoneId`, `deviceId` or `circuitId`) is filled automatically by discovery.

## Channels

### EasyControl Bridge

| Channel | Item Type | Read/Write | Description |
|---------|-----------|------------|-------------|
| `firmware-version` | String | R | Controller firmware version |
| `outdoor-temperature` | Number:Temperature | R | Outdoor temperature |
| `system-pressure` | Number:Pressure | R | Heating system pressure |
| `supply-temperature` | Number:Temperature | R | Heat source supply temperature |
| `modulation` | Dimmer | R | Heat source modulation |
| `flame-indication` | String | R | Current flame indication |
| `heating-pump` | String | R | Heating-pump state reported by older boilers; numeric modulation from newer boilers is converted to `on` or `off` |
| `heating-pump-modulation` | Dimmer | R | Heating-pump modulation reported by newer boilers; `on`/`off` from older boilers is converted to 100/0 % |
| `room-influence` | Number | RW | Heating circuit room-influence level |
| `target-supply-temperature` | Number:Temperature | R | Heat source supply-temperature setpoint |
| `notification-light` | Switch | W | Activates the controller notification light |
| `away-mode` | Switch | RW | Controller-wide away mode |

The `/heatSources/CHpumpModulation` endpoint differs between boiler generations.
Older boilers expose a string value (`on` or `off`), while newer boilers expose numeric modulation from 0 to 100 %.
The binding detects the endpoint type and updates both heating-pump channels: numeric values are converted to an
`on`/`off` state, and string values are converted to 100/0 % modulation.

### Zone

| Channel | Item Type | Read/Write | Description |
|---------|-----------|------------|-------------|
| `room-temperature` | Number:Temperature | R | Current zone temperature |
| `target-temperature` | Number:Temperature | RW | Target temperature, 5–30 °C in 0.5 °C steps |
| `operation-mode` | String | RW | `clock` or `manual` |
| `heating-status` | String | R | Current heating state |
| `humidity` | Dimmer | R | Relative humidity |

In manual mode, a target-temperature command changes the manual setpoint.
In clock mode, it changes the current clock override and does not modify the weekly program.

### Device

| Channel | Item Type | Read/Write | Description |
|---------|-----------|------------|-------------|
| `battery` | String | R | Battery status reported by the device |
| `signal` | Dimmer | R | Radio signal strength |
| `device-type` | String | R | Device type, such as `thermostat_valve` |
| `firmware-version` | String | R | Device firmware version |
| `device-zone` | Number | R | Assigned zone number |
| `child-lock` | Switch | RW | Device child lock, when supported |
| `device-temperature` | Number:Temperature | R | Radiator valve measured temperature, when supported |
| `valve-position` | Dimmer | R | Radiator valve position, when supported |

### Domestic Hot Water

| Channel | Item Type | Read/Write | Description |
|---------|-----------|------------|-------------|
| `dhw-temperature` | Number:Temperature | R | Current water temperature |
| `dhw-target-temperature` | Number:Temperature | RW | Target water temperature, when the controller reports it as available |
| `dhw-mode` | String | RW | `Off`, `high`, `ownprogram` or `eco` |
| `dhw-status` | String | R | Current hot-water state |

Optional endpoints that are not exposed by a particular installation remain `UNDEF`.

The binding checks the controller metadata before writing the hot-water target temperature. If Bosch reports this
feature as unavailable, its reported value remains readable but commands are rejected locally without changing the
Thing status.

## Polling and Errors

Linked channels are refreshed using `refreshInterval`. Each child Thing also verifies its identity endpoint once
after initialization, and the Bridge reads its firmware once to populate the Thing property even when the matching
channel is not linked.
The connection sends an XMPP presence heartbeat every 30 seconds and answers server-side XMPP ping, software-version
and Bosch client-version requests, as required by the Bosch backend. Reconnection is owned by the binding and reuses
one XMPP resource so that a failed session is replaced instead of leaving multiple resources that could receive
responses for the same controller account.
The protocol sequence number is an unsigned byte and is therefore wrapped from 255 back to 0 during long-running
sessions.

A rejected API command (for example an HTTP 400 or 403 response) does not take a connected Bridge offline. HTTP 401
is treated as an authentication failure, while rate limits and server errors are treated as communication failures.
GET requests may be retried after reconnecting. PUT requests are not repeated after an ambiguous connection failure;
the binding instead reads the endpoint back to determine whether the requested value was applied.

Polling reads only channels that are linked to at least one Item, which avoids cloud requests for unused channels.

## Full Example

Normally only the Bridge is written manually and child Things are accepted from the Inbox:

```java
Bridge boschxmpp:easycontrol:home "Bosch EasyControl" [
    serialNumber="123456789",
    accessKey="abcd-efgh-ijkl-mnop",
    devicePassword="device-password"
] {
    Thing zone zone1 "Heating Zone 1" [ zoneId="zn1" ]
    Thing device thermostat "CT200 Thermostat" [ deviceId="device1" ]
    Thing dhw hotwater "Hot Water" [ circuitId="dhw1" ]
}
```

```java
Number:Temperature Zone1_RoomTemperature "Room Temperature [%.1f %unit%]" { channel="boschxmpp:zone:home:zone1:room-temperature" }
Number:Temperature Zone1_TargetTemperature "Target Temperature [%.1f %unit%]" { channel="boschxmpp:zone:home:zone1:target-temperature" }
String Zone1_OperationMode "Operation Mode [%s]" { channel="boschxmpp:zone:home:zone1:operation-mode" }
Switch Thermostat_ChildLock "Child Lock" { channel="boschxmpp:device:home:thermostat:child-lock" }
Number:Temperature HotWater_Temperature "Hot Water [%.1f %unit%]" { channel="boschxmpp:dhw:home:hotwater:dhw-temperature" }
```

## Cloud Dependency

This is an unofficial integration and depends on the Bosch cloud service and its private protocol.
Bosch can change either without notice.

The server uses a private Bosch certificate authority.
The binding trusts only the bundled Bosch EasyControl root CA and retains normal TLS hostname verification.
