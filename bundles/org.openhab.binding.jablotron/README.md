# Jablotron Alarm Binding

This is the OH2.x binding for Jablotron alarms.
https://www.jablotron.com/en/jablotron-products/alarms/

## Supported Things

* bridge (the bridge to your jablonet cloud account)
* JA-80 OASIS alarm
* JA-100 alarm (partial support, still under development)
 
Please contact me if you want to add other alarms (e.g. JA-100 etc)

## Discovery

This binding supports auto discovery. Just manually add bridge thing and supply login & password to your Jablonet account.

## Binding Configuration

Binding itself doesn't require specific configuration.

## Thing Configuration

The bridge thing requires this configuration:

* login (login to your jablonet account)
* password (password to your jablonet account)

optionally you can set

* lang (language of the alarm texts)

The both alarm things have this configuration:

* refresh (thing status refresh period in seconds, default is 180s)

The Ja100 alarm thing has one extra parameter

 * code (alarm master code, used for controlling the sections & PGMs)

## Channels

The bridge thing does not have any channels.
The oasis thing exposes these channels:

* statusA (the status of A section)
* statusB (the status of AB/B section)
* statusABC (the status of ABC section)
* statusPGX (the status of PGX)
* statusPGY (the status of PGY)
* command (the channel for sending codes to alarm)
* lastEvent (the text description of the last event)
* lastEventCode (the code of the last event)
* lastEventClass (the class of the last event - arm, disarm, service)
* lastEventTime (the time of the last event)
* lastCheckTime (the time of the last checking)
* lastTrouble (the last problem reported by alarm)
* lastTroubleDetail (the detail info about the last problem)
* alarm (the alarm status OFF/ON)

The JA100 thing has these channels:

* lastEvent (the text description of the last event)
* lastEventSection (the section of the last event)
* lastEventClass (the class of the last event - arm, disarm, service)
* lastEventTime (the time of the last event)
* lastCheckTime (the time of the last checking)

all other channels (sections, PGMs, temperature sensors) are dynamicaly created according to your configuration 

## Full Example

#items file for JA80

```
String  HouseAlarm "Alarm [%s]" <alarm>
String JablotronCode { channel="jablotron:oasis:8c93a5ed:50139:command", autoupdate="false" }
Switch	ArmSectionA	"Garage arming"	<jablotron>	(Alarm)	{ channel="jablotron:oasis:8c93a5ed:50139:statusA" }
Switch	ArmSectionAB	"1st floor arming"	<jablotron>	(Alarm)	{ channel="jablotron:oasis:8c93a5ed:50139:statusB" }
Switch	ArmSectionABC	"2nd floor arming"	<jablotron>	(Alarm)	{ channel="jablotron:oasis:8c93a5ed:50139:statusABC" }
String LastEvent "Last event code [%s]" <jablotron> { channel="jablotron:oasis:8c93a5ed:50139:lastEvent" }
DateTime LastEventTime "Last event [%1$td.%1$tm.%1$tY %1$tR]" <clock> { channel="jablotron:oasis:8c93a5ed:50139:lastEventTime" }
DateTime LastCheckTime "Last check [%1$td.%1$tm.%1$tY %1$tR]" <clock> { channel="jablotron:oasis:8c93a5ed:50139:lastCheckTime" }
Switch	ArmControlPGX	"PGX"	<jablotron>	(Alarm)	{ channel="jablotron:oasis:8c93a5ed:50139:statusPGX" }
Switch	ArmControlPGY	"PGY"	<jablotron>	(Alarm)	{ channel="jablotron:oasis:8c93a5ed:50139:statusPGY" }
```

#sitemap example for JA80

```
Text item=HouseAlarm icon="alarm" {
            Switch item=ArmSectionA
            Switch item=ArmSectionAB
            Switch item=ArmSectionABC
            Text item=LastEvent
            Text item=LastEventCode
            Text item=LastEventClass
            Text item=LastEventTime
            Text item=LastCheckTime
            Switch item=ArmControlPGX
            Switch item=ArmControlPGY
            Switch item=JablotronCode label="Arm" mappings=[1234=" A ",2345=" B ",3456="ABC"]
            Switch item=JablotronCode label="Disarm" mappings=[9876="Disarm"]
      }
```

#rule example for JA80

```
rule "Alarm"
when 
  Item ArmSectionA changed or Item ArmSectionAB changed or Item ArmSectionABC changed or 
  System started
then
   if( ArmSectionA.state == ON || ArmSectionAB.state == ON || ArmSectionABC.state == ON)
   {   postUpdate(HouseAlarm, "partial")  }
   if( ArmSectionA.state == OFF && ArmSectionAB.state == OFF && ArmSectionABC.state == OFF)
   {   postUpdate(HouseAlarm, "disarmed") }
   if( ArmSectionA.state == ON && ArmSectionAB.state == ON && ArmSectionABC.state == ON)
   {   postUpdate(HouseAlarm, "armed")    }
end
```