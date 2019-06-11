# Sitzreservation QR

This app works together with my Python-based webtool for selling seats at a local event. The website produces a ticket with QR code and RSA signature and this app can be used to check in those tickets.

The seat order is hard-coded, at the moment there is no intent to change that.

## Usage
The created QR codes contain text with info about the year, bill number, seats, and number of ordered menus:

```
TVM 2019
Nr. Sa13Nov-MUE-043
Seat: D13, D14, D15, D16
Menu: 4
Checksum: 1F33A3942
```

Scanning a valid QR code will check-in the seats and mark them read on the Room tab. You can also check in seats by hand there. In the preferences, you need to set the year and event key (above `Sa13Nov`) and then you can download the data directly from my website. This is also hard-coded at the moment.


## Technical details

The checksum is a base64-encoded RSA certificate using the SHA-256 hash of the first four lines as message and the public key `n=1276109729173033093` and `d=197116842892907279`.

## Credits
Thanks to Mauricio, from whom the QR code reader app was originally forked. It is so good to see people writing perfectly functioning, ad-free, open source apps.
