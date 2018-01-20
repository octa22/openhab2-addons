# Mi Internet Speaker Binding

This is the binding for the Xiaomi Mi Internet Speaker
https://xiaomi-mi.com/portable-speakers/xiaomi-mi-internet-speaker-white/

## Supported Things
Xiaomi Mi Internet Speaker

## Discovery

Once binding active, your Internet Speaker is auto discovered and appears in Inbox of PaperUI.

## Binding Configuration

No configuration needed.

## Thing Configuration

The Internet speaker needs a device URL string. Since it is not easy to find the device URL, it is better to have it
auto discovered.

## Channels

* control - control the speaker, e.g. start/pause/next/previous
* bluetooth - control bluetooth capability of the speaker device
* command - sending commands to the speaker device (play, pause, next, prev, off) 
* volume - control volume of the speaker device by dimmer/slider
* sound - control sound mode of the speaker device
* sleep - control sleep feature of the speaker device
* playmode - control play mode of the speaker device (repeat all, one, shuffle)
* artist - display current song's artist name of the speaker device
* title - display current song's title of the speaker device
* status - display status of the speaker device (playing/pause/...)

## Example
items file:
```
Player XiaomiSpeakerControl "Xiaomi speaker control" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:control" } 
String XiaomiSpeakerStatus "Xiaomi speaker status [%s]" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:status" }
Dimmer XiaomiSpeakerVolume "Xiaomi speaker volume" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:volume" }
String XiaomiSpeakerSound "Xiaomi speaker sound" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:sound" }
String XiaomiSpeakerSleep "Xiaomi speaker sleep" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:sleep" }
String XiaomiSpeakerCommand "Xiaomi speaker command" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:command", autoupdate="false" }
String XiaomiSpeakerMode "Xiaomi speaker play mode" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:playmode" }
String XiaomiSpeakerArtist "Xiaomi speaker artist [%s]" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:artist" }
String XiaomiSpeakerTitle "Xiaomi speaker title [%s]" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:title" }
Switch XiaomiSpeakerBT "Xiaomi speaker bluetooth" { channel="miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15:bluetooth" }
```

sitemap:
```
Group item=FF_MiSpeaker {
        Default item=XiaomiSpeakerControl label="Control" icon="speaker"
        //Switch item=XiaomiSpeakerCommand label="Control" icon="speaker" mappings=["PAUSE"="PAUSE","PLAY"="PLAY"]
        //Switch item=XiaomiSpeakerCommand label="Song" icon="arrows" mappings=["PREV"="PREV","NEXT"="NEXT"]
        Switch item=XiaomiSpeakerCommand label="Power" icon="shutdown" mappings=[OFF="OFF"]
        Slider item=XiaomiSpeakerVolume label="Volume" icon="music"
        Setpoint item=XiaomiSpeakerVolume label="Volume" icon="music" minValue=0 maxValue=100 step=1
        Selection label="Sound" item=XiaomiSpeakerSound icon="music" mappings=["NORMAL"="Normal", "VOICE"="Voice", "BASS"="Bass", "TREBLE"="Treble"]
        Selection label="Sleep" item=XiaomiSpeakerSleep icon="sleep" mappings=["0"="OFF", "300"="5 min", "600"="10 min", "1200"="20 min", "1800"="30 min", "2400"="40 min", "3000"="50 min", "3600"="60 min", "4200"="70 min","4800"="80 min","5400"="90 min","6000"="100 min", "6600"="110 min","7200"="120 min"]                
        Selection label="Mode" item=XiaomiSpeakerMode icon="music" mappings=["REPEAT_ALL"="Repeat all", "REPEAT_ONE"="Repeat one", "REPEAT_SHUFFLE"="Repeat shuffle"]
        Text item=XiaomiSpeakerStatus label="Status [%s]"
        Text item=XiaomiSpeakerArtist label="Artist [%s]"
        Text item=XiaomiSpeakerTitle label="Title [%s]"
        Switch item=XiaomiSpeakerBT icon="bluetooth"
    }
```
things:
```
Thing miinternetspeaker:speaker:0b28241c-de33-6013-6dab-f1aad3d87e15 [ deviceUrl="http://192.168.2.216:9999/0b28241c-de33-6013-6dab-f1aad3d87e15-MR/" ]
```

## Note

